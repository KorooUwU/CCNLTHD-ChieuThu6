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
public class MessageResponseDTO {
    private String idMessage;
    private String conversationId;
    private String senderId;
    private String senderName;
    private String messageContent;
    private LocalDateTime sendAt;
    private boolean IsRead;

}
