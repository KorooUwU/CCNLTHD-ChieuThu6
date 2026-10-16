package com.notification.dispatcher.config;

import com.notification.dispatcher.websocket.StompAuthChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Cấu hình WebSocket Message Broker với STOMP protocol.
 *
 * <p>Luồng kết nối:
 * <ol>
 *   <li>Client kết nối đến {@code /ws} với header {@code Authorization: Bearer <token>}</li>
 *   <li>{@link StompAuthChannelInterceptor} xác thực JWT tại STOMP CONNECT frame</li>
 *   <li>Client subscribe các destination sau:
 *     <ul>
 *       <li>{@code /topic/conversation.{id}} – tin nhắn mới trong cuộc trò chuyện</li>
 *       <li>{@code /topic/conversation.{id}.typing} – sự kiện đang soạn tin</li>
 *       <li>{@code /topic/presence} – trạng thái online/offline của người dùng</li>
 *       <li>{@code /user/queue/notifications} – thông báo cá nhân (mapped tự động theo user)</li>
 *     </ul>
 *   </li>
 *   <li>Client gửi message đến:
 *     <ul>
 *       <li>{@code /app/chat.send} – gửi tin nhắn mới</li>
 *       <li>{@code /app/chat.typing} – báo hiệu đang soạn tin</li>
 *     </ul>
 *   </li>
 * </ol>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    public WebSocketConfig(StompAuthChannelInterceptor stompAuthChannelInterceptor) {
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // In-memory broker xử lý /topic (broadcast nhiều người) và /queue (cá nhân)
        config.enableSimpleBroker("/topic", "/queue");
        // Prefix cho các message từ client gửi lên @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");
        // Prefix cho user-specific destination: /user/{userId}/queue/...
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint WebSocket native (không dùng SockJS để hỗ trợ client chuẩn WS)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    /**
     * Đăng ký ChannelInterceptor để xác thực JWT tại bước STOMP CONNECT.
     * Sau khi xác thực thành công, {@code accessor.setUser()} được gọi và tất cả
     * các message tiếp theo trong session sẽ mang thông tin principal này.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
