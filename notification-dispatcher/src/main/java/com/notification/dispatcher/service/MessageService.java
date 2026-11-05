package com.notification.dispatcher.service;

import com.notification.dispatcher.DTO.MessageResponseDTO;
import com.notification.dispatcher.DTO.SendMessageRequestDTO;
import com.notification.dispatcher.entity.Conversation;
import com.notification.dispatcher.entity.ConversationType;
import com.notification.dispatcher.entity.Message;
import com.notification.dispatcher.entity.TaiKhoan;
import com.notification.dispatcher.exception.UserNotFoundException;
import com.notification.dispatcher.repository.ConversationRepository;
import com.notification.dispatcher.repository.MessageRepository;
import com.notification.dispatcher.repository.TaiKhoanRepository;
import com.notification.dispatcher.DTO.ConversationUpdatedEventDTO;
import com.notification.dispatcher.DTO.NotificationResponseDTO;
import com.notification.dispatcher.entity.Notification;
import com.notification.dispatcher.rabbitmq.RabbitMQProducer;
import com.notification.dispatcher.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final NotificationRepository notificationRepository;
    private final RabbitMQProducer rabbitMQProducer;

    public MessageService(
            MessageRepository messageRepository,
            ConversationRepository conversationRepository,
            TaiKhoanRepository taiKhoanRepository,
            NotificationRepository notificationRepository,
            RabbitMQProducer rabbitMQProducer) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.taiKhoanRepository = taiKhoanRepository;
        this.notificationRepository = notificationRepository;
        this.rabbitMQProducer = rabbitMQProducer;
    }

    @Transactional
    public MessageResponseDTO sendMessage(UUID senderId, SendMessageRequestDTO request) {
        boolean hasText = request.getMessageContent() != null && !request.getMessageContent().isBlank();
        boolean hasImage = request.getImageUrl() != null && !request.getImageUrl().isBlank();

        if (!hasText && !hasImage) {
            throw new IllegalArgumentException("Tin nhắn phải có nội dung văn bản hoặc hình ảnh");
        }

        UUID convId = UUID.fromString(request.getConversationId());
        Conversation conversation = conversationRepository.findById(convId)
                .orElseThrow(() -> new IllegalArgumentException("Cuộc hội thoại không tồn tại"));

        validateParticipant(conversation, senderId);

        TaiKhoan sender = taiKhoanRepository.findById(senderId)
                .orElseThrow(() -> new UserNotFoundException("Người gửi không tồn tại"));

        String type = request.getType();
        if (type == null || type.isBlank()) {
            type = hasImage ? "IMAGE" : "TEXT";
        }

        Instant now = Instant.now();
        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.getMessageContent())
                .type(type)
                .imageUrl(request.getImageUrl())
                .attachmentPublicId(request.getAttachmentPublicId())
                .read(false)
                .sentAt(now)
                .build();

        Message saved = messageRepository.save(message);

        conversation.setLastMessageAt(now);
        conversationRepository.save(conversation);

        MessageResponseDTO response = toDTO(saved);

        // 1. Xác định danh sách người nhận (1-1 hoặc toàn bộ thành viên nhóm khác sender)
        List<TaiKhoan> recipients = new ArrayList<>();
        if (conversation.getType() == ConversationType.GROUP) {
            if (conversation.getMembers() != null) {
                for (TaiKhoan member : conversation.getMembers()) {
                    if (!member.getId().equals(senderId)) {
                        recipients.add(member);
                    }
                }
            }
        } else {
            TaiKhoan partner = (conversation.getUser1() != null && conversation.getUser1().getId().equals(senderId))
                    ? conversation.getUser2()
                    : conversation.getUser1();
            if (partner != null) {
                recipients.add(partner);
            }
        }

        // 2. Tạo Notification và phát tán sự kiện realtime qua RabbitMQ / SSE
        String notifType = conversation.getType() == ConversationType.GROUP ? "GROUP_MESSAGE" : "CHAT_MESSAGE";
        for (TaiKhoan recipient : recipients) {
            Notification notification = Notification.builder()
                    .recipient(recipient)
                    .message(saved)
                    .triggeredBy(sender)
                    .type(notifType)
                    .read(false)
                    .createdAt(now)
                    .build();
            notification = notificationRepository.save(notification);

            NotificationResponseDTO notifDto = NotificationResponseDTO.builder()
                    .notificationId(notification.getId().toString())
                    .type(notifType)
                    .isRead(false)
                    .senderId(sender.getId().toString())
                    .senderName(sender.getTenTaiKhoan())
                    .messageContent(saved.getContent() != null ? saved.getContent() : "[Hình ảnh]")
                    .conversationId(conversation.getId().toString())
                    .sendAt(LocalDateTime.ofInstant(now, ZoneOffset.UTC))
                    .build();

            // Gửi tin nhắn mới & thông báo cho người nhận
            rabbitMQProducer.sendChatMessage(recipient.getId(), response, senderId, conversation.getId());
            rabbitMQProducer.sendNotification(recipient.getId(), notifDto, senderId);
            rabbitMQProducer.sendConversationUpdated(recipient.getId(),
                    buildConversationUpdated(conversation, recipient.getId(), "NEW_MESSAGE"));
        }

        // 3. Đồng bộ inbox cho chính người gửi
        rabbitMQProducer.sendConversationUpdated(senderId,
                buildConversationUpdated(conversation, senderId, "NEW_MESSAGE"));

        return response;
    }

    @Transactional
    public List<MessageResponseDTO> getMessages(UUID currentUserId, UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Cuộc hội thoại không tồn tại"));

        validateParticipant(conversation, currentUserId);

        messageRepository.markAllMessagesAsRead(conversationId, currentUserId);

        // Đánh dấu đã đọc các thông báo từ đối phương trong cuộc trò chuyện này
        if (conversation.getType() == ConversationType.DIRECT) {
            TaiKhoan partner = (conversation.getUser1() != null && conversation.getUser1().getId().equals(currentUserId))
                    ? conversation.getUser2()
                    : conversation.getUser1();
            if (partner != null) {
                notificationRepository.markReadBySender(currentUserId, partner.getId());
            }
        } else if (conversation.getMembers() != null) {
            for (TaiKhoan m : conversation.getMembers()) {
                if (!m.getId().equals(currentUserId)) {
                    notificationRepository.markReadBySender(currentUserId, m.getId());
                }
            }
        }

        return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private void validateParticipant(Conversation conversation, UUID userId) {
        if (conversation.getType() == ConversationType.GROUP) {
            boolean isMember = conversationRepository.isMember(conversation.getId(), userId);
            if (!isMember) {
                throw new com.notification.dispatcher.exception.ConversationAccessDeniedException();
            }
        } else {
            boolean isParticipant = (conversation.getUser1() != null && conversation.getUser1().getId().equals(userId))
                    || (conversation.getUser2() != null && conversation.getUser2().getId().equals(userId));
            if (!isParticipant) {
                throw new com.notification.dispatcher.exception.ConversationAccessDeniedException();
            }
        }
    }

    public MessageResponseDTO toDTO(Message message) {
        return MessageResponseDTO.builder()
                .messageId(message.getId().toString())
                .conversationId(message.getConversation().getId().toString())
                .senderId(message.getSender().getId().toString())
                .senderName(message.getSender().getTenTaiKhoan())
                .messageContent(message.getContent())
                .type(message.getType())
                .imageUrl(message.getImageUrl())
                .attachmentPublicId(message.getAttachmentPublicId())
                .sendAt(message.getSentAt() == null
                        ? LocalDateTime.now(ZoneOffset.UTC)
                        : message.getSentAt().atZone(ZoneOffset.UTC).toLocalDateTime())
                .isRead(message.isRead())
                .build();
    }

    private ConversationUpdatedEventDTO buildConversationUpdated(
            Conversation conversation, UUID userId, String reason) {
        boolean isGroup = conversation.getType() == ConversationType.GROUP;
        TaiKhoan other = null;
        if (!isGroup && conversation.getUser1() != null) {
            other = conversation.getUser1().getId().equals(userId)
                    ? conversation.getUser2()
                    : conversation.getUser1();
        }
        long unread = messageRepository.countUnreadMessages(conversation.getId(), userId);
        var lastMsg = messageRepository.findFirstByConversationIdOrderBySentAtDesc(conversation.getId());

        String name = isGroup ? conversation.getGroupName() : (other != null ? other.getTenTaiKhoan() : "Người dùng");
        String username = isGroup ? conversation.getGroupName() : (other != null ? other.getUserName() : "user");

        return ConversationUpdatedEventDTO.builder()
                .conversationId(conversation.getId().toString())
                .otherUserId(other != null ? other.getId().toString() : null)
                .otherUserName(username)
                .otherTenTaiKhoan(name)
                .lastMessageContent(lastMsg.map(Message::getContent).orElse(null))
                .lastMessageAt(conversation.getLastMessageAt())
                .unreadCount(unread)
                .reason(reason)
                .build();
    }
}
