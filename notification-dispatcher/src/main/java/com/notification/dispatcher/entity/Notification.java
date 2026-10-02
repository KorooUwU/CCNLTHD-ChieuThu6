package com.notification.dispatcher.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "notifications")
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

    @Column(name = "is_read", nullable = false)
    private boolean read;

    protected Notification() {
        // Required by JPA.
    }

    public Notification(TaiKhoan recipient, TaiKhoan triggeredBy, String type) {
        this.recipient = recipient;
        this.triggeredBy = triggeredBy;
        this.type = type;
    }

    public UUID getId() {
        return id;
    }

    public TaiKhoan getRecipient() {
        return recipient;
    }

    public void setRecipient(TaiKhoan recipient) {
        this.recipient = recipient;
    }

    public TaiKhoan getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(TaiKhoan triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
