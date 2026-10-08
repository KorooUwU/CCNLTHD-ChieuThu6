-- ====================================================================
-- Migration: V2__seed_test_data.sql
-- Mô tả: Dữ liệu mẫu khởi tạo (Seed data) cho môi trường kiểm thử / phát triển
-- ====================================================================

-- 1. Thêm 2 tài khoản mẫu
INSERT INTO tai_khoan (id, ten_tai_khoan, email, user_name, created_at)
VALUES 
    ('11111111-1111-1111-1111-111111111111', 'Nguyễn Văn A', 'user_a@example.com', 'user_a', CURRENT_TIMESTAMP),
    ('22222222-2222-2222-2222-222222222222', 'Trần Thị B', 'user_b@example.com', 'user_b', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 2. Thêm 1 cuộc hội thoại giữa User A và User B
INSERT INTO conversations (id, user1_id, user2_id, created_at, last_message_at)
VALUES 
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 3. Thêm 2 tin nhắn mẫu
INSERT INTO messages (id, conversation_id, sender_id, content, sent_at, is_read)
VALUES 
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'Xin chào bạn B, mình là A!', CURRENT_TIMESTAMP - INTERVAL '5 minutes', true),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '22222222-2222-2222-2222-222222222222', 'Chào bạn A, rất vui được làm quen!', CURRENT_TIMESTAMP, false)
ON CONFLICT (id) DO NOTHING;

-- 4. Thêm 1 thông báo mẫu cho User A
INSERT INTO notifications (id, tai_khoan_id, triggered_by_user_id, type, is_read, created_at)
VALUES 
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 'NEW_MESSAGE', false, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
