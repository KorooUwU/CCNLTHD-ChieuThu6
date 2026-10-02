package com.notification.dispatcher.DTO;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class SendMessageRequestDTO {

    @NotBlank(message = "Conversation ID ko được để trống")
    private String conversationId;

    private String MessagaContent;

    private String receiverId;

}
