-- Thêm cột type vào bảng messages để hỗ trợ tin nhắn văn bản, hình ảnh, file đính kèm
ALTER TABLE messages ADD COLUMN IF NOT EXISTS type VARCHAR(20) NOT NULL DEFAULT 'TEXT';
