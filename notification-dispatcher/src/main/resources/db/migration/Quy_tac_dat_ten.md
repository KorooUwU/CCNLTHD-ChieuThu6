Hai lưu ý quan trọng khi thêm script mới:
# Quy tắc đặt tên file: Bắt buộc phải là 2 dấu gạch dưới __:
Chuẩn: V7__ten_file.sql (chữ V viết hoa + số thứ tự + 2 dấu gạch dưới + tên mô tả).
# Không sửa các file cũ đã chạy (V1 - V6):
Các file đã chạy lên Supabase đều đã được lưu mã băm (checksum). Nếu bạn sửa nội dung các file cũ đó, khi khởi động Flyway sẽ báo lỗi Checksum mismatch và dừng ứng dụng ngay lập tức để bảo vệ tính nhất quán dữ liệu.
Nếu cần thêm cột, sửa bảng hay thêm dữ liệu, bạn chỉ cần tạo file mới kế tiếp (ví dụ V7__...sql).