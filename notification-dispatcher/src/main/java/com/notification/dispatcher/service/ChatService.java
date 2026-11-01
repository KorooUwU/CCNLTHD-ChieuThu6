package com.notification.dispatcher.service;

import com.notification.dispatcher.DTO.*;
import com.notification.dispatcher.entity.Conversation;
import com.notification.dispatcher.entity.Message;
import com.notification.dispatcher.entity.Notification;
import com.notification.dispatcher.entity.TaiKhoan;
import com.notification.dispatcher.rabbitmq.RabbitMQProducer;
import com.notification.dispatcher.repository.ConversationRepository;
import com.notification.dispatcher.repository.MessageRepository;
import com.notification.dispatcher.repository.NotificationRepository;
import com.notification.dispatcher.repository.TaiKhoanRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Xử lý nghiệp vụ tin nhắn và cuộc trò chuyện:
 * <ul>
 *   <li>Lưu tin nhắn vào PostgreSQL</li>
 *   <li>Cập nhật timestamp cuộc trò chuyện</li>
 *   <li>Tạo notification cho người nhận</li>
 *   <li>Publish sự kiện vào RabbitMQ để consumer phân phối tới SSE</li>
 * </ul>
 */
@Service
public class ChatService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final NotificationRepository notificationRepository;
    private final RabbitMQProducer rabbitMQProducer;

    public ChatService(MessageRepository messageRepository,
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

    /**
     * Gửi tin nhắn mới: lưu DB, tạo notification và publish vào RabbitMQ.
     */
    @Transactional
    public MessageResponseDTO sendMessage(SendMessageRequestDTO request, UUID senderId) {
        UUID conversationId = UUID.fromString(request.getConversationId());

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Cuộc trò chuyện không tồn tại: " + conversationId));

        TaiKhoan sender = taiKhoanRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại: " + senderId));

        // Kiểm tra sender có thuộc conversation không
        boolean isUser1 = conversation.getUser1().getId().equals(senderId);
        boolean isUser2 = conversation.getUser2().getId().equals(senderId);
        if (!isUser1 && !isUser2) {
            throw new IllegalArgumentException("Bạn không phải thành viên của cuộc trò chuyện này");
        }

        TaiKhoan recipient = isUser1 ? conversation.getUser2() : conversation.getUser1();
        Instant now = Instant.now();

        // 1. Lưu Message vào database
        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.getMessageContent())
                .read(false)
                .sentAt(now)
                .build();
        message = messageRepository.save(message);

        // 2. Cập nhật thời điểm tin nhắn cuối của conversation
        conversation.setLastMessageAt(now);
        conversationRepository.save(conversation);

        // 3. Tạo bản ghi Notification cho người nhận trong DB
        Notification notification = Notification.builder()
                .recipient(recipient)
                .triggeredBy(sender)
                .type("CHAT_MESSAGE")
                .read(false)
                .createdAt(now)
                .build();
        notification = notificationRepository.save(notification);

        // 4. Chuyển đổi DTO
        MessageResponseDTO messageResponse = toMessageDTO(message);
        NotificationResponseDTO notificationResponse = NotificationResponseDTO.builder()
                .notificationId(notification.getId().toString())
                .type("CHAT_MESSAGE")
                .isRead(false)
                .senderId(sender.getId().toString())
                .senderName(sender.getTenTaiKhoan())
                .messageContent(message.getContent())
                .conversationId(conversation.getId().toString())
                .sendAt(LocalDateTime.ofInstant(now, ZoneOffset.UTC))
                .build();

        // 5. Publish vào RabbitMQ để chuyển tiếp tới SSE
        rabbitMQProducer.sendChatMessage(recipient.getId(), messageResponse, senderId, conversation.getId());
        rabbitMQProducer.sendNotification(recipient.getId(), notificationResponse, senderId);

        // 6. Push CONVERSATION_UPDATED cho người nhận (đang ở màn hình ngoài / inbox)
        //    → cập nhật preview tin nhắn cuối + tăng unread badge
        ConversationUpdatedEventDTO updatedForRecipient = buildConversationUpdated(
                conversation, recipient.getId(), "NEW_MESSAGE");
        rabbitMQProducer.sendConversationUpdated(recipient.getId(), updatedForRecipient);

        // 7. Push CONVERSATION_UPDATED cho người gửi (đồng bộ đa tab / đa thiết bị)
        //    → cập nhật preview trong inbox của chính họ (unread = 0 vì họ là sender)
        ConversationUpdatedEventDTO updatedForSender = buildConversationUpdated(
                conversation, senderId, "NEW_MESSAGE");
        rabbitMQProducer.sendConversationUpdated(senderId, updatedForSender);

        return messageResponse;
    }

    /**
     * Phát sự kiện đang soạn tin (typing) qua RabbitMQ.
     */
    @Transactional(readOnly = true)
    public void sendTyping(TypingEventDTO event, UUID senderId) {
        UUID conversationId = UUID.fromString(event.getConversationId());
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Cuộc trò chuyện không tồn tại"));

        TaiKhoan recipient = conversation.getUser1().getId().equals(senderId)
                ? conversation.getUser2()
                : conversation.getUser1();

        TypingEventDTO verified = TypingEventDTO.builder()
                .conversationId(event.getConversationId())
                .senderId(senderId.toString())
                .isTyping(event.isTyping())
                .build();

        rabbitMQProducer.sendTyping(verified, recipient.getId());
    }

    /**
     * Đánh dấu tất cả tin nhắn trong conversation là đã đọc.
     */
    @Transactional
    public void markAsRead(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Cuộc trò chuyện không tồn tại"));

        messageRepository.markAllMessagesAsRead(conversationId, userId);

        TaiKhoan otherUser = conversation.getUser1().getId().equals(userId)
                ? conversation.getUser2()
                : conversation.getUser1();

        MessageReadEventDTO readEvent = MessageReadEventDTO.builder()
                .conversationId(conversationId.toString())
                .messageId("")
                .readerId(userId.toString())
                .readAt(LocalDateTime.now())
                .build();

        rabbitMQProducer.sendMessageRead(readEvent, otherUser.getId());

        // Sau khi đánh dấu đọc: push CONVERSATION_UPDATED cho chính người đọc
        // → unread count về 0 để cập nhật badge ngay lập tức
        ConversationUpdatedEventDTO updatedForReader = buildConversationUpdated(
                conversation, userId, "MESSAGES_READ");
        rabbitMQProducer.sendConversationUpdated(userId, updatedForReader);

        // Push cho người kia để họ biết tin nhắn của mình đã được đọc (cập nhật inbox preview)
        ConversationUpdatedEventDTO updatedForOther = buildConversationUpdated(
                conversation, otherUser.getId(), "MESSAGES_READ");
        rabbitMQProducer.sendConversationUpdated(otherUser.getId(), updatedForOther);
    }

    /**
     * Lấy danh sách cuộc trò chuyện của người dùng kèm thông tin đối phương và số tin chưa đọc.
     */
    @Transactional(readOnly = true)
    public List<ConversationResponseDTO> getConversations(UUID userId) {
        List<Conversation> list = conversationRepository.findAllByUserId(userId);
        return list.stream().map(c -> {
            TaiKhoan other = c.getUser1().getId().equals(userId) ? c.getUser2() : c.getUser1();
            long unread = messageRepository.countUnreadMessages(c.getId(), userId);
            var lastMsg = messageRepository.findFirstByConversationIdOrderBySentAtDesc(c.getId());

            return ConversationResponseDTO.builder()
                    .conversationId(c.getId().toString())
                    .otherUserId(other.getId().toString())
                    .otherUserName(other.getUserName())
                    .otherTenTaiKhoan(other.getTenTaiKhoan())
                    .lastMessageContent(lastMsg.map(Message::getContent).orElse(null))
                    .lastMessageAt(c.getLastMessageAt())
                    .unreadCount(unread)
                    .createdAt(c.getCreatedAt())
                    .build();
        }).toList();
    }

    /**
     * Lấy lịch sử tin nhắn của một cuộc trò chuyện.
     */
    @Transactional(readOnly = true)
    public List<MessageResponseDTO> getMessages(UUID conversationId, UUID userId, int page, int size) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Cuộc trò chuyện không tồn tại"));

        boolean isMember = conversation.getUser1().getId().equals(userId)
                || conversation.getUser2().getId().equals(userId);
        if (!isMember) {
            throw new IllegalArgumentException("Bạn không có quyền xem cuộc trò chuyện này");
        }

        var pageRequest = PageRequest.of(page, size);
        return messageRepository.findByConversationIdOrderBySentAtDesc(conversationId, pageRequest)
                .map(this::toMessageDTO)
                .getContent();
    }

    /**
     * Khởi tạo hoặc lấy lại cuộc trò chuyện hiện có với partner.
     */
    @Transactional
    public ConversationResponseDTO createOrGetConversation(UUID userId, UUID partnerId) {
        if (userId.equals(partnerId)) {
            throw new IllegalArgumentException("Không thể tạo cuộc trò chuyện với chính mình");
        }

        TaiKhoan user = taiKhoanRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));
        TaiKhoan partner = taiKhoanRepository.findById(partnerId)
                .orElseThrow(() -> new IllegalArgumentException("Partner không tồn tại"));

        Conversation conversation = conversationRepository.findBetweenUsers(userId, partnerId)
                .orElseGet(() -> conversationRepository.save(new Conversation(user, partner)));

        long unread = messageRepository.countUnreadMessages(conversation.getId(), userId);
        var lastMsg = messageRepository.findFirstByConversationIdOrderBySentAtDesc(conversation.getId());

        return ConversationResponseDTO.builder()
                .conversationId(conversation.getId().toString())
                .otherUserId(partner.getId().toString())
                .otherUserName(partner.getUserName())
                .otherTenTaiKhoan(partner.getTenTaiKhoan())
                .lastMessageContent(lastMsg.map(Message::getContent).orElse(null))
                .lastMessageAt(conversation.getLastMessageAt())
                .unreadCount(unread)
                .createdAt(conversation.getCreatedAt())
                .build();
    }

    /**
     * Tạo ConversationUpdatedEventDTO từ conversation, nhìn từ góc nhìn của userId.
     *
     * @param conversation  cuộc trò chuyện
     * @param userId        userId của người nhận event (để tính unreadCount từ góc nhìn họ)
     * @param reason        "NEW_MESSAGE" hoặc "MESSAGES_READ"
     * @return DTO sẵn sàng publish
     */
    private ConversationUpdatedEventDTO buildConversationUpdated(
            Conversation conversation, UUID userId, String reason) {
        TaiKhoan other = conversation.getUser1().getId().equals(userId)
                ? conversation.getUser2()
                : conversation.getUser1();
        long unread = messageRepository.countUnreadMessages(conversation.getId(), userId);
        return ConversationUpdatedEventDTO.builder()
                .conversationId(conversation.getId().toString())
                .otherUserId(other.getId().toString())
                .otherUserName(other.getUserName())
                .otherTenTaiKhoan(other.getTenTaiKhoan())
                .lastMessageContent(messageRepository
                        .findFirstByConversationIdOrderBySentAtDesc(conversation.getId())
                        .map(Message::getContent).orElse(null))
                .lastMessageAt(conversation.getLastMessageAt())
                .unreadCount(unread)
                .reason(reason)
                .build();
    }

    private MessageResponseDTO toMessageDTO(Message message) {
        Instant sentInstant = message.getSentAt() != null ? message.getSentAt() : Instant.now();
        LocalDateTime sentAt = LocalDateTime.ofInstant(sentInstant, ZoneOffset.UTC);
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
