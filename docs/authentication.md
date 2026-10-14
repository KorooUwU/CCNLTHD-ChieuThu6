# Authentication giai đoạn 1

## API

| Method | Endpoint | Quyền | Thành công |
|---|---|---|---|
| POST | /api/auth/register | Public | 201, UserResponseDTO |
| POST | /api/auth/login | Public | 200, accessToken/tokenType/expiresIn |
| GET | /api/users/me | Bearer JWT | 200, UserResponseDTO của chủ token |

Register nhận tenTaiKhoan, email, userName, password. Login nhận userName, password. Username/email hiện unique có phân biệt hoa/thường; username và email được trim khi đăng ký. Mật khẩu không trim; BCrypt giới hạn 72 byte UTF-8. Response không có password/passwordHash. Sai username hoặc sai mật khẩu trả cùng lỗi 401.

JWT ký HS256, chứa sub là UUID user, iss=chat-system, iat, exp, jti. Thời hạn mặc định 900 giây. Dùng bộ lọc Bearer của Spring Security OAuth2 Resource Server để xác thực chữ ký, issuer, thời gian và UUID subject. Không có refresh token hoặc logout/revoke trong giai đoạn này; token hết hạn phải đăng nhập lại. /me kiểm tra tài khoản còn tồn tại trong DB.

Gửi Authorization: Bearer <accessToken>. Không truyền token qua URL, không dùng cookie xác thực. Vì chỉ dùng Bearer header và stateless, CSRF tắt; nếu chuyển sang session/cookie cần thiết kế lại CSRF. Không mở wildcard CORS; frontend cùng origin dùng được, frontend khác origin cần nhóm chốt allowlist.

## Cấu hình và chạy

Trong notification-dispatcher/.env, cấu hình PostgreSQL và JWT_SECRET. JWT_SECRET phải là Base64 của ít nhất 32 byte ngẫu nhiên, không phải mật khẩu ngắn. Không commit .env. Tạo khóa trong PowerShell và ghi thẳng vào file local:

```powershell
$jwtBytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($jwtBytes)
Add-Content .env ('JWT_SECRET=' + [Convert]::ToBase64String($jwtBytes))
```

Chỉ chạy khi chưa có JWT_SECRET; tránh thêm nhiều dòng cùng tên. Đổi khóa sẽ làm token cũ không còn hợp lệ. JWT_TTL_SECONDS mặc định 900, cho phép 1..86400.

```powershell
docker compose up -d postgres
.\mvnw.cmd spring-boot:run
```

Profile dev đọc .env. Profile supabase yêu cầu SUPABASE_DB_URL, SUPABASE_DB_USER, SUPABASE_DB_PASSWORD qua môi trường/.env; không có mật khẩu dự phòng trong source. Nếu thông tin đã lộ từng là credential thật, người quản lý Supabase cần đổi mật khẩu, cập nhật env của thành viên/CI và kiểm tra lịch sử Git. Xóa khỏi file hiện tại không thu hồi credential đã lộ. Không kết nối Supabase trong test unit/MVC.

Nếu cổng 8081 đang chạy container app cũ, dừng container đó trước khi chạy bản code mới, hoặc chạy Maven với `'-Dspring-boot.run.arguments=--server.port=18081'` và đổi baseUrl trong Postman sang http://localhost:18081. Đổi POSTGRES_USER/POSTGRES_DB trong .env không tự tạo role/database trong volume PostgreSQL đã tồn tại. Không xóa volume chứa dữ liệu để sửa lỗi mật khẩu; đồng bộ role/database với cấu hình.

## Kiểm thử

```powershell
.\mvnw.cmd '-Dtest=SecurityConfigTest,AuthControllerTest,AuthServiceTest,AuthLoginTest' test
```

Import docs/postman/authentication.postman_collection.json vào Postman, chạy Collection Runner theo thứ tự. Collection tạo username/email/mật khẩu ngẫu nhiên và giữ token trong collection variable; không export collection đã điền token/mật khẩu. Chạy tạo một tài khoản test trong DB; nên dùng database dev/test riêng.

Collection kiểm tra đăng ký, trùng tài khoản, validation, login, /me, không thể đổi danh tính qua query, thiếu/sai token và sai mật khẩu. Token hết hạn kiểm tra tự động bằng MVC test; kiểm tra thủ công bằng cách chạy JWT_TTL_SECONDS=5, đăng nhập rồi chờ trên 5 giây trước gọi /me.

Chạy collection bằng Newman (CLI của Postman collection):

```powershell
npx.cmd --yes newman@6.2.1 run docs/postman/authentication.postman_collection.json --env-var baseUrl=http://localhost:18081
```

Lệnh chạy từ thư mục gốc repo; thay cổng theo app đang chạy. Ngày 2026-10-14, collection đã chạy trên database PostgreSQL tạm riêng: 9 request, 11 assertion, không lỗi. Database tạm đã được dọn sau kiểm thử.

Lỗi trả errorCode/errorMessage/details/timestamp (timestamp UTC, DTO hiện LocalDateTime). 400 validation/body/input; 401 credential/token; 409 duplicate; 500 nội bộ. Lỗi Security không qua ControllerAdvice nên SecurityConfig có handler JSON riêng.

## Bàn giao và giới hạn

Chưa bao gồm Chat REST, SSE/WebSocket hoặc quyền sở hữu tin nhắn. Code do AI hỗ trợ cần nhóm đọc và giải thích constructor injection, transaction, BCrypt và luồng Bearer filter. Kết quả test thật cần ghi vào PR; không coi mock repository là kiểm chứng DB end-to-end. Mật khẩu seed cũ phải kiểm chứng riêng, không mặc định tin comment hash; có thể đăng ký user mới để demo.
