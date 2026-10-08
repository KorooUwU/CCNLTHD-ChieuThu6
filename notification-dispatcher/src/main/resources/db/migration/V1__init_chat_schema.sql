-- ====================================================================
-- Migration: V1__init_chat_schema.sql
-- Mô tả: Khởi tạo schema cơ sở dữ liệu cho Chat & Notification (PostgreSQL / Supabase)
-- ====================================================================

-- Kích hoạt extension hỗ trợ sinh UUID v4 ngẫu nhiên
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Bảng Tài Khoản (tai_khoan)
CREATE TABLE IF NOT EXISTS tai_khoan (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ten_tai_khoan VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    user_name VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL DEFAULT '$2a$10$7EqJtq98hPqEX7fNZaFWoO.8/q9.PZg8k0iMsq5/oGqT21F7fXySu',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tai_khoan_email ON tai_khoan (email);
CREATE INDEX IF NOT EXISTS idx_tai_khoan_username ON tai_khoan (user_name);

-- 2. Bảng Cuộc Trò Chuyện (conversations) - Mô hình Chat 1-1
CREATE TABLE IF NOT EXISTS conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user1_id UUID NOT NULL REFERENCES tai_khoan(id) ON DELETE CASCADE,
    user2_id UUID NOT NULL REFERENCES tai_khoan(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_message_at TIMESTAMPTZ,
    CONSTRAINT chk_conversations_distinct_users CHECK (user1_id <> user2_id)
);

-- Ràng buộc duy nhất: Một cặp người dùng chỉ tồn tại tối đa 1 cuộc trò chuyện trực tiếp (bất kể thứ tự user1/user2)
CREATE UNIQUE INDEX IF NOT EXISTS uq_conversations_user_pair 
ON conversations (LEAST(user1_id, user2_id), GREATEST(user1_id, user2_id));

CREATE INDEX IF NOT EXISTS idx_conversations_user1 ON conversations (user1_id);
CREATE INDEX IF NOT EXISTS idx_conversations_user2 ON conversations (user2_id);
CREATE INDEX IF NOT EXISTS idx_conversations_last_message ON conversations (last_message_at DESC NULLS LAST);

-- 3. Bảng Tin Nhắn (messages)
CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES tai_khoan(id) ON DELETE RESTRICT,
    content TEXT NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_messages_conversation_sent ON messages (conversation_id, sent_at ASC);
CREATE INDEX IF NOT EXISTS idx_messages_sender ON messages (sender_id);

-- 4. Bảng Thông Báo (notifications)
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tai_khoan_id UUID NOT NULL REFERENCES tai_khoan(id) ON DELETE CASCADE,
    triggered_by_user_id UUID NOT NULL REFERENCES tai_khoan(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_read ON notifications (tai_khoan_id, is_read, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_triggered_by ON notifications (triggered_by_user_id);
