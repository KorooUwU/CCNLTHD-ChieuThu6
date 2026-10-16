package com.notification.dispatcher.controller;

import com.notification.dispatcher.DTO.*;
import com.notification.dispatcher.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API cho chức năng Chat (gửi tin nhắn, danh sách hội thoại, lịch sử, typing, read receipt).
 * Tin nhắn gửi lên sẽ được lưu vào DB và publish qua RabbitMQ tới SSE của người nhận.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Gửi tin nhắn mới.
     */
    @PostMapping("/send")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponseDTO sendMessage(
            @Valid @RequestBody SendMessageRequestDTO request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID senderId = UUID.fromString(jwt.getSubject());
        return chatService.sendMessage(request, senderId);
    }

    /**
     * Gửi tín hiệu đang soạn tin (typing indicator).
     */
    @PostMapping("/typing")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void typing(
            @Valid @RequestBody TypingEventDTO event,
            @AuthenticationPrincipal Jwt jwt) {
        UUID senderId = UUID.fromString(jwt.getSubject());
        chatService.sendTyping(event, senderId);
    }

    /**
     * Đánh dấu đã đọc tin nhắn trong cuộc trò chuyện.
     */
    @PostMapping("/conversations/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        chatService.markAsRead(id, userId);
    }

    /**
     * Lấy danh sách cuộc trò chuyện của người dùng hiện tại (hộp thư inbox).
     */
    @GetMapping("/conversations")
    public List<ConversationResponseDTO> getConversations(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return chatService.getConversations(userId);
    }

    /**
     * Tạo hoặc lấy lại cuộc trò chuyện với một đối tác (partner).
     */
    @PostMapping("/conversations")
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponseDTO createOrGetConversation(
            @Valid @RequestBody CreateConversationRequestDTO request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        UUID partnerId = UUID.fromString(request.getPartnerId());
        return chatService.createOrGetConversation(userId, partnerId);
    }

    /**
     * Lấy lịch sử tin nhắn trong một cuộc trò chuyện (phân trang).
     */
    @GetMapping("/conversations/{id}/messages")
    public List<MessageResponseDTO> getMessages(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return chatService.getMessages(id, userId, page, size);
    }
}
