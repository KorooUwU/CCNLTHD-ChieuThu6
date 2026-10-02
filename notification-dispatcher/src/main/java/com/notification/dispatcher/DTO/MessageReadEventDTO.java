package com.notification.dispatcher.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// DTO gửi tín hiệu đối phương đã đọc tin nhắn trong cuộc hội thoại thời gian thực
public class MessageReadEventDTO {

    @NotBlank(message = "Conversation ID không được để trống")
    private String conversationId;

    @NotBlank(message = "Message ID không được để trống")
    private String messageId;

    private String readerId;
    private LocalDateTime readAt;

}
