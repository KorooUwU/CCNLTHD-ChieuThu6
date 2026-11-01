package com.notification.dispatcher.sse;

import com.notification.dispatcher.presence.PresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Quản lý kết nối Server-Sent Events (SSE) theo từng người dùng.
 *
 * <p>Mỗi người dùng có thể mở nhiều tab/thiết bị, do đó lưu danh sách
 * {@link SseEmitter} trong {@link CopyOnWriteArrayList}.
 *
 * <p>Tính năng:
 * <ul>
 *   <li>Đăng ký kết nối SSE với timeout 30 phút</li>
 *   <li>Gửi sự kiện cá nhân (sendToUser) và broadcast cho tất cả</li>
 *   <li>Tự động dọn dẹp kết nối khi đóng, timeout hoặc lỗi mạng</li>
 *   <li>Heartbeat định kỳ 25 giây gửi comment "ping" để giữ kết nối qua proxy/firewall</li>
 * </ul>
 */
@Service
@EnableScheduling
public class SseService {

    private static final Logger log = LoggerFactory.getLogger(SseService.class);

    // Timeout 30 phút (ms)
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    /** Map lưu userId -> danh sách các SSE emitter đang mở */
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    /** PresenceService để cập nhật trạng thái Online/Offline khi kết nối SSE thay đổi */
    private final PresenceService presenceService;

    /**
     * @param presenceService @Lazy để tránh circular dependency (PresenceService → RabbitMQProducer → RabbitTemplate)
     */
    public SseService(@Lazy PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    /**
     * Tạo và đăng ký một SSE Emitter mới cho người dùng.
     *
     * @param userId UUID người dùng
     * @return SseEmitter sẵn sàng truyền dữ liệu
     */
    public SseEmitter subscribe(UUID userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        boolean wasOffline = !isUserOnline(userId);
        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> removeEmitter(userId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        // Gửi sự kiện chào mừng ngay sau khi kết nối thành công
        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .data(Map.of(
                            "status", "CONNECTED",
                            "userId", userId.toString(),
                            "message", "Kết nối SSE thành công"
                    ), MediaType.APPLICATION_JSON));
            log.info("SSE client connected: userId={}, activeCount={}", userId, userEmitters.get(userId).size());
        } catch (IOException e) {
            log.warn("Lỗi gửi event CONNECTED tới userId={}: {}", userId, e.getMessage());
            cleanup.run();
            return emitter;
        }

        // Đánh dấu ONLINE sau khi emitter đã ổn định (chỉ broadcast lần đầu kết nối)
        if (wasOffline) {
            presenceService.markOnline(userId);
        }

        return emitter;
    }

    /**
     * Gửi sự kiện realtime đến một người dùng cụ thể.
     *
     * @param userId    UUID người nhận
     * @param eventName tên sự kiện (NEW_MESSAGE, NOTIFICATION, TYPING,...)
     * @param data      dữ liệu sự kiện
     */
    public void sendToUser(UUID userId, String eventName, Object data) {
        CopyOnWriteArrayList<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("Người dùng {} hiện không có kết nối SSE active, bỏ qua push", userId);
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException e) {
                log.warn("Không thể gửi SSE tới userId={}, đang dọn dẹp emitter hỏng: {}", userId, e.getMessage());
                removeEmitter(userId, emitter);
            }
        }
    }

    /**
     * Broadcast sự kiện đến toàn bộ người dùng đang online.
     *
     * @param eventName tên sự kiện
     * @param data      dữ liệu sự kiện
     */
    public void broadcast(String eventName, Object data) {
        userEmitters.forEach((userId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name(eventName)
                            .data(data, MediaType.APPLICATION_JSON));
                } catch (IOException | IllegalStateException e) {
                    removeEmitter(userId, emitter);
                }
            }
        });
    }

    /**
     * Kiểm tra xem người dùng có ít nhất 1 kết nối SSE đang mở hay không.
     */
    public boolean isUserOnline(UUID userId) {
        CopyOnWriteArrayList<SseEmitter> list = userEmitters.get(userId);
        return list != null && !list.isEmpty();
    }

    /**
     * Trả về danh sách UUID của các người dùng đang online.
     */
    public Set<UUID> getOnlineUserIds() {
        return Collections.unmodifiableSet(userEmitters.keySet());
    }

    /**
     * Heartbeat định kỳ 25 giây gửi SSE comment để giữ kết nối qua proxy/firewall
     * và phát hiện sớm các kết nối đã bị đứt ở phía client.
     */
    @Scheduled(fixedRate = 25000)
    public void sendHeartbeat() {
        userEmitters.forEach((userId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (IOException | IllegalStateException e) {
                    removeEmitter(userId, emitter);
                }
            }
        });
    }

    private void removeEmitter(UUID userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
                log.info("Tất cả kết nối SSE của userId={} đã đóng → đánh dấu OFFLINE", userId);
                presenceService.markOffline(userId);
            }
        }
    }
}
