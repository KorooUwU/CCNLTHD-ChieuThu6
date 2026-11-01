# BÁO CÁO TỔNG HỢP CÔNG VIỆC HOÀN THÀNH

**Dự án:** Ứng dụng Trò chuyện Trực tuyến (Spring Boot 4.x + Next.js Turbopack + PostgreSQL Supabase)  
**Thời gian thực hiện:** Ngày 01/11/2026  
**Nhánh Git:** `Quy` (Đã hợp nhất từ `origin/quocBao`)

---

## MỤC LỤC
1. [Giải quyết xung đột Git Merge (Merge Conflicts)](#1-giải-quyết-xung-đột-git-merge)
2. [Cấu hình & Vận hành Dự án](#2-cấu-hình--vận-hành-dự-án)
3. [Hiện thực Tính năng Chat Nhóm (Group Chat)](#3-hiện-thực-tính-năng-chat-nhóm)
4. [Tối ưu Thông báo Realtime (SSE & RabbitMQ Fallback)](#4-tối-ưu-thông-báo-realtime)
5. [Tạo 10 Tài khoản Test trên CSDL Supabase](#5-tạo-10-tài-khoản-test-trên-csdl-supabase)
6. [Khắc phục Lỗi Chuông Thông báo Không Mất Chấm Đỏ](#6-khắc-phục-lỗi-chuông-thông-báo)
7. [Kết quả Kiểm thử Trình duyệt Thực tế](#7-kết-quả-kiểm-thử-trình-duyệt)
8. [Hướng dẫn Khởi động & Sử dụng](#8-hướng-dẫn-khởi-động--sử-dụng)

---

## 1. GIẢI QUYẾT XUNG ĐỘT GIT MERGE
* **Nguồn:** Hợp nhất nhánh `origin/quocBao` vào nhánh cục bộ `Quy`.
* **Các tệp xung đột đã xử lý:**
  - `.gitignore`: Hợp nhất toàn bộ quy tắc ignore cho file cấu hình môi trường `.env`, các file cấu hình IDE (`.vscode/`, `.idea/`), thư mục build `target/` của Maven và các file tạm hệ điều hành.
  - `notification-dispatcher/.env.example`: Hợp nhất đầy đủ các biến môi trường cấu hình Cloudinary (`CLOUDINARY_URL`, `CLOUD_NAME`, `API_KEY`, `API_SECRET`) và RabbitMQ (`RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `RABBITMQ_VIRTUAL_HOST`).
  - `notification-dispatcher/pom.xml`: Giữ lại toàn bộ các dependency bổ sung từ cả hai nhánh gồm `cloudinary-http44:1.39.0`, `spring-boot-starter-actuator`, `spring-boot-starter-amqp`, `spring-boot-starter-security`.
* **Kết quả:** Đã tạo commit hợp nhất thành công: `[Quy 0269f6b] Merge branch 'origin/quocBao' into Quy`.

---

## 2. CẤU HÌNH & VẬN HÀNH DỰ ÁN
* **Backend:** Spring Boot 4.1.1, Java 21, chạy trên cổng `8081`. Cấu hình profile mặc định `supabase` kết nối trực tiếp đến CSDL PostgreSQL đám mây của Supabase.
* **Frontend:** Next.js 16.4 (App Router, Turbopack), chạy trên cổng `3000`.
* **CORS:** Cấu hình trong `SecurityConfig.java` cho phép các nguồn gốc cục bộ (`http://localhost:*`, `http://127.0.0.1:*`) kết nối trực tiếp đến endpoint stream Server-Sent Events (SSE).
* **Kiểm tra đăng nhập:** Đăng nhập thành công với tài khoản thật `taikhoan075` (mật khẩu `123456`) trên cơ sở dữ liệu Supabase.

---

## 3. HIỆN THỰC TÍNH NĂNG CHAT NHÓM (GROUP CHAT)
### Phía Backend
* **Ràng buộc CSDL (`chk_conversations_type`):**
  - Đối với hội thoại `GROUP`, `user1_id` và `user2_id` bắt buộc phải là `NULL`. Thành viên nhóm được lưu trữ trong bảng liên kết `conversation_members`.
  - Cập nhật logic trong `ChatService.java` và `MessageService.java` để xử lý phân nhánh an toàn, tránh lỗi `NullPointerException` khi truy cập `user1`/`user2`.
* **Phát tin nhắn đa hướng (Broadcasting):**
  - Trong `MessageService.java`: Duyệt qua toàn bộ danh sách thành viên trong nhóm, phát tin nhắn mới đến từng thành viên qua RabbitMQ / SSE, và tạo bản ghi thông báo loại `GROUP_MESSAGE` cho mọi thành viên (trừ người gửi).
* **Cập nhật Unit Test:** Sửa lại constructor mock trong `MessageServiceTest.java` để đảm bảo bộ kiểm thử đơn vị biên dịch và chạy thành công 100%.

### Phía Frontend
* **Modal tạo nhóm mới (`CreateGroupModal.tsx`):**
  - Cho phép người dùng nhập tên nhóm, tìm kiếm tài khoản thật trên Supabase qua thanh tìm kiếm.
  - Chọn và hiển thị danh sách thành viên dưới dạng chip (có thể xóa nhanh).
  - Tích hợp gọi API `POST /api/conversations/groups` để tạo nhóm trên database.
* **Giao diện Sidebar (`Sidebar.tsx`):** Thêm nút `+` tạo nhóm ở thanh tiêu đề, thêm badge *"Nhóm"* trên các mục hội thoại nhóm và bộ lọc tab *"Nhóm"*.
* **Giao diện Khung chat (`ChatArea.tsx`):**
  - Hiển thị badge *"Nhóm"* và số lượng thành viên ở header chat.
  - Hiển thị tên người gửi phía trên bong bóng chat của từng thành viên đến để phân biệt rõ ràng người gửi trong nhóm.

---

## 4. TỐI ƯU THÔNG BÁO REALTIME (SSE & RABBITMQ FALLBACK)
* **Cơ chế Fallback thông minh:**
  - Khi RabbitMQ broker chưa khởi chạy cục bộ (Docker Desktop tắt), `RabbitMQProducer.java` tự động chuyển tiếp sự kiện trực tiếp vào `SseService` trong bộ nhớ thay vì ném ngoại lệ kết nối `AmqpConnectException`.
  - Vừa đảm bảo kiến trúc sẵn sàng cho RabbitMQ khi triển khai production, vừa giúp quá trình phát triển dev nội bộ hoạt động trơn tru 100%.
* **Frontend Realtime EventSource (`app/page.tsx`):**
  - Lắng nghe luồng SSE `/api/notifications/stream?token=...`.
  - Hiển thị huy hiệu trạng thái *"Realtime SSE Online"* màu xanh lục trên Header khi kết nối thành công.
  - Xử lý các sự kiện realtime:
    - `NEW_MESSAGE`: Thêm tin nhắn mới vào khung chat và cập nhật dòng xem trước ở sidebar ngay tức thì.
    - `NOTIFICATION`: Đẩy thông báo mới vào popup chuông và cập nhật chấm đỏ.
    - `CONVERSATION_UPDATED`: Cập nhật nội dung tin nhắn cuối cùng và số lượng tin nhắn chưa đọc.

---

## 5. TẠO 10 TÀI KHOẢN TEST TRÊN CSDL SUPABASE
Đã khởi tạo và xác thực thành công 10 tài khoản người dùng trên bảng `tai_khoan` của Supabase (sử dụng mật khẩu chung: `password123`):

| STT | Username | Tên hiển thị | Email |
| :---: | :--- | :--- | :--- |
| 1 | `ducanh01` | Đức Anh | `ducanh01@example.com` |
| 2 | `baongoc02` | Bảo Ngọc | `baongoc02@example.com` |
| 3 | `haidang03` | Hải Đăng | `haidang03@example.com` |
| 4 | `thutrang04` | Thu Trang | `thutrang04@example.com` |
| 5 | `minhquan05` | Minh Quân | `minhquan05@example.com` |
| 6 | `phuongthao06` | Phương Thảo | `phuongthao06@example.com` |
| 7 | `giahuy07` | Gia Huy | `giahuy07@example.com` |
| 8 | `thanhtruc08` | Thanh Trúc | `thanhtruc08@example.com` |
| 9 | `hoangnam09` | Hoàng Nam | `hoangnam09@example.com` |
| 10 | `khanhlinh10` | Khánh Linh | `khanhlinh10@example.com` |

---

## 6. KHẮC PHỤC LỖI CHUÔNG THÔNG BÁO KHÔNG MẤT CHẤM ĐỎ
### Nguyên nhân lỗi
1. **Lệch trạng thái (State Queue Lag):** Biến đếm `unreadNotificationCount` trước đó được cập nhật bằng cách trừ thủ công trong callback setter của React, gây hiện tượng trễ cập nhật hoặc trừ không đúng số lượng.
2. **Thiếu thông tin đối soát:** Thông báo ban đầu tải từ DB không có `conversationId`, còn kiểu `Conversation` ở Frontend thiếu `partnerId` của đối phương.
3. **Chưa đồng bộ CSDL:** Khi người dùng mở xem đoạn chat, Frontend chưa gọi API đánh dấu đã đọc về Backend để lưu vào Supabase.
4. **Tin nhắn đến khi đang xem chat:** Người dùng đang mở cuộc trò chuyện với người đó nhưng khi người đó nhắn tin tới, chuông vẫn cộng thêm 1 chấm đỏ.

### Giải pháp khắc phục
* **Frontend:**
  - Thêm `partnerId` vào kiểu `Conversation` trong `types/chat.ts`.
  - Tự động đồng bộ `unreadNotificationCount` từ `notifications.filter(n => !n.isRead).length` trong `app/page.tsx`. Khi thông báo được đánh dấu đọc, chấm đỏ tự động giảm / biến mất ngay lập tức.
  - Khi người dùng click vào đoạn chat hoặc click vào thông báo trong chuông:
    - Toàn bộ thông báo của người đó / nhóm đó lập tức chuyển sang `isRead: true`.
    - Gọi API `markNotificationAsRead` và `markNotificationsReadBySender` để lưu trạng thái đã đọc vào CSDL Supabase.
  - Nếu người dùng đang mở sẵn cuộc trò chuyện đó, thông báo mới đến sẽ tự động coi là đã đọc ngay, không làm hiện chấm đỏ trên chuông.
* **Backend:**
  - Nâng cấp `NotificationService.java`: Tự động tìm kiếm `conversationId` và `messageContent` khi tải danh sách thông báo từ database.
  - Bổ sung endpoint `POST /api/notifications/read-by-sender/{senderId}` trong `NotificationController.java`.
  - Bổ sung các Next.js API Proxy routes:
    - `app/api/notifications/read/route.ts`
    - `app/api/notifications/read-by-sender/[senderId]/route.ts`

---

## 7. KẾT QUẢ KIỂM THỬ TRÌNH DUYỆT THỰC TẾ
Đã chạy 4 phiên kiểm thử tự động trên trình duyệt Chrome:
1. **Phiên 1:** Đăng nhập thành công với tài khoản thật `taikhoan075`.
2. **Phiên 2:** Đăng nhập `baongoc02`, nhận thông báo nhóm realtime từ `ducanh01`, tạo thành công nhóm mới *"Nhóm Lập Trình Web"* cùng `haidang03`.
3. **Phiên 3:** Kiểm tra luồng nhận thông báo chuông và xem chi tiết.
4. **Phiên 4:** Kiểm tra tính năng tự động xóa chấm đỏ chuông thông báo:
   - Khi `baongoc02` có 5 thông báo chưa đọc (chuông đỏ hiển thị số 5).
   - Click chuyển sang *"Nhóm Lập Trình Web"*: Chấm đỏ lập tức giảm xuống 2.
   - Click chuyển sang *"Nhóm Đồ Án CCNLTHD"*: Chấm đỏ trên chuông **lập tức biến mất hoàn toàn (về 0)**.
   - Mở popup chuông: Các thông báo đã chuyển sang trạng thái đã đọc (mất chấm tròn xanh).

---

## 8. HƯỚNG DẪN KHỞI ĐỘNG & SỬ DỤNG

Hiện tại cả **Frontend** và **Backend** đã được tắt an toàn (không còn tiến trình nào chiếm dụng cổng `3000` và `8081`).

Khi cần khởi động lại dự án, bạn thực hiện theo các bước sau:

### 1. Khởi động Backend (Spring Boot)
Mở terminal tại thư mục `notification-dispatcher`:
```powershell
cd "d:\MCRo VSCODE\CCNLTHD\CCNLTHD-ChieuThu6\notification-dispatcher"
./mvnw spring-boot:run
```
*Backend sẽ lắng nghe tại `http://localhost:8081`.*

### 2. Khởi động Frontend (Next.js)
Mở một terminal khác tại thư mục `Frontend/chatboxspringboot`:
```powershell
cd "d:\MCRo VSCODE\CCNLTHD\CCNLTHD-ChieuThu6\Frontend\chatboxspringboot"
npm run dev
```
*Frontend sẽ chạy tại `http://localhost:3000`.*

### 3. Đăng nhập kiểm tra
* Truy cập `http://localhost:3000`.
* Sử dụng bất kỳ tài khoản test nào (ví dụ: `ducanh01` hoặc `baongoc02`, mật khẩu: `password123`).
