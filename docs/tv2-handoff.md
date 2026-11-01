# Bàn giao Module 2 Authentication và User API

## Hợp đồng API

Base URL tùy môi trường, local ví dụ `http://localhost:18081`. JSON UTF-8. UUID truyền dạng chuỗi. Không commit token, mật khẩu thật hoặc `.env`.

| Method | Path | Request | Response thành công |
|---|---|---|---|
| POST | /api/auth/register | tenTaiKhoan, email, userName, password | 201: id, tenTaiKhoan, email, userName, createdAt |
| POST | /api/auth/login | userName, password | 200: accessToken, tokenType=Bearer, expiresIn (giây) |
| GET | /api/users/me | Bearer JWT | 200: hồ sơ chủ token |
| PATCH | /api/users/me | Bearer JWT; tenTaiKhoan | 200: hồ sơ sau cập nhật |
| GET | /api/users/search | Bearer JWT; keyword, page=0, size=20 | 200: content, page, size, totalElements, totalPages |

Search trả mỗi user gồm id, tenTaiKhoan, userName; không trả email hoặc passwordHash. Keyword không trắng, tối đa 100 ký tự; page 0..10000; size 1..50. Tên hiển thị cập nhật không trắng, tối đa 100 ký tự. Email, username, password không cập nhật qua PATCH này. Xem `user-api.md` và `authentication.md` để biết chi tiết validation.

Các API ngoài register/login yêu cầu `Authorization: Bearer <accessToken>`. Danh tính do server đọc từ JWT `sub`, không lấy từ userId do client gửi. Token HS256, issuer chat-system, TTL mặc định 900 giây. Chưa có refresh/revoke; token hết hạn cần đăng nhập lại. Không gửi JWT qua URL.

Lỗi thống nhất gồm errorCode, errorMessage, details, timestamp: 400 input/validation, 401 thiếu/sai/hết hạn token, 409 đăng ký trùng. Security có nhánh xử lý 403 nhưng từng module vẫn phải tự kiểm tra quyền sở hữu tài nguyên; chỉ yêu cầu đăng nhập chưa đủ chứng minh quyền này.

## Phối hợp thành viên

- TV1: giữ schema UUID và unique username/email. Thứ tự migration hiện tại V1..V5; V4 thêm messages.type, V5 thêm nhóm. Không sửa file đã áp dụng; migration mới dùng version chưa tồn tại. Cung cấp PostgreSQL và biến môi trường theo `.env.example`.
- TV3: dùng user ID từ JWT đã xác thực, kiểm tra thành viên hội thoại trong service trước khi đọc/gửi/sửa dữ liệu. Luồng chat 1–1 hiện có POST /api/conversations/create với partnerId và GET /api/conversations/get. Không suy ra quyền từ userId tùy ý trong request.
- TV4: tích hợp JWT cho SSE, gắn kết nối với principal đã xác thực; không cho user chọn danh tính người khác. Thống nhất cách frontend truyền Bearer header (ví dụ fetch streaming); native EventSource không có tùy chọn header Authorization. Cần kiểm tra kết nối lại, token hết hạn và chống đọc sự kiện người khác trước khi ghi nhận hoàn tất.
- Frontend: giữ token ngoài log/ảnh chụp, xử lý 401 yêu cầu đăng nhập lại. CORS/proxy phải chốt theo origin thực tế. Test Postman không chứng minh CORS trình duyệt.

## Bộ kiểm thử bàn giao

Collection `postman/user-handoff.postman_collection.json` tạo hai tài khoản dev mới mỗi lần chạy, kiểm tra cập nhật A không sửa B, tìm kiếm/phân trang, input sai, JWT và hội thoại A–B. Các tài khoản/hội thoại này được lưu trong database dev; không chạy lên production. Không export collection đã chứa giá trị token/password sau khi chạy.

Import collection vào Postman, đặt baseUrl rồi Run theo thứ tự. Mặc định bỏ qua ca chờ token hết hạn. Chỉ đặt biến checkExpiry=true khi ứng dụng test riêng được chạy với JWT_TTL_SECONDS=30; ca cuối chờ 31 giây. Không đổi TTL của ứng dụng dùng chung chỉ để chạy ca này.

Unit/integration: từ notification-dispatcher chạy `./mvnw.cmd clean verify`; PostgreSQL local phải hoạt động. Kết quả thực tế từng lần chạy được ghi riêng, không coi collection là đã pass chỉ vì đã tạo file.

## Phần ngoài phạm vi xác nhận

Chưa xác nhận gửi/đọc tin nhắn, SSE/RabbitMQ, quyền file Cloudinary và frontend đăng nhập qua trình duyệt. API nhóm có trong code nhưng nằm ngoài luồng chat 1–1 đang bàn giao. Chưa gửi tài liệu trực tiếp cho thành viên; chia sẻ qua PR để review chéo.

## Kết quả kiểm chứng ngày 22/10/2026

Code đối chiếu commit `564e3e3`; các tài liệu và collection bàn giao là thay đổi mới chưa commit.

| Kiểm tra | Kết quả thực tế |
|---|---|
| clean verify với PostgreSQL local | 45 test, Failures=0, Errors=0, Skipped=0; BUILD SUCCESS |
| Collection Authentication qua Newman 6.2.1 | 9 request, 11 assertion, không lỗi |
| Collection hai tài khoản qua Newman 6.2.1 | 22 request, 31 assertion, không lỗi |
| JWT hợp lệ | /me trả 200, đúng chủ token |
| JWT sai hoặc thiếu | /me trả 401 |
| JWT hết hạn thật | App test cổng 18082, TTL 30 giây; login rồi chờ 31 giây, /me trả 401 |
| Hồ sơ A/B | A cập nhật kèm query userId B vẫn chỉ sửa A; kiểm tra B giữ nguyên |
| Hội thoại 1–1 | A tạo hội thoại với B; danh sách cả A và B có cùng conversationId |

Ứng dụng kiểm thử riêng được dừng sau kiểm tra; không thay đổi TTL cấu hình nguồn. Tài khoản và hội thoại kiểm thử còn trong database dev. Kết quả trên không chứng minh tài khoản thứ ba bị chặn trên mọi tài nguyên chat, cũng không chứng minh frontend/CORS hoặc SSE.
