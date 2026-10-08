-- ====================================================================
-- Migration: V3__add_password_hash_to_tai_khoan.sql
-- Mô tả: Thêm cột password_hash vào bảng tai_khoan phục vụ Spring Security
-- ====================================================================

-- Mật khẩu mặc định là hash BCrypt của 'password123'
ALTER TABLE tai_khoan 
ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255) NOT NULL DEFAULT '$2a$10$7EqJtq98hPqEX7fNZaFWoO.8/q9.PZg8k0iMsq5/oGqT21F7fXySu';
