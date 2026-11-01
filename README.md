# Chat và Notification Dispatcher

Đồ án Spring Boot xây dựng hệ thống chat 1–1 có xác thực JWT, hồ sơ người dùng, tìm kiếm, lưu hội thoại và tin nhắn trên PostgreSQL. Repository gồm backend Spring Boot và frontend Next.js. SSE và RabbitMQ là hướng tích hợp realtime của nhóm; chưa có endpoint SSE hoặc cấu hình RabbitMQ trong phiên bản được đối chiếu cho README này.

## Công nghệ và cấu trúc

| Thành phần | Công nghệ |
|---|---|
| Backend | Java 21, Spring Boot 4.1.1, Maven Wrapper |
| Dữ liệu | PostgreSQL 17 local hoặc Supabase PostgreSQL, Spring Data JPA, Flyway |
| Xác thực | Spring Security, OAuth2 Resource Server, JWT HS256, BCrypt |
| Frontend | Next.js 16.4.0, React 19.3.0, TypeScript |
| Tệp đính kèm | Cloudinary |
| Kiểm thử | JUnit, Mockito, MockMvc, PostgreSQL integration test, Postman/Newman |

```text
notification-dispatcher/        Backend, Maven Wrapper, Docker Compose PostgreSQL
  src/main/java/               Controller, service, repository, entity, DTO, security
  src/main/resources/          Profile cấu hình và migration Flyway
  src/test/                    Test tự động
Frontend/chatboxspringboot/    Giao diện Next.js và API routes chuyển tiếp tới backend
docs/                         Tài liệu Authentication, User API, upload và bàn giao
```

## Yêu cầu môi trường

- JDK 21; kiểm tra `java -version`. `JAVA_HOME` phải trỏ đến JDK.
- Docker Desktop đang chạy nếu sử dụng PostgreSQL local.
- Node.js và npm tương thích với phiên bản Next.js trong `Frontend/chatboxspringboot/package.json` nếu chạy frontend.
- Postman nếu kiểm thử API bằng giao diện. Không cần cài Maven riêng vì đã có wrapper.

Các lệnh dưới đây dùng PowerShell trên Windows, bắt đầu tại thư mục gốc repository. Với macOS/Linux, dùng `./mvnw` thay ` .\mvnw.cmd` và cú pháp shell tương ứng.

## Thiết lập biến môi trường

```powershell
cd notification-dispatcher
Copy-Item .env.example .env
```

Chỉ sao chép khi chưa có `.env`; không ghi đè cấu hình đang sử dụng. Chỉnh `.env` với thông tin của môi trường bạn quản lý:

```dotenv
POSTGRES_DB=notification_dispatcher
POSTGRES_USER=notification
POSTGRES_PASSWORD=REPLACE_WITH_LOCAL_PASSWORD
POSTGRES_PORT=5432
JWT_SECRET=REPLACE_WITH_RANDOM_BASE64_KEY
JWT_TTL_SECONDS=900

# Chỉ cần khi chạy profile supabase
SUPABASE_DB_URL=jdbc:postgresql://<host>:<port>/<database>?sslmode=require
SUPABASE_DB_USER=<database-user>
SUPABASE_DB_PASSWORD=<database-password>

# Cần để kiểm thử upload thực tế
CLOUDINARY_CLOUD_NAME=<cloud-name>
CLOUDINARY_API_KEY=<api-key>
CLOUDINARY_API_SECRET=<api-secret>
```

JWT_SECRET là Base64 của ít nhất 32 byte ngẫu nhiên. Chạy đoạn sau **một lần khi chưa có JWT_SECRET**, rồi thay dòng placeholder bằng kết quả được ghi trong `.env` để tránh trùng tên biến:

```powershell
$jwtBytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($jwtBytes)
$jwtLine = 'JWT_SECRET=' + [Convert]::ToBase64String($jwtBytes)
$envLines = Get-Content .env
$envLines = $envLines | Where-Object { $_ -notmatch '^JWT_SECRET=' }
Set-Content .env -Value @($envLines; $jwtLine) -Encoding UTF8
```

Nếu PowerShell/.NET không hỗ trợ `Fill`, thay dòng tạo byte bằng:

```powershell
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($jwtBytes)
```

Không commit `.env`, token hoặc mật khẩu thật. Secret từng bị lộ cần được thu hồi/đổi tại dịch vụ tương ứng; xóa khỏi file không thu hồi secret cũ. Hiện `application-cloudinary.properties` còn chứa secret dự phòng trong source: cần loại bỏ giá trị này và đổi secret trước khi chia sẻ/triển khai; README không sao chép giá trị đó.

## Chạy backend với PostgreSQL local

Từ thư mục `notification-dispatcher`:

