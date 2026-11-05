# Kiểm thử tích hợp Chương 9 bằng Postman

Import `postman/chapter9-integration.postman_collection.json`. Collection có 6 thư mục, 30 request. Chạy toàn bộ theo thứ tự, một iteration; không chạy riêng request trước khi các bước tạo tài khoản/login/hội thoại hoàn thành.

## Chuẩn bị

1. Chạy backend và database dev; cổng mặc định 8081. Có thể dùng cổng khác nếu cập nhật baseUrl tương ứng.
2. Collection Variables: baseUrl=`http://localhost:8081`, runGroups=`true`, runCloudinary=`false`. Nếu environment hiện tại có biến trùng tên, đồng bộ hoặc bỏ biến đó vì environment có thể ghi đè collection variable.
3. Không cần nhập JWT hoặc ID thủ công: collection tạo A/B/C, đăng nhập rồi lưu biến cho các bước sau. C là người ngoài hội thoại A–B, dùng để chứng minh quyền truy cập.
4. Chọn Run collection, tất cả folder, iteration 1. Không export collection sau khi chạy nếu còn token/password trong biến.

## Phạm vi và kết quả mong đợi

| Folder | Kiểm tra |
|---|---|
| 01 Authentication | Register/login A/B/C, mật khẩu sai 401; response không có passwordHash |
| 02 User Management | GET/PATCH hồ sơ A, B giữ nguyên dù query gửi id B, tìm kiếm phân trang không lộ email/hash, thiếu JWT 401 |
| 03 Conversations | Tạo A–B, gọi lặp trả cùng ID, cả A/B có hội thoại, tùy chọn tạo nhóm |
| 04 Messages | A gửi text, B đọc thấy đúng tin; tin rỗng 400; C bị từ chối đọc với 403 |
| 05 Cloudinary | Tùy chọn upload thật, gửi IMAGE và B đọc lại đúng URL/messageId |
| 06 Negative & Security | Email trùng 409, input sai 400, JWT sai 401; C bị từ chối gửi với 403, không có tin của C được lưu |

Tạo lại hội thoại hiện trả HTTP 201 theo controller; assertion quan trọng là cùng conversationId, không tạo bản ghi mới. Kết quả này chưa chứng minh idempotency khi hai request thực sự đồng thời.

**Backend update:** ConversationAccessDeniedException now maps membership failures to HTTP 403. Bad input remains HTTP 400. See chapter9-fixes.md for migration and regression results.

Mặc định bỏ qua 1 request nhóm và 3 request Cloudinary, còn 26 request. Skip không phải pass. Nhóm chỉ là tính năng bổ sung trong code; nếu báo cáo chốt chat 1–1, ghi rõ không thuộc phạm vi nghiệm thu chính.
