# Sửa lỗi kiểm thử Chương 9

## Các thay đổi

- ConversationAccessDeniedException được handler ánh xạ thành JSON 403 cho nhánh không phải thành viên. Input không hợp lệ vẫn trả 400. Áp dụng cho đọc/gửi tin nhóm và trực tiếp, kiểm tra thành viên nhóm, typing và read receipt.
- Notification mới tham chiếu Message tạo ra nó qua message_id, thay vì tra tin mới nhất của hội thoại. Cả MessageService và ChatService lưu liên kết này. Dữ liệu cũ không thể xác định tin gốc sẽ trả messageContent/conversationId rỗng; không gán tin mới nhất để đoán nội dung.
- TypingEventDTO hỗ trợ tên chuẩn isTyping và alias typing. Test xác nhận cả hai nhận true; sender thật lấy từ JWT, không từ senderId giả trong body. Người ngoài không được typing hoặc đánh dấu đọc.
- Test unit MessageService được bổ sung mock save Notification phù hợp luồng realtime hiện tại.

## Migration và khởi động

V7__link_notification_to_message.sql thêm FK message_id nullable, index, và ON DELETE SET NULL. Chưa backfill thông báo cũ vì không đủ thông tin xác định message gốc. Thống nhất số V7 với nhóm trước khi áp dụng trên database dùng chung; không sửa nội dung migration đã chạy. Lần xác minh này chỉ áp dụng migration vào PostgreSQL local, không chạy lên Supabase.

Dừng backend cũ bằng Ctrl+C, rồi khởi động bản code mới với profile/database phù hợp. Không giữ ứng dụng cũ để chạy collection sau khi source đã sửa.

## Kết quả thực tế ngày 02/11/2026

| Kiểm tra | Kết quả |
|---|---|
| Maven clean verify, profile dev, PostgreSQL local | 67 test; Failures=0, Errors=0, Skipped=0; BUILD SUCCESS |
| UserApiRegressionTest | 7 test HTTP/DB, transaction rollback; producer mock để cô lập |
| Newman Chương 9, app cổng 18085, PostgreSQL và RabbitMQ local | 62 request, 100 assertion, 0 lỗi |
| Năm ca từ chối đọc/gửi/đã bị xóa khỏi nhóm | Trả 403 |
| GET notifications B | Có đúng ba thông báo từ lượt chạy, nội dung gắn đúng tin nguồn |
| Trigger TYPING | Trả 204 với body hiện có dùng typing; cả hai cách đặt tên đều có test tự động |

Group Chat bật. Bỏ qua 3 Cloudinary và 2 stream SSE dài; không coi 5 ca skip là pass. Có publish RabbitMQ thật trong luồng Newman, nhưng lần chạy này chưa quan sát EventStream người nhận, Presence transition hoặc chứng minh delivery/ack end-to-end. Không khẳng định đây là nguyên nhân duy nhất của lỗi typing ở lần chạy trước vì không có response lỗi gốc.

App kiểm thử riêng được dừng sau khi chạy. Tài khoản/hội thoại/tin nhắn của Newman còn trong database dev. Không export collection đã chứa token/password thật. Hướng dẫn chạy chung: [chapter9-postman.md](chapter9-postman.md).

## Presence và minh chứng Hình 9.26

Giữ đúng idB/tokenB của cùng lượt chạy; dùng token A gọi GET /api/presence/{{idB}}.

| Trước | Thao tác | Sau cần quan sát |
|---|---|---|
| OFFLINE | B mở SSE và nhận CONNECTED | ONLINE |
| ONLINE | Đóng toàn bộ stream B, kể cả frontend/tab khác | OFFLINE sau khi backend phát hiện kết nối cuối đã đóng |

Chụp response với cùng userId, thời điểm, log cleanup nếu có. Poll có giới hạn, ví dụ mỗi 5 giây trong 60 giây; nếu chưa offline thì ghi nhận chưa đạt trong cửa sổ quan sát, không khẳng định luôn phát hiện trong 60 giây. SseService chỉ đánh dấu offline khi emitter cuối được dọn. Presence lưu trong bộ nhớ instance, chưa chứng minh đồng bộ nhiều instance. Giữ manualSse=false khi chạy Runner, chỉ bật khi Send từng stream thủ công.
