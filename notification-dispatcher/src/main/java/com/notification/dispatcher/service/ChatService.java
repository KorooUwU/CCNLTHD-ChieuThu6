package com.notification.dispatcher.service;

import com.notification.dispatcher.DTO.MessageResponseDTO;
import com.notification.dispatcher.DTO.SendMessageRequestDTO;
import com.notification.dispatcher.entity.Conversation;
import com.notification.dispatcher.entity.Message;
import com.notification.dispatcher.entity.TaiKhoan;
import com.notification.dispatcher.repository.ConversationRepository;
import com.notification.dispatcher.repository.MessageRepository;
import com.notification.dispatcher.repository.TaiKhoanRepository;
import com.notification.dispatcher.websocket.UserPresenceService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Xử lý nghiệp vụ chat: lưu tin nhắn vào DB và dispatch realtime qua WebSocket.
 *
 * <p>Sau khi lưu tin nhắn:
 * <ul>
 *   <li>Cập nhật {@code lastMessageAt} của Conversation để sort inbox đúng thứ tự.</li>
 *   <li>Dispatch tin nhắn đến người nhận qua {@code /user/{recipientId}/queue/notifications}
 *       (dù họ có online hay không — nếu offline thì frame sẽ bị drop, cần polling
 *       REST API để lấy lịch sử khi vào lại app).</li>
 * </ul>
 */
@Service
public class ChatService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserPresenceService presenceService;

    public ChatService(MessageRepository messageRepository,
                       ConversationRepository conversationRepository,
                       TaiKhoanRepository taiKhoanRepository,
                       SimpMessagingTemplate messagingTemplate,
                       UserPresenceService presenceService) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.taiKhoanRepository = taiKhoanRepository;
        this.messagingTemplate = messagingTemplate;
        this.presenceService = presenceService;
    }

    /**
     * Lưu tin nhắn vào DB và trả về DTO để dispatch qua STOMP.
     *
     * @param request  nội dung tin nhắn từ client
     * @param senderId UUID của người gửi (lấy từ JWT principal)
     * @return DTO chứa đầy đủ thông tin để broadcast
     * @throws IllegalArgumentException nếu conversationId hoặc senderId không tồn tại
     */
    @Transactional
    public MessageResponseDTO persistMessage(SendMessageRequestDTO request, UUID senderId) {
        UUID conversationId = UUID.fromString(request.getConversationId());

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Conversation không tồn tại: " + conversationId));

        TaiKhoan sender = taiKhoanRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Người dùng không tồn tại: " + senderId));

        // Kiểm tra sender có thuộc conversation này không
        boolean isMember = sender.getId().equals(conversation.getUser1().getId())
                || sender.getId().equals(conversation.getUser2().getId());
        if (!isMember) {
            throw new IllegalArgumentException("Bạn không thuộc cuộc trò chuyện này");
        }

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.getMessageContent())
                .read(false)
                .build();
        message = messageRepository.save(message);

        // Cập nhật timestamp để sort inbox theo tin nhắn mới nhất
        conversation.setLastMessageAt(Instant.now());
        conversationRepository.save(conversation);

        return toDTO(message);
    }

    /**
     * Gửi tin nhắn đến người nhận qua personal WebSocket queue.
     *
     * <p>Destination: {@code /user/{recipientId}/queue/notifications}
     * Client subscribe tại: {@code /user/queue/notifications}
     * (Spring tự động map theo user principal).
     *
     * @param message     DTO của tin nhắn đã lưu
     * @param senderId    UUID của người gửi để xác định người nhận
     * @param conversation cuộc trò chuyện liên quan
     */
    public void pushToRecipient(MessageResponseDTO message, UUID senderId, Conversation conversation) {
        TaiKhoan recipient = conversation.getUser1().getId().equals(senderId)
                ? conversation.getUser2()
                : conversation.getUser1();

        // convertAndSendToUser sử dụng principal name (UUID string) để route đúng session
        messagingTemplate.convertAndSendToUser(
                recipient.getId().toString(),
                "/queue/notifications",
                message);
    }

    /**
     * Lấy Conversation theo ID — dùng bởi ChatController sau khi persistMessage.
     */
    @Transactional(readOnly = true)
    public Conversation getConversation(UUID conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Conversation không tồn tại: " + conversationId));
    }

    // ── Mapping helper ──────────────────────────────────────────────────────

    private MessageResponseDTO toDTO(Message message) {
        LocalDateTime sentAt = LocalDateTime.ofInstant(
                message.getSentAt(), ZoneOffset.UTC);
        return MessageResponseDTO.builder()
                .messageId(message.getId().toString())
                .conversationId(message.getConversation().getId().toString())
                .senderId(message.getSender().getId().toString())
                .senderName(message.getSender().getTenTaiKhoan())
                .messageContent(message.getContent())
                .type("TEXT")
                .sendAt(sentAt)
                .isRead(message.isRead())
                .build();
    }
}
