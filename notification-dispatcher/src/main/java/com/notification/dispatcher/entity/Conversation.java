package com.notification.dispatcher.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user1_id")
    private TaiKhoan user1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user2_id")
    private TaiKhoan user2;

    @Enumerated(EnumType.STRING)
    @Column(name = "conversation_type", nullable = false, length = 20)
    @Builder.Default
    private ConversationType type = ConversationType.DIRECT;

    @Column(name = "group_name", length = 100)
    private String groupName;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "conversation_members",
            joinColumns = @JoinColumn(name = "conversation_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    @Builder.Default
    private Set<TaiKhoan> members = new LinkedHashSet<>();

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Conversation(TaiKhoan user1, TaiKhoan user2) {
        this.user1 = user1;
        this.user2 = user2;
        this.type = ConversationType.DIRECT;
        this.members = new LinkedHashSet<>();
        this.members.add(user1);
        this.members.add(user2);
    }

    public static Conversation group(String groupName, Set<TaiKhoan> members) {
        Conversation conversation = new Conversation();
        conversation.type = ConversationType.GROUP;
        conversation.groupName = groupName;
        conversation.members = new LinkedHashSet<>(members);
        return conversation;
    }
}
