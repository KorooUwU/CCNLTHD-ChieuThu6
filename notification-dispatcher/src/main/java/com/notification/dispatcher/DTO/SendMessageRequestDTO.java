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
// DTO này được dùng để gửi tin nhắn
public class SendMessageRequestDTO {

    @NotBlank(message = "Conversation ID không được để trống")
    private String conversationId;

    private String messageContent;

    private String receiverId;

    // Loại tin nhắn (TEXT, IMAGE, FILE...) mặc định là TEXT nếu null
    private String type;

    private String imageUrl;

    private String attachmentPublicId;
}
