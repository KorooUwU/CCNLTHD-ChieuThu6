package com.notification.dispatcher.controller;

import com.notification.dispatcher.DTO.ConversationCreatedResponseDTO;
import com.notification.dispatcher.DTO.ConversationSummaryDTO;
import com.notification.dispatcher.DTO.CreateConversationRequestDTO;
import com.notification.dispatcher.DTO.CreateGroupConversationRequestDTO;
import com.notification.dispatcher.DTO.GroupConversationResponseDTO;
import com.notification.dispatcher.DTO.GroupMemberRequestDTO;
import com.notification.dispatcher.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/create")
    public ResponseEntity<ConversationCreatedResponseDTO> createConversation(
            @AuthenticationPrincipal Jwt jwt, // Yêu cầu phải dăng nhập để tạo cuộc hội thoại mới
            @Valid @RequestBody CreateConversationRequestDTO request) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        UUID partnerId = UUID.fromString(request.getPartnerId());

        ConversationCreatedResponseDTO response =
                conversationService.createOrGetConversation(currentUserId, partnerId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/get")
    public ResponseEntity<List<ConversationSummaryDTO>> getConversations(
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(conversationService.getConversations(currentUserId));
    }

    @PostMapping("/groups")
    public ResponseEntity<GroupConversationResponseDTO> createGroupConversation(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateGroupConversationRequestDTO request) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                conversationService.createGroupConversation(currentUserId, request));
    }

    @PostMapping("/{conversationId}/members")
    public ResponseEntity<GroupConversationResponseDTO> addGroupMember(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID conversationId,
            @Valid @RequestBody GroupMemberRequestDTO request) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(conversationService.addGroupMember(
                currentUserId, conversationId, UUID.fromString(request.getUserId())));
    }

    @DeleteMapping("/{conversationId}/members/{memberId}")
    public ResponseEntity<Void> removeGroupMember(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID conversationId,
            @PathVariable UUID memberId) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        conversationService.removeGroupMember(currentUserId, conversationId, memberId);
        return ResponseEntity.noContent().build();
    }
}
