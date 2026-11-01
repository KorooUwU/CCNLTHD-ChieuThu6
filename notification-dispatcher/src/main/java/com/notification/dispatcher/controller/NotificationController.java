package com.notification.dispatcher.controller;

import com.notification.dispatcher.DTO.NotificationResponseDTO;
import com.notification.dispatcher.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API quản lý thông báo của người dùng.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Lấy danh sách thông báo của người dùng hiện tại.
     */
    @GetMapping
    public List<NotificationResponseDTO> getNotifications(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return notificationService.getNotifications(userId);
    }

    /**
     * Lấy số lượng thông báo chưa đọc.
     */
    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Map.of("unreadCount", notificationService.getUnreadCount(userId));
    }

    /**
     * Đánh dấu toàn bộ thông báo là đã đọc.
     */
    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        notificationService.markAllAsRead(userId);
    }

    /**
     * Đánh dấu một thông báo cụ thể là đã đọc.
     */
    @PostMapping("/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(
            @jakarta.validation.Valid @RequestBody com.notification.dispatcher.DTO.MarkReadRequestDTO request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        notificationService.markAsRead(UUID.fromString(request.getNotificationId()), userId);
    }
}
