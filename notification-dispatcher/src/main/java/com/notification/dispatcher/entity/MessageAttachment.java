package com.notification.dispatcher.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Metadata tệp đính kèm được upload lên server.
 * File thực tế được lưu trên local filesystem tại storagePath.
 */
@Entity
@Table(name = "message_attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Tin nhắn chứa attachment (nullable: pre-upload chưa gán message) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_id", nullable = false)
    private TaiKhoan uploader;

    /** Tên file gốc do người dùng đặt */
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    /** Tên file lưu trên disk (UUID-based, unique) */
    @Column(name = "stored_name", nullable = false, length = 255, unique = true)
    private String storedName;

    /** MIME type đã được validate */
    @Column(name = "mime_type", nullable = false, length = 127)
    private String mimeType;

    /** Kích thước tính bằng byte (≤ 10 MB) */
    @Column(name = "file_size", nullable = false)
    private long fileSize;

    /** Đường dẫn tuyệt đối trên server */
    @Column(name = "storage_path", nullable = false, columnDefinition = "text")
    private String storagePath;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;
}
