package com.notification.dispatcher.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private TaiKhoan sender;

    @Column(name = "content", columnDefinition = "text")
    private String content;

    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @Column(name = "attachment_public_id", length = 255)
    private String attachmentPublicId;

    @Builder.Default
    @Column(name = "type", nullable = false, length = 20)
    private String type = "TEXT";

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @CreationTimestamp
    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;

    public Message(Conversation conversation, TaiKhoan sender, String content) {
        this(conversation, sender, content, "TEXT", null, null);
    }

    public Message(Conversation conversation, TaiKhoan sender, String content, String type) {
        this(conversation, sender, content, type, null, null);
    }

    public Message(Conversation conversation, TaiKhoan sender, String content, String type, String imageUrl, String attachmentPublicId) {
        this.conversation = conversation;
        this.sender = sender;
        this.content = content;
        this.type = type != null ? type : (imageUrl != null ? "IMAGE" : "TEXT");
        this.imageUrl = imageUrl;
        this.attachmentPublicId = attachmentPublicId;
        this.read = false;
    }
}
