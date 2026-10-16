package com.notification.dispatcher.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class GroupConversationResponseDTO {
    private String conversationId;
    private String groupName;
    private List<UserSummaryDTO> members;
}
