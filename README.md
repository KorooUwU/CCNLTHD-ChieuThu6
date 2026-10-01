# Notification Alert Dispatcher

Dự án học tập Spring Boot, Java 21 và PostgreSQL. Hiện có trang HTML kiểm tra ứng dụng, cấu hình database, Docker và CI. API nghiệp vụ, Security, migration và seed đang chờ các thành viên triển khai. RabbitMQ dự kiến bổ sung tuần 4.

## Chuẩn bị

- Chạy bằng terminal: JDK 21 và Docker Desktop đang hoạt động. Maven Wrapper tải Maven nên không cần cài Maven riêng.
- Chạy toàn bộ bằng Docker: Docker Desktop và Internet để tải image, dependency.

Từ thư mục gốc repository:

```powershell
cd notification-dispatcher
Copy-Item .env.example .env
```

Chỉ copy nếu chưa có `.env`. Thay `YOUR_PASSWORD` trong `.env` bằng mật khẩu local tự chọn trước lần khởi tạo database đầu tiên. Không commit `.env`.

## Chạy database và ứng dụng local

```powershell
docker compose up -d postgres
docker compose ps
.\mvnw.cmd spring-boot:run
```

Profile `dev` tự đọc `.env` trong thư mục `notification-dispatcher`. Mở http://localhost:8081. Nhấn Ctrl+C để dừng ứng dụng.

## Chạy app và database bằng Docker

```powershell
docker compose up --build -d
docker compose logs -f app
```

App dùng profile `docker`, kết nối hostname `postgres` và chờ database healthy trước khi khởi động. Mở http://localhost:8081, hoặc cổng `APP_PORT` trong `.env`.

Không chạy app local và app Docker trên cùng cổng. Dừng app Docker trước khi chạy local:

```powershell
docker compose stop app
```

Dừng toàn bộ nhưng giữ dữ liệu:

```powershell
docker compose down
```

Volume lưu dữ liệu PostgreSQL qua các lần khởi động. Đổi `POSTGRES_PASSWORD` trong `.env` không tự đổi mật khẩu trong database đã tồn tại. Khi cần đổi, cập nhật mật khẩu role trong PostgreSQL rồi đồng bộ cấu hình. `docker compose down -v` xóa dữ liệu database; chỉ dùng khi chủ động muốn reset môi trường local.

## Build và kiểm thử

Build/test với cấu hình dev và database local đang chạy:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress clean verify
```

Profile `test` không đọc `.env`. Để chạy với profile này, đặt `SPRING_PROFILES_ACTIVE=test` cùng `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` trong môi trường terminal; `POSTGRES_HOST` và `POSTGRES_PORT` mặc định localhost/5432. Database phải tồn tại và chạy trước khi kiểm thử.

GitHub Actions chạy `clean verify` mỗi push/PR, tạo PostgreSQL riêng và cung cấp biến môi trường cho profile `test`. Dockerfile bỏ qua test khi đóng gói vì giai đoạn build image không có database; CI chịu trách nhiệm chạy test. Hiện test kiểm tra Spring context và kết nối JPA, chưa kiểm chứng nghiệp vụ. Báo cáo test được tải lên khi bước kiểm thử lỗi.

## Phân công và tích hợp

- TV1: DTO, Controller, contract, Swagger và test API.
- TV2: migration, Entity, Repository và seed idempotent.
- TV3: Service, validation, exception handler, session và phân quyền.
- TV4: Docker, Compose, profile, CI và hướng dẫn chạy.

Thống nhất với TV4 trước khi cùng sửa `pom.xml`, cấu hình môi trường hoặc workflow. Dùng feature branch và PR có review trước khi merge develop. Tài khoản demo và URL Swagger sẽ được ghi khi Security, seed và Swagger hoàn thành.

## AI hỗ trợ và kiểm chứng

Codex hỗ trợ cấu hình database, sửa Maven Wrapper trên PowerShell, xử lý tên múi giờ Java, cấu hình CI và chuẩn bị Docker/README. Nhóm cần review thay đổi, chạy lại build/test, kiểm tra Docker và bổ sung bằng chứng PR/review. Không coi test khởi động thành bằng chứng hoàn thành API hoặc Security.
