package com.notification.dispatcher.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.notification.dispatcher.entity.Message;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByConversationIdOrderBySentAtAsc(UUID conversationId);

    Page<Message> findByConversationIdOrderBySentAtDesc(UUID conversationId, Pageable pageable);

    Optional<Message> findFirstByConversationIdOrderBySentAtDesc(UUID conversationId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :conversationId AND m.sender.id <> :userId AND m.read = false")
    long countUnreadMessages(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE Message m SET m.read = true WHERE m.conversation.id = :conversationId AND m.sender.id <> :userId AND m.read = false")
    int markAllMessagesAsRead(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId);
}
