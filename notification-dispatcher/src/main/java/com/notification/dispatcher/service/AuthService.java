package com.notification.dispatcher.service;

import com.notification.dispatcher.DTO.CreateUserRequestDTO;
import com.notification.dispatcher.DTO.UpdateUserRequestDTO;
import com.notification.dispatcher.DTO.UserResponseDTO;
import com.notification.dispatcher.DTO.UserSummaryDTO;
import com.notification.dispatcher.entity.TaiKhoan;
import com.notification.dispatcher.exception.DuplicateAccountException;
import com.notification.dispatcher.repository.TaiKhoanRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {
    private final com.notification.dispatcher.security.JwtService jwtService;
    private final TaiKhoanRepository taiKhoanRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(TaiKhoanRepository taiKhoanRepository, PasswordEncoder passwordEncoder,
            com.notification.dispatcher.security.JwtService jwtService) {
        this.taiKhoanRepository = taiKhoanRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public com.notification.dispatcher.DTO.TokenResponseDTO login(com.notification.dispatcher.DTO.LoginRequestDTO request) {
        TaiKhoan user = taiKhoanRepository.findByUserName(request.userName().trim()).orElse(null);
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72 || user == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new org.springframework.security.authentication.BadCredentialsException("Thông tin đăng nhập không đúng");
        }
        return jwtService.issue(user.getId());
    }

    @Transactional(readOnly = true)
    public UserResponseDTO me(java.util.UUID id) {
        TaiKhoan user = taiKhoanRepository.findById(id).orElseThrow(() ->
                new org.springframework.security.authentication.BadCredentialsException("Tài khoản không còn tồn tại"));
        return new UserResponseDTO(user.getId().toString(), user.getTenTaiKhoan(), user.getEmail(),
                user.getUserName(), user.getCreatedAt());
    }

    // The caller must validate CreateUserRequestDTO before invoking registration.
    @Transactional
    public UserResponseDTO register(CreateUserRequestDTO request) {
        String userName = request.getUserName().trim();
        String email = request.getEmail().trim();

        // BCrypt limits passwords by UTF-8 bytes, not Java character count.
        // Preserve the password exactly as entered; never trim or truncate it.
        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Mật khẩu không được vượt quá 72 byte UTF-8");
        }
        if (taiKhoanRepository.existsByUserName(userName)) {
            throw new DuplicateAccountException("Username đã được sử dụng");
        }
        if (taiKhoanRepository.existsByEmail(email)) {
            throw new DuplicateAccountException("Email đã được sử dụng");
        }

        TaiKhoan user = new TaiKhoan(
                request.getTenTaiKhoan().trim(), email, userName,
                passwordEncoder.encode(request.getPassword()));

        try {
            // Flush inside the transaction so concurrent unique conflicts surface here.
            user = taiKhoanRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            if (exception.getMostSpecificCause() instanceof SQLException sqlException
                    && "23505".equals(sqlException.getSQLState())) {
                throw new DuplicateAccountException("Username hoặc email đã được sử dụng", exception);
            }
            throw exception;
        }

        return UserResponseDTO.builder()
                .id(user.getId().toString())
                .tenTaiKhoan(user.getTenTaiKhoan())
                .email(user.getEmail())
                .userName(user.getUserName())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Cập nhật tên hiển thị của người dùng (PATCH /api/users/me).
     * Chỉ cập nhật các trường được cung cấp (không null, không blank).
     *
     * @param userId  UUID của người dùng đang đăng nhập
     * @param request DTO chứa các trường muốn cập nhật
     * @return UserResponseDTO sau khi cập nhật
     */
    @Transactional
    public UserResponseDTO updateMe(UUID userId, UpdateUserRequestDTO request) {
        TaiKhoan user = taiKhoanRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại"));

        if (request.getTenTaiKhoan() != null && !request.getTenTaiKhoan().isBlank()) {
            user.setTenTaiKhoan(request.getTenTaiKhoan().trim());
        }

        user = taiKhoanRepository.save(user);

        return UserResponseDTO.builder()
                .id(user.getId().toString())
                .tenTaiKhoan(user.getTenTaiKhoan())
                .email(user.getEmail())
                .userName(user.getUserName())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Tìm kiếm người dùng theo keyword trong userName (GET /api/users/search).
     * Giới hạn 20 kết quả, chỉ trả về thông tin công khai.
     *
     * @param keyword từ khóa tìm kiếm (bắt buộc, không được để trống)
     * @return danh sách UserSummaryDTO khớp với keyword
     */
    @Transactional(readOnly = true)
    public List<UserSummaryDTO> searchUsers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("Keyword tìm kiếm không được để trống");
        }
        String safeKeyword = keyword.trim();
        if (safeKeyword.length() < 2) {
            throw new IllegalArgumentException("Keyword phải có ít nhất 2 ký tự");
        }
        return taiKhoanRepository
                .searchByUserNameContaining(safeKeyword, PageRequest.of(0, 20))
                .stream()
                .map(u -> UserSummaryDTO.builder()
                        .id(u.getId().toString())
                        .tenTaiKhoan(u.getTenTaiKhoan())
                        .userName(u.getUserName())
                        .build())
                .toList();
    }
}
