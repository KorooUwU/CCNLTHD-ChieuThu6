package com.notification.dispatcher.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * Controller cung cấp endpoint SSE để client đăng ký nhận luồng sự kiện realtime.
 *
 * <p>Endpoint hỗ trợ 2 hình thức xác thực JWT:
 * <ul>
 *   <li><b>Header:</b> {@code Authorization: Bearer <token>} (dành cho fetch/Axios, Mobile, Postman)</li>
 *   <li><b>Query param:</b> {@code ?token=<jwt>} (dành cho browser {@code new EventSource('/api/notifications/stream?token=...')})</li>
 * </ul>
 */
@RestController
public class SseController {

    private static final Logger log = LoggerFactory.getLogger(SseController.class);

    private final SseService sseService;
    private final JwtDecoder jwtDecoder;

    public SseController(SseService sseService, JwtDecoder jwtDecoder) {
        this.sseService = sseService;
        this.jwtDecoder = jwtDecoder;
    }

    /**
     * Endpoint chính để đăng ký luồng SSE thông báo và tin nhắn realtime.
     *
     * <p>Client nhận các sự kiện:
     * <ul>
     *   <li>{@code CONNECTED}: Xác nhận kết nối thành công</li>
     *   <li>{@code NOTIFICATION}: Thông báo mới</li>
     *   <li>{@code NEW_MESSAGE}: Tin nhắn chat mới</li>
     *   <li>{@code TYPING}: Báo hiệu đối phương đang soạn tin</li>
     *   <li>{@code MESSAGE_READ}: Xác nhận đã đọc tin</li>
     *   <li>{@code PRESENCE}: Trạng thái online/offline của người dùng</li>
     * </ul>
     */
    @GetMapping(value = {"/api/notifications/stream", "/api/sse/stream"}, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "token", required = false) String tokenParam) {

        UUID userId = resolveUserId(jwt, tokenParam);
        log.info("Client bắt đầu mở SSE stream: userId={}", userId);
        return sseService.subscribe(userId);
    }

    private UUID resolveUserId(Jwt jwt, String tokenParam) {
        if (jwt != null) {
            try {
                return UUID.fromString(jwt.getSubject());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Subject trong JWT không phải UUID hợp lệ");
            }
        }

        if (tokenParam != null && !tokenParam.isBlank()) {
            try {
                Jwt decodedJwt = jwtDecoder.decode(tokenParam.strip());
                return UUID.fromString(decodedJwt.getSubject());
            } catch (JwtException | IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token không hợp lệ hoặc đã hết hạn: " + e.getMessage());
            }
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cần cung cấp JWT token qua header Bearer hoặc query param ?token=");
    }
}
