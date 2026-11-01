package com.notification.dispatcher.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO trả về thông tin tóm tắt của một cuộc trò chuyện trong danh sách inbox.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponseDTO {

    private String conversationId;

    /** Thông tin người dùng kia (không phải người đang đăng nhập) */
    private String otherUserId;
    private String otherUserName;
    private String otherTenTaiKhoan;

    /** Nội dung tin nhắn cuối cùng trong cuộc trò chuyện */
    private String lastMessageContent;
    private Instant lastMessageAt;

    /** Số tin nhắn chưa đọc của người dùng hiện tại trong cuộc trò chuyện */
    private long unreadCount;

    /** Thời điểm tạo cuộc trò chuyện */
    private Instant createdAt;
}
