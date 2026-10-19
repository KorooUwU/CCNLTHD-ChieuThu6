package com.notification.dispatcher.service;

import com.notification.dispatcher.DTO.ConversationCreatedResponseDTO;
import com.notification.dispatcher.DTO.ConversationSummaryDTO;
import com.notification.dispatcher.DTO.CreateGroupConversationRequestDTO;
import com.notification.dispatcher.DTO.GroupConversationResponseDTO;
import com.notification.dispatcher.DTO.UserSummaryDTO;
import com.notification.dispatcher.entity.Conversation;
import com.notification.dispatcher.entity.ConversationType;
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
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @Transactional
    public GroupConversationResponseDTO createGroupConversation(
            UUID currentUserId,
            CreateGroupConversationRequestDTO request) {
        getUser(currentUserId);
        Set<UUID> memberIds = request.getMemberIds().stream()
                .map(UUID::fromString)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        memberIds.add(currentUserId);

        Set<TaiKhoan> members = memberIds.stream()
                .map(this::getUser)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Conversation conversation = conversationRepository.save(
                Conversation.group(request.getGroupName().trim(), members));
        return toGroupResponse(conversation);
    }

    @Transactional
    public GroupConversationResponseDTO addGroupMember(
            UUID currentUserId,
            UUID conversationId,
            UUID memberId) {
        Conversation conversation = getGroup(conversationId);
        requireMember(conversation, currentUserId);
        TaiKhoan member = getUser(memberId);
        conversation.getMembers().add(member);
        return toGroupResponse(conversationRepository.save(conversation));
    }

    @Transactional
    public void removeGroupMember(
            UUID currentUserId,
            UUID conversationId,
            UUID memberId) {
        Conversation conversation = getGroup(conversationId);
        requireMember(conversation, currentUserId);
        if (!conversation.getMembers().contains(getUser(memberId))) {
            throw new IllegalArgumentException("Người dùng không phải thành viên nhóm");
        }
        if (conversation.getMembers().size() == 1) {
            throw new IllegalArgumentException("Nhóm phải còn ít nhất một thành viên");
        }
        conversation.getMembers().removeIf(member -> member.getId().equals(memberId));
        conversationRepository.save(conversation);
    }

    private TaiKhoan getUser(UUID userId) {
        return taiKhoanRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private Conversation getGroup(UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        if (conversation.getType() != ConversationType.GROUP) {
            throw new IllegalArgumentException("Conversation is not a group");
        }
        return conversation;
    }

    private void requireMember(Conversation conversation, UUID userId) {
        if (conversation.getMembers().stream().noneMatch(member -> member.getId().equals(userId))) {
            throw new IllegalArgumentException("Người dùng không phải thành viên nhóm");
        }
    }

    private GroupConversationResponseDTO toGroupResponse(Conversation conversation) {
        List<UserSummaryDTO> members = conversation.getMembers().stream()
                .map(member -> UserSummaryDTO.builder()
                        .id(member.getId().toString())
                        .tenTaiKhoan(member.getTenTaiKhoan())
                        .userName(member.getUserName())
                        .build())
                .toList();
        return GroupConversationResponseDTO.builder()
                .conversationId(conversation.getId().toString())
                .groupName(conversation.getGroupName())
                .members(members)
                .build();
    }

    private ConversationSummaryDTO toSummary(
            Conversation conversation,
            UUID currentUserId) {
        boolean group = conversation.getType() == ConversationType.GROUP;
        TaiKhoan partner = group ? null
                : conversation.getUser1().getId().equals(currentUserId)
                ? conversation.getUser2() : conversation.getUser1();

        Message lastMessage = messageRepository
                .findFirstByConversationIdOrderBySentAtDesc(conversation.getId())
                .orElse(null);

        long unreadCount = messageRepository.countUnreadMessages(
                conversation.getId(),
                currentUserId);

        return ConversationSummaryDTO.builder()
                .conversationId(conversation.getId().toString())
                .conversationType(conversation.getType().name())
                .partnerId(group ? null : partner.getId().toString())
                .partnerName(group ? conversation.getGroupName() : partner.getTenTaiKhoan())
                .partnerAvatarUrl(null)
                .memberCount(group ? conversation.getMembers().size() : 2)
                .lastMessageContent(lastMessage == null ? null : lastMessage.getContent())
                .lastMessageAt(lastMessage == null || lastMessage.getSentAt() == null
                        ? null
                        : lastMessage.getSentAt().atZone(ZoneOffset.UTC).toLocalDateTime())
                .unreadCount(Math.toIntExact(unreadCount))
                .build();
    }
}