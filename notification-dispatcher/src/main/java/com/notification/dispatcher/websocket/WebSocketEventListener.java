package com.notification.dispatcher.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.UUID;

/**
 * Lắng nghe sự kiện kết nối/ngắt kết nối WebSocket để cập nhật trạng thái presence.
 *
 * <p>Spring phát ra {@link SessionConnectedEvent} sau khi STOMP CONNECT được xử lý thành công
 * (tức là sau khi {@link StompAuthChannelInterceptor} đã gắn principal vào session).
 * {@link SessionDisconnectEvent} được phát khi connection đóng theo bất kỳ nguyên nhân nào.
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final UserPresenceService presenceService;

    public WebSocketEventListener(UserPresenceService presenceService) {
        this.presenceService = presenceService;
    }

    /**
     * Xử lý khi client STOMP CONNECT thành công.
     * Đánh dấu người dùng ONLINE và broadcast presence.
     */
    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        var accessor = StompHeaderAccessor.wrap(event.getMessage());
        UUID userId = extractUserId(accessor);
        if (userId != null) {
            presenceService.markOnline(userId);
            log.info("WebSocket connected: userId={}", userId);
        }
    }

    /**
     * Xử lý khi client ngắt kết nối (bình thường, timeout, hoặc lỗi mạng).
     * Đánh dấu người dùng OFFLINE và broadcast presence.
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        var accessor = StompHeaderAccessor.wrap(event.getMessage());
        UUID userId = extractUserId(accessor);
        if (userId != null) {
            presenceService.markOffline(userId);
            log.info("WebSocket disconnected: userId={}, sessionId={}", userId, event.getSessionId());
        }
    }

    /**
     * Trích xuất userId từ principal của session WebSocket.
     * Principal được gắn vào trong {@link StompAuthChannelInterceptor#preSend}.
     *
     * @return UUID của người dùng, hoặc {@code null} nếu session không có xác thực
     */
    private UUID extractUserId(StompHeaderAccessor accessor) {
        var principal = accessor.getUser();
        if (principal instanceof JwtAuthenticationToken jwt) {
            String sub = jwt.getToken().getSubject();
            try {
                return UUID.fromString(sub);
            } catch (IllegalArgumentException e) {
                log.warn("JWT subject không phải UUID hợp lệ: {}", sub);
            }
        }
        return null;
    }
}
