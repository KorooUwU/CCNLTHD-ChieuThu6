package com.notification.dispatcher.service;

import com.notification.dispatcher.DTO.NotificationResponseDTO;
import com.notification.dispatcher.entity.Notification;
import com.notification.dispatcher.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Service quản lý danh sách và trạng thái thông báo của người dùng.
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Lấy danh sách thông báo của người dùng theo thứ tự mới nhất.
     */
    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getNotifications(UUID recipientId) {
        List<Notification> list = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
        return list.stream().map(this::toDTO).toList();
    }

    /**
     * Lấy số lượng thông báo chưa đọc.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID recipientId) {
        return notificationRepository.countByRecipientIdAndReadFalse(recipientId);
    }

    /**
     * Đánh dấu toàn bộ thông báo của người dùng là đã đọc.
     */
    @Transactional
    public void markAllAsRead(UUID recipientId) {
        notificationRepository.markAllAsRead(recipientId);
    }

    /**
     * Đánh dấu một thông báo cụ thể là đã đọc.
     */
    @Transactional
    public void markAsRead(UUID notificationId, UUID recipientId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getRecipient().getId().equals(recipientId)) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    private NotificationResponseDTO toDTO(Notification notification) {
        LocalDateTime sendAt = LocalDateTime.ofInstant(notification.getCreatedAt(), ZoneOffset.UTC);
        return NotificationResponseDTO.builder()
                .notificationId(notification.getId().toString())
                .type(notification.getType())
                .isRead(notification.isRead())
                .senderId(notification.getTriggeredBy() != null ? notification.getTriggeredBy().getId().toString() : null)
                .senderName(notification.getTriggeredBy() != null ? notification.getTriggeredBy().getTenTaiKhoan() : null)
                .messageContent(null)
                .conversationId(null)
                .sendAt(sendAt)
                .build();
    }
}
