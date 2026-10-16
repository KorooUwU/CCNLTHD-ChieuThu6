package com.notification.dispatcher.service;

import com.notification.dispatcher.DTO.ConversationCreatedResponseDTO;
import com.notification.dispatcher.DTO.ConversationSummaryDTO;
import com.notification.dispatcher.entity.Conversation;
import com.notification.dispatcher.entity.Message;
import com.notification.dispatcher.entity.TaiKhoan;
import com.notification.dispatcher.exception.UserNotFoundException;
import com.notification.dispatcher.repository.ConversationRepository;
import com.notification.dispatcher.repository.MessageRepository;
import com.notification.dispatcher.repository.TaiKhoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final TaiKhoanRepository taiKhoanRepository;

    public ConversationService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            TaiKhoanRepository taiKhoanRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.taiKhoanRepository = taiKhoanRepository;
    }

    @Transactional
    public ConversationCreatedResponseDTO createOrGetConversation(
            UUID currentUserId,
            UUID partnerId) {

        if (currentUserId.equals(partnerId)) {
            throw new IllegalArgumentException(
                    "Không thể tạo cuộc trò chuyện với chính mình");
        }

        TaiKhoan currentUser = taiKhoanRepository.findById(currentUserId)
                .orElseThrow(() ->
                        new UserNotFoundException("Current user not found"));

        TaiKhoan partner = taiKhoanRepository.findById(partnerId)
                .orElseThrow(() ->
                        new UserNotFoundException("Partner not found"));

        Conversation conversation = conversationRepository
                .findBetweenUsers(currentUserId, partnerId)
                .orElseGet(() -> conversationRepository.save(
                        new Conversation(currentUser, partner)
                ));

        return ConversationCreatedResponseDTO.builder()
                .conversationId(conversation.getId().toString())
                .partnerId(partner.getId().toString())
                .partnerName(partner.getTenTaiKhoan())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ConversationSummaryDTO> getConversations(UUID currentUserId) {
        return conversationRepository.findAllByUserId(currentUserId)
                .stream()
                .map(conversation -> toSummary(conversation, currentUserId))
                .toList();
    }

    private ConversationSummaryDTO toSummary(
            Conversation conversation,
            UUID currentUserId) {
        TaiKhoan partner = conversation.getUser1().getId().equals(currentUserId)
                ? conversation.getUser2()
                : conversation.getUser1();

        Message lastMessage = messageRepository
                .findFirstByConversationIdOrderBySentAtDesc(conversation.getId())
                .orElse(null);

        long unreadCount = messageRepository.countUnreadMessages(
                conversation.getId(),
                currentUserId);

        return ConversationSummaryDTO.builder()
                .conversationId(conversation.getId().toString())
                .partnerId(partner.getId().toString())
                .partnerName(partner.getTenTaiKhoan())
                .partnerAvatarUrl(null)
                .lastMessageContent(lastMessage == null ? null : lastMessage.getContent())
                .lastMessageAt(lastMessage == null || lastMessage.getSentAt() == null
                        ? null
                        : lastMessage.getSentAt().atZone(ZoneOffset.UTC).toLocalDateTime())
                .unreadCount(Math.toIntExact(unreadCount))
                .build();
    }
}