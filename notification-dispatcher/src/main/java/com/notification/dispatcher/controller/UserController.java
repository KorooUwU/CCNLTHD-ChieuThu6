package com.notification.dispatcher.controller;

import com.notification.dispatcher.DTO.UpdateUserRequestDTO;
import com.notification.dispatcher.DTO.UserResponseDTO;
import com.notification.dispatcher.DTO.UserSummaryDTO;
import com.notification.dispatcher.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final AuthService authService;
    public UserController(AuthService authService) { this.authService = authService; }

    @GetMapping("/me")
    public UserResponseDTO me(@AuthenticationPrincipal Jwt jwt) {
        return authService.me(UUID.fromString(jwt.getSubject()));
    }

    /**
     * Cập nhật thông tin cá nhân của người dùng đang đăng nhập.
     * PATCH /api/users/me — chỉ người dùng chủ sở hữu mới được cập nhật.
     */
    @PatchMapping("/me")
    public UserResponseDTO updateMe(
            @Valid @RequestBody UpdateUserRequestDTO request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return authService.updateMe(userId, request);
    }

    /**
     * Tìm kiếm người dùng theo keyword trong userName.
     * GET /api/users/search?keyword=abc — trả về tối đa 20 kết quả.
     * Chỉ trả về thông tin công khai (không có email).
     */
    @GetMapping("/search")
    public List<UserSummaryDTO> searchUsers(
            @RequestParam String keyword,
            @AuthenticationPrincipal Jwt jwt) {
        // jwt chỉ để xác thực đã đăng nhập, bất kỳ user nào cũng search được
        return authService.searchUsers(keyword);
    }
}
