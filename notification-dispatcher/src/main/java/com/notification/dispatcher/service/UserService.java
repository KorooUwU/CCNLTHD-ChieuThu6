package com.notification.dispatcher.service;

import com.notification.dispatcher.DTO.*;
import com.notification.dispatcher.repository.TaiKhoanRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {
    private final TaiKhoanRepository repository;

    public UserService(TaiKhoanRepository repository) { this.repository = repository; }

    @Transactional
    public UserResponseDTO updateProfile(UUID currentUserId, UpdateProfileRequestDTO request) {
        String name = request.tenTaiKhoan();
        if (name == null || name.isBlank() || name.length() > 100) {
            throw new IllegalArgumentException("Invalid display name");
        }
        var user = repository.findById(currentUserId)
                .orElseThrow(() -> new BadCredentialsException("Account no longer exists"));
        user.setTenTaiKhoan(name.strip());
        // The managed entity is updated by JPA dirty checking on transaction commit.
        return new UserResponseDTO(user.getId().toString(), user.getTenTaiKhoan(), user.getEmail(),
                user.getUserName(), user.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public UserSearchResponseDTO search(String keyword, int page, int size) {
        if (keyword == null || keyword.isBlank() || keyword.length() > 100
                || page < 0 || page > 10000 || size < 1 || size > 50) {
            throw new IllegalArgumentException("Invalid search parameters");
        }
        var result = repository.findByUserNameContainingIgnoreCaseOrTenTaiKhoanContainingIgnoreCase(
                keyword.strip(), keyword.strip(), PageRequest.of(page, size,
                        Sort.by("userName").ascending().and(Sort.by("id").ascending())));
        var content = result.getContent().stream().map(user -> new UserSummaryDTO(
                user.getId().toString(), user.getTenTaiKhoan(), user.getUserName())).toList();
        return new UserSearchResponseDTO(content, page, size, result.getTotalElements(), result.getTotalPages());
    }
}
