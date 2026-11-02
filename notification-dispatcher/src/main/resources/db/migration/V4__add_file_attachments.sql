-- ====================================================================
-- Migration: V4__add_file_attachments.sql
-- Mô tả: Thêm bảng lưu metadata tệp đính kèm tin nhắn
-- ====================================================================

CREATE TABLE IF NOT EXISTS message_attachments (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id    UUID REFERENCES messages(id) ON DELETE CASCADE,
    uploader_id   UUID NOT NULL REFERENCES tai_khoan(id) ON DELETE RESTRICT,
    original_name VARCHAR(255) NOT NULL,
    stored_name   VARCHAR(255) NOT NULL UNIQUE,
    mime_type     VARCHAR(127) NOT NULL,
    file_size     BIGINT NOT NULL CHECK (file_size > 0 AND file_size <= 10485760),
    storage_path  TEXT NOT NULL,
    uploaded_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_attachments_message  ON message_attachments (message_id);
CREATE INDEX IF NOT EXISTS idx_attachments_uploader ON message_attachments (uploader_id);