```powershell
docker compose up -d postgres
docker compose ps
.\mvnw.cmd spring-boot:run
```

Chờ PostgreSQL `healthy` và dòng `Started NotificationDispatcherApplication`. Profile mặc định là `dev`; backend chạy tại `http://localhost:8081`.

Compose hiện chỉ chạy PostgreSQL; backend vẫn chạy bằng Maven. Chưa có lệnh Compose khởi chạy toàn bộ frontend/backend/database.

Để chạy backend riêng ở cổng khác:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=18081"
```

Đổi baseUrl trong Postman tương ứng. Đổi POSTGRES_USER/POSTGRES_DB/PASSWORD trong `.env` không tự đổi role/database/mật khẩu trong volume đã khởi tạo; cần đồng bộ với database thực tế. Không xóa volume chứa dữ liệu để sửa lỗi kết nối.

## Chạy backend với Supabase

Điền đúng ba biến SUPABASE_DB_* trong `.env`, lấy thông tin kết nối từ người quản lý database của nhóm. Không dùng URL HTTP của dự án hoặc Supabase API key làm JDBC URL/mật khẩu database.

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=supabase" "-Dspring-boot.run.arguments=--server.port=18081"
```

Không cần PostgreSQL Docker local cho profile này. Flyway chạy trên database Supabase đang cấu hình, nên cần kiểm tra migration trước khi khởi động với database dùng chung. Với frontend hiện tại, giữ backend cổng 8081 bằng cách bỏ tham số đổi port hoặc dùng `--server.port=8081`.

Nếu gặp `EMAXCONNSESSION ... max clients reached in session mode`, dừng các backend dư và disconnect công cụ database không dùng; nhờ người quản lý kiểm tra pool dùng chung. Có thể giới hạn Hikari theo từng instance bằng tham số:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=supabase" "-Dspring-boot.run.arguments=--server.port=18081 --spring.datasource.hikari.maximum-pool-size=2 --spring.datasource.hikari.minimum-idle=0"
```

Giảm pool của bản chạy mới không giải phóng session do ứng dụng khác đang giữ.

## Chạy frontend

Mở terminal khác, từ thư mục gốc repository:

```powershell
cd Frontend/chatboxspringboot
npm ci
npm run dev
```

Truy cập địa chỉ Next.js in trong terminal, thường là `http://localhost:3000`. Frontend gọi các API routes cùng origin; các route server hiện chuyển tiếp tới **`http://localhost:8081`**. Backend cổng 18081 chỉ dùng được với frontend sau khi đồng bộ địa chỉ trong các route; hiện chưa có biến BACKEND_URL dùng chung. Có thể dùng hai cửa sổ trình duyệt riêng/ẩn danh để thử hai tài khoản.

Frontend và backend có code đăng nhập, tìm kiếm, hội thoại, tin nhắn và upload; sự hiện diện của code không đồng nghĩa đã kiểm chứng toàn bộ giao diện end-to-end. Không coi luồng REST hiện tại là minh chứng SSE/RabbitMQ hoạt động.

## API backend

Register và login công khai; các endpoint còn lại yêu cầu `Authorization: Bearer <accessToken>`. Backend lấy danh tính từ JWT, không tin userId client truyền để chọn chủ tài khoản.

| Method | Endpoint | Chức năng |
|---|---|---|
| POST | /api/auth/register | Đăng ký: tenTaiKhoan, email, userName, password |
| POST | /api/auth/login | Đăng nhập: userName, password; trả accessToken, tokenType, expiresIn |
| GET | /api/users/me | Hồ sơ của chủ JWT |
| PATCH | /api/users/me | Cập nhật tenTaiKhoan của chính mình |
| GET | /api/users/search?keyword=bao&page=0&size=20 | Tìm tên/username, phân trang; không trả email/password hash |
| POST | /api/conversations/create | Tạo/lấy hội thoại 1–1 với partnerId |
| GET | /api/conversations/get | Danh sách hội thoại của người đăng nhập |
| POST | /api/messages | Gửi tin nhắn: conversationId, messageContent; các trường tệp theo DTO |
| GET | /api/conversations/{conversationId}/messages | Lịch sử tin nhắn |
| POST | /api/files/upload | Upload multipart với trường file |
| POST | /api/conversations/groups | API nhóm bổ sung trong code |
| POST | /api/conversations/{conversationId}/members | Thêm thành viên nhóm |
| DELETE | /api/conversations/{conversationId}/members/{memberId} | Xóa thành viên nhóm |

Ví dụ PATCH hồ sơ:

```json
{"tenTaiKhoan": "Tên hiển thị mới"}
```

