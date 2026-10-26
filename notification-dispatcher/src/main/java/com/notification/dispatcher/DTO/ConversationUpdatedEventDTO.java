package com.notification.dispatcher.DTO;

import lombok.*;
import java.time.Instant;

/**
 * Sự kiện cập nhật danh sách cuộc trò chuyện (inbox) realtime.
 * Được push qua SSE khi có tin nhắn mới hoặc khi đánh dấu đã đọc.
 * Client dùng event này để cập nhật preview và unread badge mà không cần reload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationUpdatedEventDTO {

    /** ID cuộc trò chuyện được cập nhật */
    private String conversationId;

    /** Thông tin người dùng kia trong cuộc trò chuyện */
    private String otherUserId;
    private String otherUserName;
    private String otherTenTaiKhoan;

    /** Preview tin nhắn cuối */
    private String lastMessageContent;
    private Instant lastMessageAt;

    /** Số tin nhắn chưa đọc của người nhận event này */
    private long unreadCount;

    /**
     * Lý do update: "NEW_MESSAGE" hoặc "MESSAGES_READ"
     * Giúp client biết nguyên nhân update để xử lý animation/âm thanh đúng.
     */
    private String reason;
}
