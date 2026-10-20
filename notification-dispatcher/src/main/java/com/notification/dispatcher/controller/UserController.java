package com.notification.dispatcher.controller;

import com.notification.dispatcher.DTO.UserResponseDTO;
import com.notification.dispatcher.DTO.UpdateProfileRequestDTO;
import com.notification.dispatcher.DTO.UserSearchResponseDTO;
import com.notification.dispatcher.service.AuthService;
import com.notification.dispatcher.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final AuthService authService;
    private final UserService userService;
    public UserController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PatchMapping("/me")
    public UserResponseDTO updateProfile(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequestDTO request) {
        return userService.updateProfile(UUID.fromString(jwt.getSubject()), request);
    }

    @GetMapping("/search")
    public UserSearchResponseDTO search(
            @RequestParam String keyword, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return userService.search(keyword, page, size);
    }

    @GetMapping("/me")
    public UserResponseDTO me(@AuthenticationPrincipal Jwt jwt) {
        return authService.me(UUID.fromString(jwt.getSubject()));
    }
}
