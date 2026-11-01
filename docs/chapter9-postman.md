# Kiểm thử tích hợp Chương 9 bằng Postman

Import `postman/chapter9-integration.postman_collection.json`. Collection có 6 thư mục, 30 request. Chạy toàn bộ theo thứ tự, một iteration; không chạy riêng request trước khi các bước tạo tài khoản/login/hội thoại hoàn thành.

## Chuẩn bị

1. Chạy backend và database dev; cổng mặc định 8081. Có thể dùng cổng khác nếu cập nhật baseUrl tương ứng.
2. Collection Variables: baseUrl=`http://localhost:8081`, runGroups=`false`, runCloudinary=`false`. Nếu environment hiện tại có biến trùng tên, đồng bộ hoặc bỏ biến đó vì environment có thể ghi đè collection variable.
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

**Điểm chưa đạt đã thấy trong code:** MessageService ném IllegalArgumentException khi người ngoài hội thoại truy cập; handler hiện ánh xạ thành 400. Hai ca truy cập trái phép trong collection yêu cầu 403 và sẽ fail nếu backend giữ hành vi đó. Không đổi assertion về 400 để báo tất cả xanh; cần xử lý lỗi phân quyền riêng ở backend rồi chạy lại. Collection này chưa được chạy thực tế nên chưa có số liệu pass/fail được xác nhận.

Mặc định bỏ qua 1 request nhóm và 3 request Cloudinary, còn 26 request. Skip không phải pass. Nhóm chỉ là tính năng bổ sung trong code; nếu báo cáo chốt chat 1–1, ghi rõ không thuộc phạm vi nghiệm thu chính.

## Bật kiểm thử Cloudinary

- Cấu hình credential Cloudinary hợp lệ trong môi trường backend. Dùng tài khoản dịch vụ test được nhóm cho phép.
- Đặt runCloudinary=`true`.
- Mở Upload Image → Body → form-data → trường file kiểu File, chọn ảnh mẫu nhỏ trên máy. Nếu chạy Runner, cấp quyền truy cập file/working directory theo Postman. Không đặt Content-Type thủ công vì multipart cần boundary.
- Với Newman, đặt imagePath thành đường dẫn ảnh cục bộ truy cập được. Nếu runner không thay biến trong đường dẫn file, chọn file trực tiếp trước khi export bản dùng trên máy đó.
- Chạy lại toàn bộ collection; upload trả 201, lưu url/publicId để gửi tin IMAGE. Request sau yêu cầu upload thành công; không thay bằng URL giả rồi ghi nhận là upload thật.
- Test lưu tài khoản, hội thoại, tin nhắn và file Cloudinary thật, không tự cleanup. Dọn dữ liệu/file test theo quy trình của nhóm; không chạy collection trên production.

## Minh chứng báo cáo

Chụp tổng kết Runner, từng response 201/200 quan trọng và 400/401/403 ở negative tests. Ghi ngày chạy, commit, profile, baseUrl, số assertion pass/fail và số skip. Che JWT/password, không chụp `.env`. Nếu test fail thì ghi nhận đúng lỗi cùng kết quả chạy lại sau sửa.

JWT hết hạn chưa có trong collection này. Dùng ca expiry trong `user-handoff.postman_collection.json` với ứng dụng test TTL 30 giây, checkExpiry=true; xem `tv2-handoff.md`. Collection Chương 9 cần token sống đủ lâu để chạy trọn luồng, không dùng TTL 30 giây cho luồng upload dài.

Đây là kiểm thử REST; không chứng minh SSE, RabbitMQ, retry/DLQ hoặc realtime trình duyệt.
