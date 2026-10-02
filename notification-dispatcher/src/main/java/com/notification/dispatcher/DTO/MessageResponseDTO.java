package com.notification.dispatcher.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// DTO này được dùng để hiển thị thông tin của một tin nhắn
public class MessageResponseDTO {
    private String idMessage;
    private String conversationId;
    private String senderId;
    private String senderName;
    private String messageContent;
    private LocalDateTime sendAt;
    private boolean IsRead;

}
