package com.notification.dispatcher.presence;

import java.time.LocalDateTime;

/**
 * Bản ghi trạng thái presence của một người dùng trong In-Memory store.
 *
 * <p>Không dùng record vì cần mutate {@code lastActiveAt} khi cập nhật heartbeat.
 */
public class UserPresenceEntry {

    /** Trạng thái: "ONLINE" hoặc "OFFLINE" */
    private String status;

    /** Thời điểm hoạt động gần nhất (kết nối SSE hoặc gửi tin nhắn) */
    private LocalDateTime lastActiveAt;

    public UserPresenceEntry(String status, LocalDateTime lastActiveAt) {
        this.status = status;
        this.lastActiveAt = lastActiveAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(LocalDateTime lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }
}
