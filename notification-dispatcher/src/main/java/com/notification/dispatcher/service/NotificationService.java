package com.notification.dispatcher.service;

import com.notification.dispatcher.DTO.NotificationResponseDTO;
import com.notification.dispatcher.entity.Conversation;
import com.notification.dispatcher.entity.ConversationType;
import com.notification.dispatcher.entity.Notification;
import com.notification.dispatcher.repository.ConversationRepository;
import com.notification.dispatcher.repository.MessageRepository;
import com.notification.dispatcher.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service quản lý danh sách và trạng thái thông báo của người dùng.
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository) {
        this.notificationRepository = notificationRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
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

    /**
     * Đánh dấu tất cả thông báo từ một người gửi cụ thể là đã đọc.
     */
    @Transactional
    public void markReadBySender(UUID recipientId, UUID senderId) {
        notificationRepository.markReadBySender(recipientId, senderId);
    }

    private NotificationResponseDTO toDTO(Notification notification) {
        LocalDateTime sendAt = LocalDateTime.ofInstant(notification.getCreatedAt(), ZoneOffset.UTC);
        String convId = null;
        String content = null;

        // A legacy notification has no reliable message reference; do not guess
        // its content from a newer message or another group with the same users.
        if (notification.getMessage() != null) {
            var message = notification.getMessage();
            convId = message.getConversation().getId().toString();
            content = message.getContent() != null ? message.getContent()
                    : (message.getImageUrl() != null ? "[Hình ảnh]" : null);
        }

        return NotificationResponseDTO.builder()
                .notificationId(notification.getId().toString())
                .type(notification.getType())
                .isRead(notification.isRead())
                .senderId(notification.getTriggeredBy() != null ? notification.getTriggeredBy().getId().toString() : null)
                .senderName(notification.getTriggeredBy() != null ? notification.getTriggeredBy().getTenTaiKhoan() : null)
                .messageContent(content)
                .conversationId(convId)
                .sendAt(sendAt)
                .build();
    }
}
