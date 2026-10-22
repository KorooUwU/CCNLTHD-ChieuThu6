-- V6: Hỗ trợ lưu trực tiếp link ảnh và tệp đính kèm Cloudinary vào bảng messages
ALTER TABLE messages
    ALTER COLUMN content DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS image_url TEXT,
    ADD COLUMN IF NOT EXISTS attachment_public_id VARCHAR(255);

-- Đảm bảo tin nhắn phải có ít nhất nội dung văn bản hoặc link hình ảnh/tệp đính kèm
ALTER TABLE messages
    ADD CONSTRAINT chk_messages_content_or_image
    CHECK (content IS NOT NULL OR image_url IS NOT NULL);
