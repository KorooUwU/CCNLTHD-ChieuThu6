package com.notification.dispatcher.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ConversationCreatedResponseDTO {
    private String conversationId;
    private String partnerId;
    private String partnerName;
}