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

        if (notification.getTriggeredBy() != null && notification.getRecipient() != null) {
            if ("GROUP_MESSAGE".equalsIgnoreCase(notification.getType())) {
                List<Conversation> convs = conversationRepository.findAllByUserId(notification.getRecipient().getId());
                for (Conversation c : convs) {
                    if (c.getType() == ConversationType.GROUP &&
                            conversationRepository.isMember(c.getId(), notification.getTriggeredBy().getId())) {
                        convId = c.getId().toString();
                        var lastMsg = messageRepository.findFirstByConversationIdOrderBySentAtDesc(c.getId());
                        if (lastMsg.isPresent() && lastMsg.get().getContent() != null) {
                            content = lastMsg.get().getContent();
                        }
                        break;
                    }
                }
            } else {
                Optional<Conversation> convOpt = conversationRepository.findBetweenUsers(
                        notification.getRecipient().getId(),
                        notification.getTriggeredBy().getId()
                );
                if (convOpt.isPresent()) {
                    convId = convOpt.get().getId().toString();
                    var lastMsg = messageRepository.findFirstByConversationIdOrderBySentAtDesc(convOpt.get().getId());
                    if (lastMsg.isPresent() && lastMsg.get().getContent() != null) {
                        content = lastMsg.get().getContent();
                    }
                }
            }
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
