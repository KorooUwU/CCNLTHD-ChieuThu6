package com.notification.dispatcher.controller;

import com.notification.dispatcher.DTO.MessageResponseDTO;
import com.notification.dispatcher.DTO.SendMessageRequestDTO;
import com.notification.dispatcher.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/messages")
    public ResponseEntity<MessageResponseDTO> sendMessage(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SendMessageRequestDTO request) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        MessageResponseDTO response = messageService.sendMessage(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<MessageResponseDTO>> getMessages(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID conversationId) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(messageService.getMessages(currentUserId, conversationId));
    }
}
