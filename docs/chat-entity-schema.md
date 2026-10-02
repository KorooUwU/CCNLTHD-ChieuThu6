# Ánh xạ Entity cho PostgreSQL

TV2 bàn giao bản ánh xạ này cho TV1 viết migration. Entity ở package `com.notification.dispatcher.entity`. Đây là thiết kế đề xuất cần nhóm thống nhất, chưa phải migration đã chạy.

## Quy ước chung

- Khóa chính của cả bốn bảng là `id UUID PRIMARY KEY`. JPA sinh UUID bằng `GenerationType.UUID` trước khi INSERT. SQL seed phải cung cấp UUID hoặc tự sinh UUID trong SQL; Entity không yêu cầu database có default cho ID.
- Thời gian Java `Instant` ánh xạ PostgreSQL `TIMESTAMPTZ`, biểu diễn một thời điểm tuyệt đối. Dữ liệu API nên trả UTC ISO 8601. Múi giờ hiển thị do giao diện xử lý.
- `@CreationTimestamp` do Hibernate điền lúc tạo bản ghi. Khi seed bằng SQL, TV1 phải cung cấp thời gian hoặc đặt DEFAULT CURRENT_TIMESTAMP.
- Quan hệ chỉ khai báo từ bảng con bằng `@ManyToOne(fetch = LAZY)`, không cascade xóa và không có collection hai chiều. Không trả Entity trực tiếp trong API; TV3 ánh xạ DTO.
- Các cột bên dưới có tên khác nhãn trong hình ở một số vị trí, như tất cả PK đều là `id`. TV1 dùng tên bảng/cột dưới đây để khớp annotation.

## tai_khoan — TaiKhoan

| Cột | PostgreSQL | Ràng buộc | Java |
|---|---|---|---|
| id | UUID | PK | UUID |
| ten_tai_khoan | VARCHAR(100) | NOT NULL | String tenTaiKhoan |
| email | VARCHAR(254) | NOT NULL, UNIQUE | String email |
| user_name | VARCHAR(50) | NOT NULL, UNIQUE | String userName |
| created_at | TIMESTAMPTZ | NOT NULL | Instant createdAt |

`ten_tai_khoan` là tên hiển thị; `user_name` là tên đăng nhập. UNIQUE hiện phân biệt hoa/thường; nhóm cần thống nhất chuẩn hóa email/username ở Service hoặc thiết kế unique index phù hợp. Password hash chưa có trong ERD và chưa được thêm; phải bổ sung trước khi làm đăng nhập bằng mật khẩu.

## conversations — Conversation

| Cột | PostgreSQL | Ràng buộc | Java |
|---|---|---|---|
| id | UUID | PK | UUID |
| user1_id | UUID | NOT NULL, FK tai_khoan(id) | TaiKhoan user1 |
| user2_id | UUID | NOT NULL, FK tai_khoan(id) | TaiKhoan user2 |
| created_at | TIMESTAMPTZ | NOT NULL | Instant createdAt |
| last_message_at | TIMESTAMPTZ | NULL khi chưa có tin nhắn | Instant lastMessageAt |

TV1 nên thêm CHECK(user1_id <> user2_id) và unique index cho cặp không có thứ tự, ví dụ trên LEAST(user1_id, user2_id), GREATEST(user1_id, user2_id). UNIQUE(user1_id, user2_id) riêng lẻ không ngăn cặp đảo ngược. Các ràng buộc này chưa được thực thi bằng annotation; cần migration và Service hỗ trợ.

## messages — Message

| Cột | PostgreSQL | Ràng buộc | Java |
|---|---|---|---|
| id | UUID | PK | UUID |
| conversation_id | UUID | NOT NULL, FK conversations(id) | Conversation conversation |
| sender_id | UUID | NOT NULL, FK tai_khoan(id) | TaiKhoan sender |
| content | TEXT | NOT NULL | String content |
| sent_at | TIMESTAMPTZ | NOT NULL | Instant sentAt |
| is_read | BOOLEAN | NOT NULL, đề xuất DEFAULT FALSE | boolean read |

TV1 nên thêm index (conversation_id, sent_at, id) phục vụ truy vấn lịch sử có thứ tự ổn định. Service phải kiểm tra sender là user1 hoặc user2; FK sender_id không tự bảo đảm điều này. Content cần trim/validation và giới hạn độ dài theo contract nhóm chốt. is_read mặc định false trong đối tượng Java; DEFAULT FALSE cho SQL cần TV1 đặt.

## notifications — Notification

| Cột | PostgreSQL | Ràng buộc | Java |
|---|---|---|---|
| id | UUID | PK | UUID |
| tai_khoan_id | UUID | NOT NULL, FK tai_khoan(id) | TaiKhoan recipient |
| triggered_by_user_id | UUID | NOT NULL, FK tai_khoan(id) | TaiKhoan triggeredBy |
| type | VARCHAR(50) | NOT NULL | String type |
| is_read | BOOLEAN | NOT NULL, đề xuất DEFAULT FALSE | boolean read |

`tai_khoan_id` là người nhận, `triggered_by_user_id` là người tạo hành động. TV1 nên thêm index (tai_khoan_id, is_read). Type giữ String theo ERD; nhóm cần chốt giá trị hợp lệ, ví dụ NEW_MESSAGE.

## Những điểm cần nhóm chốt tiếp

- Thêm password_hash vào tai_khoan nếu tự quản lý mật khẩu; không lưu mật khẩu rõ.
- Thêm message_id FK messages(id) và created_at vào notifications để mở đúng tin nhắn và sắp xếp thông báo. Bản Entity hiện giữ nguyên phạm vi trường trong ERD, chưa thêm hai cột này.
- Có cần read_at hoặc client_message_id chống gửi lặp hay không.
- Quy tắc xóa: đề xuất không cascade delete để giữ lịch sử.
- Chỉ có hai tài khoản seed hay hệ thống cho phép nhiều tài khoản nhưng mỗi cuộc trò chuyện có hai người.

## Giới hạn của phần Entity

Entity mô tả mapping, không tự triển khai API, xác thực, realtime hoặc quy tắc quyền sở hữu. Cấu hình hiện dùng ddl-auto=none nên không tự tạo bảng. TV1 viết migration/schema và seed; Service thực hiện kiểm tra nghiệp vụ và transaction. Không chuyển sang ddl-auto=update chỉ để thay thế migration.
