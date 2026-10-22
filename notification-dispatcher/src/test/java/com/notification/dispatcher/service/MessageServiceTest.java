package com.notification.dispatcher.service;

import com.notification.dispatcher.DTO.MessageResponseDTO;
import com.notification.dispatcher.DTO.SendMessageRequestDTO;
import com.notification.dispatcher.entity.Conversation;
import com.notification.dispatcher.entity.Message;
import com.notification.dispatcher.entity.TaiKhoan;
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

class MessageServiceTest {

    private MessageRepository messageRepository;
    private ConversationRepository conversationRepository;
    private TaiKhoanRepository taiKhoanRepository;
    private MessageService service;

    @BeforeEach
    void setUp() {
        messageRepository = mock(MessageRepository.class);
        conversationRepository = mock(ConversationRepository.class);
        taiKhoanRepository = mock(TaiKhoanRepository.class);
        service = new MessageService(messageRepository, conversationRepository, taiKhoanRepository);
    }

    @Test
    void sendTextMessageSuccessfully() {
        UUID senderId = UUID.randomUUID();
        UUID partnerId = UUID.randomUUID();
        UUID convId = UUID.randomUUID();

        TaiKhoan sender = user(senderId, "Alice");
        TaiKhoan partner = user(partnerId, "Bob");
        Conversation conversation = new Conversation(sender, partner);
        conversation.setId(convId);

        when(conversationRepository.findById(convId)).thenReturn(Optional.of(conversation));
        when(taiKhoanRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message m = invocation.getArgument(0);
            m.setId(UUID.randomUUID());
            m.setSentAt(Instant.now());
            return m;
        });

        SendMessageRequestDTO request = SendMessageRequestDTO.builder()
                .conversationId(convId.toString())
                .messageContent("Xin chào")
                .type("TEXT")
                .build();

        MessageResponseDTO response = service.sendMessage(senderId, request);

        assertNotNull(response);
        assertEquals("Xin chào", response.getMessageContent());
        assertEquals("TEXT", response.getType());
        assertNull(response.getImageUrl());
        verify(conversationRepository).save(conversation);
    }

    @Test
    void sendImageMessageWithImageUrlAndPublicId() {
        UUID senderId = UUID.randomUUID();
        UUID partnerId = UUID.randomUUID();
        UUID convId = UUID.randomUUID();

        TaiKhoan sender = user(senderId, "Alice");
        TaiKhoan partner = user(partnerId, "Bob");
        Conversation conversation = new Conversation(sender, partner);
        conversation.setId(convId);

        when(conversationRepository.findById(convId)).thenReturn(Optional.of(conversation));
        when(taiKhoanRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message m = invocation.getArgument(0);
            m.setId(UUID.randomUUID());
            m.setSentAt(Instant.now());
            return m;
        });

        SendMessageRequestDTO request = SendMessageRequestDTO.builder()
                .conversationId(convId.toString())
                .messageContent("Ảnh đi biển")
                .imageUrl("https://res.cloudinary.com/demo/image/upload/beach.png")
                .attachmentPublicId("chatboxspringboot/beach_123")
                .build();

        MessageResponseDTO response = service.sendMessage(senderId, request);

        assertNotNull(response);
        assertEquals("Ảnh đi biển", response.getMessageContent());
        assertEquals("IMAGE", response.getType());
        assertEquals("https://res.cloudinary.com/demo/image/upload/beach.png", response.getImageUrl());
        assertEquals("chatboxspringboot/beach_123", response.getAttachmentPublicId());
    }

    @Test
    void rejectsMessageWhenBothContentAndImageAreMissing() {
        UUID senderId = UUID.randomUUID();
        SendMessageRequestDTO request = SendMessageRequestDTO.builder()
                .conversationId(UUID.randomUUID().toString())
                .messageContent("   ")
                .imageUrl(null)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.sendMessage(senderId, request));
        assertEquals("Tin nhắn phải có nội dung văn bản hoặc hình ảnh", ex.getMessage());
    }

    @Test
    void getMessagesMarksAsReadAndReturnsList() {
        UUID senderId = UUID.randomUUID();
        UUID partnerId = UUID.randomUUID();
        UUID convId = UUID.randomUUID();

        TaiKhoan sender = user(senderId, "Alice");
        TaiKhoan partner = user(partnerId, "Bob");
        Conversation conversation = new Conversation(sender, partner);
        conversation.setId(convId);

        Message msg1 = Message.builder()
                .id(UUID.randomUUID())
                .conversation(conversation)
                .sender(partner)
                .content("Chào bạn")
                .type("TEXT")
                .sentAt(Instant.now())
                .read(false)
                .build();

        Message msg2 = Message.builder()
                .id(UUID.randomUUID())
                .conversation(conversation)
                .sender(sender)
                .type("IMAGE")
                .imageUrl("https://res.cloudinary.com/demo/image/upload/img.png")
                .sentAt(Instant.now())
                .read(false)
                .build();

        when(conversationRepository.findById(convId)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderBySentAtAsc(convId)).thenReturn(List.of(msg1, msg2));

        List<MessageResponseDTO> result = service.getMessages(senderId, convId);

        assertEquals(2, result.size());
        assertEquals("Chào bạn", result.get(0).getMessageContent());
        assertEquals("https://res.cloudinary.com/demo/image/upload/img.png", result.get(1).getImageUrl());
        verify(messageRepository).markAllMessagesAsRead(convId, senderId);
    }

    private TaiKhoan user(UUID id, String name) {
        TaiKhoan u = new TaiKhoan();
        u.setId(id);
        u.setTenTaiKhoan(name);
        u.setEmail(name.toLowerCase() + "@example.com");
        u.setUserName(name.toLowerCase());
        return u;
    }
}
