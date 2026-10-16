package com.notification.dispatcher.websocket;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * ChannelInterceptor xác thực JWT Bearer token tại STOMP CONNECT frame.
 *
 * <p>Client phải gửi header trong STOMP CONNECT:
 * <pre>
 *   Authorization: Bearer &lt;jwt-token&gt;
 * </pre>
 *
 * <p>Sau khi xác thực thành công, {@code accessor.setUser()} lưu trữ
 * {@link JwtAuthenticationToken} vào session; các {@code @MessageMapping}
 * method nhận được {@code Principal} đã xác thực từ đây.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;

    public StompAuthChannelInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // Chỉ xác thực tại CONNECT frame; các frame sau kế thừa principal từ session
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AccessDeniedException(
                    "WebSocket CONNECT bị từ chối: thiếu header Authorization Bearer");
        }

        String token = authHeader.substring(7).strip();
        try {
            Jwt jwt = jwtDecoder.decode(token);
            JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
            // Gắn principal vào session — tất cả message sau đó có thể truy cập qua Principal
            accessor.setUser(authentication);
        } catch (JwtException ex) {
            throw new AccessDeniedException(
                    "WebSocket CONNECT bị từ chối: JWT không hợp lệ — " + ex.getMessage());
        }

        return message;
    }
}
