package com.notification.dispatcher.service;

import com.notification.dispatcher.entity.Conversation;
import com.notification.dispatcher.entity.ConversationType;
import com.notification.dispatcher.entity.Message;
import com.notification.dispatcher.entity.TaiKhoan;
import com.notification.dispatcher.DTO.CreateGroupConversationRequestDTO;
import com.notification.dispatcher.exception.UserNotFoundException;
import com.notification.dispatcher.repository.ConversationRepository;
import com.notification.dispatcher.repository.MessageRepository;
import com.notification.dispatcher.repository.TaiKhoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

    @Test
    void createsGroupWithCurrentUserAndRequestedMembers() {
        UUID currentUserId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        TaiKhoan currentUser = user(currentUserId, "User A");
        TaiKhoan member = user(memberId, "User B");
        Conversation group = Conversation.group("Team", Set.of(currentUser, member));
        group.setId(UUID.randomUUID());

        when(taiKhoanRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(taiKhoanRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(group);

        var response = service.createGroupConversation(
                currentUserId,
                new CreateGroupConversationRequestDTO("Team", List.of(memberId.toString())));

        assertEquals(group.getId().toString(), response.getConversationId());
        assertEquals("Team", response.getGroupName());
        assertEquals(2, response.getMembers().size());
        assertEquals(ConversationType.GROUP, group.getType());
    }

    @Test
    void addsMemberOnlyWhenCurrentUserIsInGroup() {
        UUID currentUserId = UUID.randomUUID();
        UUID newMemberId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        TaiKhoan currentUser = user(currentUserId, "User A");
        TaiKhoan newMember = user(newMemberId, "User C");
        Conversation group = Conversation.group("Team", Set.of(currentUser));
        group.setId(conversationId);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(group));
        when(taiKhoanRepository.findById(newMemberId)).thenReturn(Optional.of(newMember));
        when(conversationRepository.save(group)).thenReturn(group);

        service.addGroupMember(currentUserId, conversationId, newMemberId);

        assertTrue(group.getMembers().stream().anyMatch(member ->
                member.getId().equals(newMemberId)));
    }

    @Test
    void removesGroupMemberAndKeepsAtLeastOneMember() {
        UUID currentUserId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        TaiKhoan currentUser = user(currentUserId, "User A");
        TaiKhoan member = user(memberId, "User B");
        Conversation group = Conversation.group("Team", Set.of(currentUser, member));
        group.setId(conversationId);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(group));
        when(taiKhoanRepository.findById(memberId)).thenReturn(Optional.of(member));

        service.removeGroupMember(currentUserId, conversationId, memberId);

        assertEquals(Set.of(currentUser), group.getMembers());
        verify(conversationRepository).save(group);
    }

    private TaiKhoan user(UUID id, String name) {
        TaiKhoan user = new TaiKhoan();
        user.setId(id);
        user.setTenTaiKhoan(name);
        return user;
    }
}
