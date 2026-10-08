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
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tai_khoan_id", nullable = false)
    private TaiKhoan recipient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "triggered_by_user_id", nullable = false)
    private TaiKhoan triggeredBy;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Notification(TaiKhoan recipient, TaiKhoan triggeredBy, String type) {
        this.recipient = recipient;
        this.triggeredBy = triggeredBy;
        this.type = type;
        this.read = false;
    }
}
