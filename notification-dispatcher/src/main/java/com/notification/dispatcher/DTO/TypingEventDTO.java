package com.notification.dispatcher.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// DTO phát tín hiệu đang soạn tin nhắn thời gian thực giữa các người dùng
public class TypingEventDTO {

    @NotBlank(message = "Conversation ID không được để trống")
    private String conversationId;

    @NotBlank(message = "Sender ID không được để trống")
    private String senderId;

    @com.fasterxml.jackson.annotation.JsonProperty("isTyping")
    @com.fasterxml.jackson.annotation.JsonAlias("typing")
    private boolean isTyping;

}
