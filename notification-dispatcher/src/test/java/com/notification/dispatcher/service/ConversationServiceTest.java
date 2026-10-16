package com.notification.dispatcher.service;

import com.notification.dispatcher.entity.Conversation;
import com.notification.dispatcher.entity.Message;
import com.notification.dispatcher.entity.TaiKhoan;
import com.notification.dispatcher.exception.UserNotFoundException;
import com.notification.dispatcher.repository.ConversationRepository;
import com.notification.dispatcher.repository.MessageRepository;
import com.notification.dispatcher.repository.TaiKhoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConversationServiceTest {

    private ConversationRepository conversationRepository;
    private MessageRepository messageRepository;
    private TaiKhoanRepository taiKhoanRepository;
    private ConversationService service;

    @BeforeEach
    void setUp() {
        conversationRepository = mock(ConversationRepository.class);
        messageRepository = mock(MessageRepository.class);
        taiKhoanRepository = mock(TaiKhoanRepository.class);
        service = new ConversationService(
                conversationRepository,
                messageRepository,
                taiKhoanRepository);
    }

    @Test
    void createsConversationBetweenCurrentUserAndPartner() {
        UUID currentUserId = UUID.randomUUID();
        UUID partnerId = UUID.randomUUID();
        TaiKhoan currentUser = user(currentUserId, "User A");
        TaiKhoan partner = user(partnerId, "User B");
        Conversation conversation = new Conversation(currentUser, partner);
        conversation.setId(UUID.randomUUID());

        when(taiKhoanRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(taiKhoanRepository.findById(partnerId)).thenReturn(Optional.of(partner));
        when(conversationRepository.findBetweenUsers(currentUserId, partnerId))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

        var response = service.createOrGetConversation(currentUserId, partnerId);

        assertEquals(conversation.getId().toString(), response.getConversationId());
        assertEquals(partnerId.toString(), response.getPartnerId());
        assertEquals("User B", response.getPartnerName());
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    void returnsExistingConversationWithoutCreatingDuplicate() {
        UUID currentUserId = UUID.randomUUID();
        UUID partnerId = UUID.randomUUID();
        TaiKhoan currentUser = user(currentUserId, "User A");
        TaiKhoan partner = user(partnerId, "User B");
        Conversation conversation = new Conversation(partner, currentUser);
        conversation.setId(UUID.randomUUID());

        when(taiKhoanRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(taiKhoanRepository.findById(partnerId)).thenReturn(Optional.of(partner));
        when(conversationRepository.findBetweenUsers(currentUserId, partnerId))
                .thenReturn(Optional.of(conversation));

        var response = service.createOrGetConversation(currentUserId, partnerId);

        assertEquals(conversation.getId().toString(), response.getConversationId());
        verify(conversationRepository, never()).save(any());
    }

    @Test
    void rejectsConversationWithSelf() {
        UUID userId = UUID.randomUUID();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createOrGetConversation(userId, userId));

        verifyNoInteractions(taiKhoanRepository, conversationRepository);
    }

    @Test
    void rejectsMissingPartner() {
        UUID currentUserId = UUID.randomUUID();
        UUID partnerId = UUID.randomUUID();
        TaiKhoan currentUser = user(currentUserId, "User A");

        when(taiKhoanRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(taiKhoanRepository.findById(partnerId)).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> service.createOrGetConversation(currentUserId, partnerId));
    }

    @Test
    void returnsLastMessageAndUnreadCountForCurrentUser() {
        UUID currentUserId = UUID.randomUUID();
        UUID partnerId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        TaiKhoan currentUser = user(currentUserId, "User A");
        TaiKhoan partner = user(partnerId, "User B");
        Conversation conversation = new Conversation(currentUser, partner);
        conversation.setId(conversationId);
        Message lastMessage = new Message(conversation, partner, "Hello");
        lastMessage.setSentAt(Instant.parse("2026-10-15T10:00:00Z"));

        when(conversationRepository.findAllByUserId(currentUserId))
                .thenReturn(List.of(conversation));
        when(messageRepository.findFirstByConversationIdOrderBySentAtDesc(conversationId))
                .thenReturn(Optional.of(lastMessage));
        when(messageRepository.countUnreadMessages(conversationId, currentUserId))
                .thenReturn(2L);

        var response = service.getConversations(currentUserId);

        assertEquals(1, response.size());
        assertEquals(partnerId.toString(), response.getFirst().getPartnerId());
        assertEquals("Hello", response.getFirst().getLastMessageContent());
        assertEquals(2, response.getFirst().getUnreadCount());
        assertEquals(
                "2026-10-15T10:00",
                response.getFirst().getLastMessageAt().toString());
    }

    @Test
    void returnsEmptyLastMessageForConversationWithoutMessages() {
        UUID currentUserId = UUID.randomUUID();
        UUID partnerId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = new Conversation(
                user(currentUserId, "User A"),
                user(partnerId, "User B"));
        conversation.setId(conversationId);

        when(conversationRepository.findAllByUserId(currentUserId))
                .thenReturn(List.of(conversation));
        when(messageRepository.findFirstByConversationIdOrderBySentAtDesc(conversationId))
                .thenReturn(Optional.empty());
        when(messageRepository.countUnreadMessages(conversationId, currentUserId))
                .thenReturn(0L);

        var response = service.getConversations(currentUserId);

        assertNull(response.getFirst().getLastMessageContent());
        assertNull(response.getFirst().getLastMessageAt());
        assertEquals(0, response.getFirst().getUnreadCount());
    }

    private TaiKhoan user(UUID id, String name) {
        TaiKhoan user = new TaiKhoan();
        user.setId(id);
        user.setTenTaiKhoan(name);
        return user;
    }
}
