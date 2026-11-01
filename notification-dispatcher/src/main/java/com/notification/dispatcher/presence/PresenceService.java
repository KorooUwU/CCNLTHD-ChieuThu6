package com.notification.dispatcher.presence;

import com.notification.dispatcher.DTO.UserPresenceDTO;
import com.notification.dispatcher.rabbitmq.RabbitMQProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý trạng thái Online/Offline của người dùng trong bộ nhớ (In-Memory).
 *
 * <p>Kiến trúc:
 * <ul>
 *   <li>Dùng {@link ConcurrentHashMap} ánh xạ {@code userId} → {@link UserPresenceEntry}
 *       (trạng thái và thời điểm hoạt động cuối).</li>
 *   <li>Khi người dùng mở SSE stream → gọi {@link #markOnline(UUID)} → ghi nhận trạng thái
 *       ONLINE và broadcast sự kiện {@code PRESENCE} qua RabbitMQ.</li>
 *   <li>Khi tất cả SSE emitter của người dùng đóng → gọi {@link #markOffline(UUID)} →
 *       ghi nhận OFFLINE và broadcast sự kiện {@code PRESENCE} qua RabbitMQ.</li>
 * </ul>
 *
 * <p>Vì dữ liệu lưu In-Memory, trạng thái sẽ bị reset khi server khởi động lại.
 * Điều này là hành vi mong đợi (người dùng cần kết nối lại SSE sau khi server restart).
 */
@Service
public class PresenceService {

    private static final Logger log = LoggerFactory.getLogger(PresenceService.class);

    /** In-Memory store: userId → UserPresenceEntry */
    private final ConcurrentHashMap<UUID, UserPresenceEntry> presenceMap = new ConcurrentHashMap<>();

    private final RabbitMQProducer rabbitMQProducer;

    public PresenceService(RabbitMQProducer rabbitMQProducer) {
        this.rabbitMQProducer = rabbitMQProducer;
    }

    // ─────────────────────────────────────────────────────────────
    // Lifecycle hooks được gọi từ SseService
    // ─────────────────────────────────────────────────────────────

    /**
     * Đánh dấu người dùng ONLINE và broadcast sự kiện PRESENCE qua RabbitMQ.
     * Được gọi khi người dùng mở kết nối SSE đầu tiên.
     *
     * @param userId UUID người dùng vừa kết nối
     */
    public void markOnline(UUID userId) {
        UserPresenceEntry entry = presenceMap.compute(userId, (id, existing) -> {
            if (existing != null && "ONLINE".equals(existing.getStatus())) {
                // Đã online (nhiều tab), chỉ cập nhật lastActiveAt
                existing.setLastActiveAt(LocalDateTime.now());
                return existing;
            }
            return new UserPresenceEntry("ONLINE", LocalDateTime.now());
        });

        if ("ONLINE".equals(entry.getStatus())) {
            log.info("User ONLINE: userId={}", userId);
            broadcastPresence(userId, "ONLINE", entry.getLastActiveAt());
        }
    }

    /**
     * Đánh dấu người dùng OFFLINE và broadcast sự kiện PRESENCE qua RabbitMQ.
     * Chỉ được gọi khi SSE emitter CUỐI CÙNG của người dùng đóng.
     *
     * @param userId UUID người dùng vừa ngắt kết nối hoàn toàn
     */
    public void markOffline(UUID userId) {
        LocalDateTime lastActiveAt = LocalDateTime.now();
        presenceMap.put(userId, new UserPresenceEntry("OFFLINE", lastActiveAt));

        log.info("User OFFLINE: userId={}", userId);
        broadcastPresence(userId, "OFFLINE", lastActiveAt);
    }

    // ─────────────────────────────────────────────────────────────
    // Query API
    // ─────────────────────────────────────────────────────────────

    /**
     * Kiểm tra người dùng có đang online không.
     *
     * @param userId UUID cần kiểm tra
     * @return true nếu trạng thái là ONLINE
     */
    public boolean isOnline(UUID userId) {
        UserPresenceEntry entry = presenceMap.get(userId);
        return entry != null && "ONLINE".equals(entry.getStatus());
    }

    /**
     * Lấy thông tin presence của một người dùng cụ thể.
     *
     * @param userId UUID người dùng
     * @return {@link UserPresenceDTO} hoặc trả về OFFLINE nếu chưa có dữ liệu
     */
    public UserPresenceDTO getPresence(UUID userId) {
        UserPresenceEntry entry = presenceMap.get(userId);
        if (entry == null) {
            return UserPresenceDTO.builder()
                    .userId(userId.toString())
                    .status("OFFLINE")
                    .lastActiveAt(null)
                    .build();
        }
        return UserPresenceDTO.builder()
                .userId(userId.toString())
                .status(entry.getStatus())
                .lastActiveAt(entry.getLastActiveAt())
                .build();
    }

    /**
     * Lấy presence của nhiều người dùng cùng lúc (dùng cho inbox: hiển thị ai online).
     *
     * @param userIds danh sách UUID cần truy vấn
     * @return map userId (string) → UserPresenceDTO
     */
    public Map<String, UserPresenceDTO> getPresenceBatch(List<UUID> userIds) {
        Map<String, UserPresenceDTO> result = new LinkedHashMap<>();
        for (UUID userId : userIds) {
            result.put(userId.toString(), getPresence(userId));
        }
        return result;
    }

    /**
     * Lấy tập hợp UUID của tất cả người dùng đang ONLINE.
     *
     * @return Set UUID (unmodifiable snapshot)
     */
    public Set<UUID> getOnlineUserIds() {
        Set<UUID> online = new HashSet<>();
        presenceMap.forEach((userId, entry) -> {
            if ("ONLINE".equals(entry.getStatus())) {
                online.add(userId);
            }
        });
        return Collections.unmodifiableSet(online);
    }

    /**
     * Cập nhật {@code lastActiveAt} mà không thay đổi trạng thái.
     * Hữu ích khi nhận heartbeat ping từ client.
     *
     * @param userId UUID người dùng cần cập nhật
     */
    public void updateLastActive(UUID userId) {
        presenceMap.computeIfPresent(userId, (id, entry) -> {
            entry.setLastActiveAt(LocalDateTime.now());
            return entry;
        });
    }

    // ─────────────────────────────────────────────────────────────
    // Internal
    // ─────────────────────────────────────────────────────────────

    private void broadcastPresence(UUID userId, String status, LocalDateTime lastActiveAt) {
        UserPresenceDTO presenceDTO = UserPresenceDTO.builder()
                .userId(userId.toString())
                .status(status)
                .lastActiveAt(lastActiveAt)
                .build();
        try {
            rabbitMQProducer.sendPresence(presenceDTO);
        } catch (Exception e) {
            log.error("Không thể broadcast PRESENCE event cho userId={}: {}", userId, e.getMessage());
        }
    }
}
