package com.notification.dispatcher.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.notification.dispatcher.entity.Conversation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

       @Query("SELECT c FROM Conversation c WHERE " +
                     "(c.user1.id = :userAId AND c.user2.id = :userBId) OR " +
                     "(c.user1.id = :userBId AND c.user2.id = :userAId)")
       Optional<Conversation> findBetweenUsers(@Param("userAId") UUID userAId, @Param("userBId") UUID userBId);

       @Query("SELECT c FROM Conversation c WHERE " +
                     "c.user1.id = :userId OR c.user2.id = :userId " +
                     "ORDER BY coalesce(c.lastMessageAt, c.createdAt) DESC")
       List<Conversation> findAllByUserId(@Param("userId") UUID userId);
}
