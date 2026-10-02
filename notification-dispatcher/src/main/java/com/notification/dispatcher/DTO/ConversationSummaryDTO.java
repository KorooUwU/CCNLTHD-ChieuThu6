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
public class ConversationSummaryDTO {
    private String idConversation; 

    private String partnerId;
    private String partnerName;

    private String lastMessageContent;
    private LocalDateTime lastMessageAt;

    private int unreadCount;
}