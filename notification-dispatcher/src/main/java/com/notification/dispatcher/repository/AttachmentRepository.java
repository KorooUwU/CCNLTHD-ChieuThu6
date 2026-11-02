package com.notification.dispatcher.repository;

import com.notification.dispatcher.entity.MessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AttachmentRepository extends JpaRepository<MessageAttachment, UUID> {

    /** Lấy tất cả attachment của một tin nhắn */
    List<MessageAttachment> findByMessageId(UUID messageId);

    /** Kiểm tra file có thuộc về uploader không (authorization) */
    boolean existsByIdAndUploaderId(UUID id, UUID uploaderId);
}
