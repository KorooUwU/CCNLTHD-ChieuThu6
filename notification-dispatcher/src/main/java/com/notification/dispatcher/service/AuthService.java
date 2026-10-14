package com.notification.dispatcher.service;

import com.notification.dispatcher.DTO.CreateUserRequestDTO;
import com.notification.dispatcher.DTO.UserResponseDTO;
import com.notification.dispatcher.entity.TaiKhoan;
import com.notification.dispatcher.exception.DuplicateAccountException;
import com.notification.dispatcher.repository.TaiKhoanRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

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
}
