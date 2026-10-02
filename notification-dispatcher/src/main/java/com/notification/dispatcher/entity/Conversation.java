package com.notification.dispatcher.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user1_id", nullable = false)
    private TaiKhoan user1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user2_id", nullable = false)
    private TaiKhoan user2;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Conversation() {
        // Required by JPA.
    }

    public Conversation(TaiKhoan user1, TaiKhoan user2) {
        this.user1 = user1;
        this.user2 = user2;
    }

    public UUID getId() {
        return id;
    }

    public TaiKhoan getUser1() {
        return user1;
    }

    public void setUser1(TaiKhoan user1) {
        this.user1 = user1;
    }

    public TaiKhoan getUser2() {
        return user2;
    }

    public void setUser2(TaiKhoan user2) {
        this.user2 = user2;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(Instant lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
