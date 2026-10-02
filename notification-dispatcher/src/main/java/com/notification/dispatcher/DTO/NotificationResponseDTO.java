package com.notification.dispatcher.DTO;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// DTO này được dùng để hiển thị thông tin của một thông báo
public class NotificationResponseDTO {

    private String notificationId;
    private String type; // loại thông báo
    private boolean isRead;

    // thông tin người gửi tin nhắn để hiển thị trên thông báo
    private String senderId; // mapping sang userid gửi tin nhắn
    private String senderName; // mapping sang username gửi tin nhắn

    private String messageContent; // tin nhắn lấy từ Content trong bảng Message

    private String conversationId;
    private LocalDateTime sendAt;
}
