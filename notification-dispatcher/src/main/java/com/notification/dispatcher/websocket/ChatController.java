package com.notification.dispatcher.websocket;

import com.notification.dispatcher.DTO.MessageResponseDTO;
import com.notification.dispatcher.DTO.SendMessageRequestDTO;
import com.notification.dispatcher.DTO.TypingEventDTO;
import com.notification.dispatcher.entity.Conversation;
import com.notification.dispatcher.service.ChatService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * STOMP Controller xử lý tin nhắn chat và typing event realtime.
 *
 * <h3>Destination mapping</h3>
 * <table>
 *   <tr><th>Client publish (→ server)</th><th>Server broadcast (→ clients)</th></tr>
 *   <tr>
 *     <td>{@code /app/chat.send}</td>
 *     <td>{@code /topic/conversation.{conversationId}} (toàn bộ participants)</td>
 *   </tr>
 *   <tr>
 *     <td></td>
 *     <td>{@code /user/{recipientId}/queue/notifications} (cá nhân người nhận)</td>
 *   </tr>
 *   <tr>
 *     <td>{@code /app/chat.typing}</td>
 *     <td>{@code /topic/conversation.{conversationId}.typing}</td>
 *   </tr>
 * </table>
 */
@Controller
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Nhận tin nhắn từ client, lưu DB và broadcast cho tất cả thành viên cuộc trò chuyện.
     *
     * <p>Client gửi đến {@code /app/chat.send}.
     * Server broadcast đến {@code /topic/conversation.{conversationId}}.
     *
     * @param request   payload JSON từ client (conversationId + messageContent)
     * @param principal JWT principal được gắn bởi {@link StompAuthChannelInterceptor}
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Valid @Payload SendMessageRequestDTO request, Principal principal) {
        UUID senderId = extractUserId(principal);

        // 1. Lưu vào DB
        MessageResponseDTO response = chatService.persistMessage(request, senderId);

        // 2. Broadcast cho tất cả thành viên trong conversation room
        String roomDestination = "/topic/conversation." + response.getConversationId();
        messagingTemplate.convertAndSend(roomDestination, response);

        // 3. Gửi thêm đến personal queue của người nhận (để hiện badge thông báo)
        Conversation conversation = chatService.getConversation(
                UUID.fromString(response.getConversationId()));
        chatService.pushToRecipient(response, senderId, conversation);

        log.debug("Message sent: conversationId={}, senderId={}", response.getConversationId(), senderId);
    }

    /**
     * Nhận sự kiện đang soạn tin và broadcast trong conversation room.
     *
     * <p>Client gửi đến {@code /app/chat.typing}.
     * Server broadcast đến {@code /topic/conversation.{conversationId}.typing}.
     *
     * <p>senderId trong payload được ghi đè bởi userId từ JWT để ngăn giả mạo.
     *
     * @param event     payload JSON chứa conversationId và trạng thái isTyping
     * @param principal JWT principal được gắn bởi {@link StompAuthChannelInterceptor}
     */
    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingEventDTO event, Principal principal) {
        UUID senderId = extractUserId(principal);

        // Ghi đè senderId từ JWT, không tin dùng giá trị client tự gửi
        TypingEventDTO verified = TypingEventDTO.builder()
                .conversationId(event.getConversationId())
                .senderId(senderId.toString())
                .isTyping(event.isTyping())
                .build();

        String destination = "/topic/conversation." + event.getConversationId() + ".typing";
        messagingTemplate.convertAndSend(destination, verified);
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    /**
     * Trích xuất UUID của người dùng từ JWT principal trong WebSocket session.
     *
     * @param principal principal được đặt bởi {@link StompAuthChannelInterceptor}
     * @return UUID người dùng
     * @throws IllegalStateException nếu session không có xác thực hợp lệ
     */
    private UUID extractUserId(Principal principal) {
        if (principal instanceof JwtAuthenticationToken jwt) {
            return UUID.fromString(jwt.getToken().getSubject());
        }
        throw new IllegalStateException(
                "Không tìm thấy principal hợp lệ trong WebSocket session");
    }
}
