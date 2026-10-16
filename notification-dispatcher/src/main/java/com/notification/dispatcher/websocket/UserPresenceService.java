package com.notification.dispatcher.websocket;

import com.notification.dispatcher.DTO.UserPresenceDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý trạng thái hiện diện (presence) của người dùng trong bộ nhớ.
 *
 * <p>Khi người dùng kết nối WebSocket thành công, họ được đánh dấu ONLINE.
 * Khi ngắt kết nối (disconnect/timeout), họ được đánh dấu OFFLINE.
 * Mỗi thay đổi trạng thái được broadcast đến {@code /topic/presence}
 * để các client đang subscribe có thể cập nhật UI realtime.
 *
 * <p>Lưu ý: implementation này dùng {@link ConcurrentHashMap} trong bộ nhớ
 * (phù hợp cho single-node deployment). Khi scale multi-node cần thay bằng
 * Redis Pub/Sub hoặc tương tự.
 */
@Service
public class UserPresenceService {

    /** Map từ userId → trạng thái ("ONLINE"). Vắng mặt trong map = OFFLINE. */
    private final ConcurrentHashMap<UUID, LocalDateTime> onlineSessions = new ConcurrentHashMap<>();

    private final SimpMessagingTemplate messagingTemplate;

    public UserPresenceService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Đánh dấu người dùng là ONLINE và broadcast đến {@code /topic/presence}.
     *
     * @param userId UUID của người dùng vừa kết nối
     */
    public void markOnline(UUID userId) {
        onlineSessions.put(userId, LocalDateTime.now());
        broadcastPresence(userId, "ONLINE");
    }

    /**
     * Đánh dấu người dùng là OFFLINE và broadcast đến {@code /topic/presence}.
     *
     * @param userId UUID của người dùng vừa ngắt kết nối
     */
    public void markOffline(UUID userId) {
        LocalDateTime lastSeen = onlineSessions.remove(userId);
        broadcastPresence(userId, "OFFLINE");
    }

    /**
     * Kiểm tra xem người dùng có đang online không.
     *
     * @param userId UUID cần kiểm tra
     * @return {@code true} nếu đang có kết nối WebSocket active
     */
    public boolean isOnline(UUID userId) {
        return onlineSessions.containsKey(userId);
    }

    /**
     * Trả về tập hợp UUID của tất cả người dùng đang online (read-only).
     */
    public Set<UUID> getOnlineUserIds() {
        return Collections.unmodifiableSet(onlineSessions.keySet());
    }

    private void broadcastPresence(UUID userId, String status) {
        var payload = UserPresenceDTO.builder()
                .userId(userId.toString())
                .status(status)
                .lastActiveAt(LocalDateTime.now())
                .build();
        // Broadcast cho tất cả client đang theo dõi trạng thái người dùng
        messagingTemplate.convertAndSend("/topic/presence", payload);
    }
}
