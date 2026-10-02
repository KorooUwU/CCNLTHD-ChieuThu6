package com.notification.dispatcher.DTO;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
// DTO này được dùng để gửi tin nhắn
public class SendMessageRequestDTO {

    @NotBlank(message = "Conversation ID ko được để trống")
    private String conversationId;

    private String MessageContent;

    private String receiverId;

}
