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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final TaiKhoanRepository taiKhoanRepository;

    public MessageService(
            MessageRepository messageRepository,
            ConversationRepository conversationRepository,
            TaiKhoanRepository taiKhoanRepository) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.taiKhoanRepository = taiKhoanRepository;
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

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.getMessageContent())
                .type(type)
                .imageUrl(request.getImageUrl())
                .attachmentPublicId(request.getAttachmentPublicId())
                .read(false)
                .build();

        Message saved = messageRepository.save(message);

        conversation.setLastMessageAt(Instant.now());
        conversationRepository.save(conversation);

        return toDTO(saved);
    }

    @Transactional
    public List<MessageResponseDTO> getMessages(UUID currentUserId, UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Cuộc hội thoại không tồn tại"));

        validateParticipant(conversation, currentUserId);

        messageRepository.markAllMessagesAsRead(conversationId, currentUserId);

        return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private void validateParticipant(Conversation conversation, UUID userId) {
        if (conversation.getType() == ConversationType.GROUP) {
            boolean isMember = conversationRepository.isMember(conversation.getId(), userId);
            if (!isMember) {
                throw new IllegalArgumentException("Bạn không phải thành viên của nhóm trò chuyện này");
            }
        } else {
            boolean isParticipant = (conversation.getUser1() != null && conversation.getUser1().getId().equals(userId))
                    || (conversation.getUser2() != null && conversation.getUser2().getId().equals(userId));
            if (!isParticipant) {
                throw new IllegalArgumentException("Bạn không tham gia cuộc hội thoại này");
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
}
