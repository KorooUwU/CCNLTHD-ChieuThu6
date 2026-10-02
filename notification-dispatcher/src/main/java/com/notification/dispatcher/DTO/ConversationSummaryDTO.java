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
// DTO này được dùng để lấy danh sách các cuộc trò chuyện

public class ConversationSummaryDTO {
    private String conversationId; 

    private String partnerId;
    private String partnerName;
    private String partnerAvatarUrl;

    private String lastMessageContent;
    private LocalDateTime lastMessageAt;

    private int unreadCount;
}