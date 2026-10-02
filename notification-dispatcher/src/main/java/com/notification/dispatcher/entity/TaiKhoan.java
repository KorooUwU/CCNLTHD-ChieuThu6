package com.notification.dispatcher.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tai_khoan")
public class TaiKhoan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "ten_tai_khoan", nullable = false, length = 100)
    private String tenTaiKhoan;

    @Column(name = "email", nullable = false, length = 254, unique = true)
    private String email;

    @Column(name = "user_name", nullable = false, length = 50, unique = true)
    private String userName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TaiKhoan() {
        // Required by JPA.
    }

    public TaiKhoan(String tenTaiKhoan, String email, String userName) {
        this.tenTaiKhoan = tenTaiKhoan;
        this.email = email;
        this.userName = userName;
    }

    public UUID getId() {
        return id;
    }

    public String getTenTaiKhoan() {
        return tenTaiKhoan;
    }

    public void setTenTaiKhoan(String tenTaiKhoan) {
        this.tenTaiKhoan = tenTaiKhoan;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
