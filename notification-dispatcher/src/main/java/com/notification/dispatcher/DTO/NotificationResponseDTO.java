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
public class NotificationResponseDTO {

    private String idNotification;
    private String type; // loại thông báo
    private boolean isRead;

    // thông tin người gửi tin nhắn để hiển thị trên thông báo
    private String senderId; // mapping sang userid gửi tin nhắn
    private String senderName; // mapping sang username gửi tin nhắn

    private String MessageContent; // tin nhắn lấy từ Content trong bảng Message

    private String conversationId;
    private LocalDateTime sendAt;
}