JWT hết hạn cần đăng nhập lại; chưa có refresh token hoặc thu hồi token. Lỗi có errorCode, errorMessage, details, timestamp. Xem thêm [Authentication](docs/authentication.md), [User API](docs/user-api.md), [Cloudinary](docs/cloudinary_file_upload.md). Các tài liệu chi tiết có thể được viết ở mốc code trước; endpoint thực tế đối chiếu controller hiện tại. Chưa tích hợp Swagger/OpenAPI.

## Database và dữ liệu demo

Flyway hiện có V1–V6: schema ban đầu, seed, password hash, loại tin nhắn, hội thoại nhóm, image URL. Không đổi nội dung migration đã áp dụng hoặc dùng trùng version; bổ sung migration mới cho thay đổi schema.

V2 tạo user_a/user_b cùng hội thoại và tin nhắn mẫu. Không mặc định mật khẩu seed là hợp lệ để demo; đăng ký hai tài khoản mới qua API/giao diện hoặc chạy collection. Collection lưu tài khoản/hội thoại mới trong database; dùng database dev/test, không chạy vào production.

## Kiểm thử

Từ thư mục backend, chuẩn bị `.env` và PostgreSQL local trước:

```powershell
docker compose up -d postgres
.\mvnw.cmd clean verify
```

Test context/repository và UserServiceIntegrationTest cần PostgreSQL. Không dùng Supabase chung cho các ca test DB local. Chỉ chạy các test Authentication không cần database:

```powershell
.\mvnw.cmd "-Dtest=AuthServiceTest,AuthLoginTest,AuthControllerTest,SecurityConfigTest" test
```

Từ thư mục gốc repo, import vào Postman:

- `docs/postman/authentication.postman_collection.json`: register/login/me, duplicate, validation và lỗi token.
- `docs/postman/user-handoff.postman_collection.json`: hai tài khoản, cập nhật hồ sơ đúng chủ, tìm kiếm/phân trang, hội thoại A–B và JWT.

Đặt biến baseUrl theo cổng backend. Run collection theo thứ tự, một iteration. Với collection bàn giao, mặc định checkExpiry=false bỏ qua ca hết hạn. Muốn chạy ca này, dùng backend test riêng có `--app.jwt.ttl-seconds=30`, đặt checkExpiry=true; request cuối chờ 31 giây và mong đợi 401.

Hoặc chạy qua Newman, từ thư mục gốc:

```powershell
npx.cmd --yes newman@6.2.1 run docs/postman/authentication.postman_collection.json --env-var baseUrl=http://localhost:18081
npx.cmd --yes newman@6.2.1 run docs/postman/user-handoff.postman_collection.json --env-var baseUrl=http://localhost:18081
```

Kết quả đã ghi nhận ngày 22/10/2026 trên commit 564e3e3: 45 test pass; collection Authentication 9 request/11 assertion; collection bàn giao có kiểm tra hết hạn 22 request/31 assertion, không lỗi. Đây là kết quả lịch sử, chưa xác nhận cho các thay đổi Message API/frontend mới sau đó. Kiểm tra kết quả mới trong `target/surefire-reports` sau mỗi lần chạy. Không export collection còn token/password thật.

## Xử lý lỗi thường gặp

| Triệu chứng | Kiểm tra và xử lý |
|---|---|
| Port 8081 already in use | `Get-NetTCPConnection -LocalPort 8081 -State Listen`; kiểm tra PID bằng Get-Process, dừng đúng app bằng Ctrl+C hoặc đổi cổng |
| jdbcUrl là `${SUPABASE_DB_URL}` | Biến chưa được đọc; kiểm tra `.env` ở thư mục backend, chạy Maven từ đúng thư mục và chọn profile supabase |
| Unable to determine Dialect | Đọc lỗi JDBC phía trên: database chưa chạy, URL/mật khẩu sai, DNS hoặc pool đầy; sửa nguyên nhân kết nối |
| Found more than one migration | Kiểm tra version file SQL và flyway_schema_history; giữ nguyên migration đã áp dụng, đánh số mới cho migration bổ sung |
| API 401 | Kiểm tra Bearer token, hạn token, JWT_SECRET và issuer; đăng nhập lại sau khi đổi key |
| Frontend không gọi được API | Xác nhận backend 8081, log API routes Next.js và trạng thái đăng nhập; Postman pass không chứng minh giao diện đã tích hợp đúng |

## Quy trình đóng góp

Tạo nhánh tính năng từ develop, commit đúng file liên quan, push nhánh và mở pull request vào develop để review chéo. Không commit thẳng nhánh chính. Chạy test phù hợp và ghi kết quả thật trong PR. Không stage `.env`, `target`, `node_modules` hoặc file tạm `.report-work`. Phần AI hỗ trợ cần được nhóm đọc, kiểm chứng và khai báo trong báo cáo theo yêu cầu môn học.
