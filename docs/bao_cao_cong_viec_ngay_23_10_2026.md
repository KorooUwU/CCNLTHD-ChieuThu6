# Báo Cáo Tiến Độ Phát Triển Hệ Thống Chatbox (Ngày 23/10/2026)

Tài liệu này tổng hợp chi tiết toàn bộ các công việc, thay đổi kiến trúc, nâng cấp cơ sở dữ liệu và mã nguồn đã hoàn thành trong ngày làm việc.

---

## 1. Tổng quan mục tiêu
* Kết nối hoàn chỉnh Frontend (Next.js 16 / React 19) với Backend (Spring Boot 4 / Java 21).
* Xây dựng giao diện Chatbox mô phỏng chuẩn Facebook Messenger hiện đại, thanh thoát.
* Chuyển đổi toàn bộ môi trường lưu trữ từ PostgreSQL nội bộ sang **Supabase PostgreSQL Cloud**.
* Xóa bỏ 100% Mock Data, kết nối trực tiếp với dữ liệu người dùng và hội thoại thật.
* Xây dựng hoàn chỉnh luồng Xác thực (Đăng nhập, Đăng ký, Đăng xuất, Lưu trữ JWT token).
* Tái cấu trúc mã nguồn Frontend thành các component độc lập, dễ bảo trì.
* Nâng cấp CSDL và hệ thống tin nhắn đa phương tiện: Thêm các cột chuyên dụng lưu link ảnh Cloudinary (`image_url`) và Cloudinary Public ID (`attachment_public_id`), cho phép gửi tin nhắn ảnh, tin nhắn chữ hoặc kết hợp cả hai.

---

## 2. Chi tiết các hạng mục đã triển khai

### 2.1. Chuyển đổi Cơ sở dữ liệu sang Supabase Cloud
* Ngắt toàn bộ dịch vụ PostgreSQL container nội bộ (Docker).
* Chuyển kết nối của backend sang Supabase PostgreSQL Pooler (`aws-0-ap-southeast-2.pooler.supabase.com:5432/postgres`) qua Spring Profile `supabase`.
* Cấu hình an toàn các biến môi trường trong `notification-dispatcher/.env`:
  * `SUPABASE_DB_URL`
  * `SUPABASE_DB_USER`
  * `SUPABASE_DB_PASSWORD`
* Đồng bộ và xác thực lịch sử migration Flyway trên Supabase thành công.

---

### 2.2. Xóa bỏ Mock Data & Đồng bộ Dữ liệu thật
* Loại bỏ toàn bộ mảng dữ liệu giả lập (`INITIAL_CONVERSATIONS`, danh sách thông báo giả, danh sách bạn bè mẫu) trong mã nguồn Frontend.
* Thay thế bằng các luồng gọi API trực tiếp tới Supabase:
  * Lấy danh sách cuộc trò chuyện thật (`GET /api/conversations/get`).
  * Tìm kiếm người dùng thật theo từ khóa (`GET /api/users/search`).
  * Khởi tạo cuộc trò chuyện 1-1 thật giữa 2 tài khoản (`POST /api/conversations/create`).

---

### 2.3. Xây dựng Luồng Xác thực (Authentication Flow)
* **Phía Backend:** Tận dụng hệ thống JWT Bearer Stateless với phân quyền bảo mật qua `SecurityConfig`.
* **Phía Frontend:**
  * Tạo modal đăng nhập & đăng ký chuyên nghiệp (`components/AuthModal.tsx`).
  * Lưu trữ và quản lý JWT `accessToken` trong `localStorage`.
  * Tạo các Next.js API Routes trung gian:
    * `POST /api/auth/login`: Xác thực và trả về JWT Bearer token.
    * `POST /api/auth/register`: Đăng ký tài khoản người dùng mới vào bảng `tai_khoan`.
    * `GET /api/users/me`: Lấy thông tin cá nhân của người dùng hiện tại từ token.
  * Tự động kiểm tra phiên đăng nhập khi tải lại trang; cung cấp nút Đăng xuất an toàn.

---

### 2.4. Thiết kế Giao diện Messenger Clone & Trải nghiệm Người dùng (UI/UX)
* Mô phỏng chuẩn phong cách Facebook Messenger:
  * Thanh Header: Biểu tượng tìm kiếm, chuông thông báo (kèm badge đỏ số lượng), avatar động người dùng (qua UI-Avatars), menu thả xuống tài khoản và nút Đăng xuất.
  * Cột bên trái (Sidebar): Thanh tìm kiếm đoạn chat, các tab lọc (*Tất cả*, *Chưa đọc*, *Nhóm*), danh sách cuộc hội thoại với avatar, tên đối tác, đoạn trích tin nhắn cuối, thời gian và chấm xanh báo trạng thái online.
  * Vùng xem trước (Empty State): Minh họa bong bóng chat gradient Messenger hiện đại cùng thông điệp hướng dẫn chọn đoạn chat.
  * Khung trò chuyện (Chat Area):
    * Cuộn tự động mượt mà xuống tin nhắn mới nhất.
    * Phân biệt rõ ràng bong bóng tin nhắn của người gửi (xanh dương `#0084ff`) và người nhận (xám nhạt `#f0f2f5`).
    * Hỗ trợ hiển thị ảnh Cloudinary bo góc với thẻ định danh `Cloudinary`, click để mở ảnh gốc ở tab mới.

---

### 2.5. Tái cấu trúc (Refactoring) Frontend
Tách nhỏ file `app/page.tsx` từ hơn 800 dòng phức tạp thành cấu trúc module hóa chuẩn Clean Architecture:
* `types/chat.ts`: Khai báo các interface dùng chung (`Message`, `Conversation`, `CurrentUser`, `UserSummary`, `AuthResponse`).
* `lib/api.ts`: Module tập trung toàn bộ các hàm gọi API client (`loginUser`, `registerUser`, `fetchCurrentUser`, `fetchConversations`, `sendMessageApi`, `fetchMessagesApi`, `uploadImageToCloudinary`,...).
* `components/AuthModal.tsx`: Modal Đăng nhập / Đăng ký.
* `components/Header.tsx`: Thanh điều hướng phía trên kèm tìm kiếm Supabase thời gian thực.
* `components/Sidebar.tsx`: Danh sách cuộc trò chuyện và bộ lọc.
* `components/EmptyState.tsx`: Giao diện chào mừng khi chưa chọn cuộc trò chuyện.
* `components/ChatArea.tsx`: Khung chat, hiển thị tin nhắn và thanh công cụ soạn thảo/đính kèm ảnh.
* `app/page.tsx`: Component điều phối chính với mã nguồn ngắn gọn (<250 dòng).

---

### 2.6. Nâng cấp CSDL và Hệ thống Tin nhắn Ảnh (Theo Cách 2)

#### a. Migration CSDL Flyway (`V6__add_image_url_to_messages.sql`)
Thực thi trực tiếp lên cơ sở dữ liệu Supabase:
```sql
ALTER TABLE messages
    ALTER COLUMN content DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS image_url TEXT,
    ADD COLUMN IF NOT EXISTS attachment_public_id VARCHAR(255);

ALTER TABLE messages
    ADD CONSTRAINT chk_messages_content_or_image
    CHECK (content IS NOT NULL OR image_url IS NOT NULL);
```
* Bổ sung cột chuyên biệt `image_url` lưu đường dẫn ảnh bảo mật từ Cloudinary.
* Bổ sung cột `attachment_public_id` lưu Cloudinary Public ID giúp thuận tiện quản lý/thu hồi file sau này.
* Chuyển `content` thành nullable để hỗ trợ gửi tin nhắn chỉ có ảnh mà không cần nhập văn bản.
* Ràng buộc kiểm tra tính hợp lệ: Tin nhắn bắt buộc phải có ít nhất nội dung chữ hoặc hình ảnh.

#### b. Cập nhật Entity & DTO Backend
* `Message.java`: Khai báo các trường `@Column(name = "image_url")` và `@Column(name = "attachment_public_id")`, hỗ trợ các constructor linh hoạt.
* `SendMessageRequestDTO.java`: Thêm `imageUrl`, `attachmentPublicId`; bỏ `@NotBlank` bắt buộc trên `messageContent`.
* `MessageResponseDTO.java`: Trả về `imageUrl`, `attachmentPublicId` ra client.
* `ConversationService.java`: Cập nhật logic xem trước tin nhắn cuối (`lastMessageContent` hiển thị `[Hình ảnh]` nếu tin nhắn cuối là ảnh).

#### c. Viết mới Message Service & REST Controller
* `MessageService.java`:
  * `sendMessage(senderId, request)`: Kiểm tra quyền tham gia cuộc trò chuyện, lưu tin nhắn chữ/ảnh vào DB Supabase và cập nhật `last_message_at` của conversation.
  * `getMessages(currentUserId, conversationId)`: Lấy toàn bộ lịch sử tin nhắn sắp xếp theo thời gian tăng dần và đánh dấu tin nhắn đã đọc (`markAllMessagesAsRead`).
* `MessageController.java`:
  * `POST /api/messages`: Endpoint gửi tin nhắn.
  * `GET /api/conversations/{conversationId}/messages`: Endpoint lấy danh sách tin nhắn.

#### d. Tích hợp trọn vẹn luồng Upload & Lưu trữ
* Người dùng chọn ảnh trong giao diện -> Tải lên Cloudinary thông qua `/api/upload`.
* Cloudinary trả về `secure_url` và `public_id`.
* Frontend tự động gửi `POST /api/messages` kèm `imageUrl` và `attachmentPublicId` để lưu vĩnh viễn vào Supabase.
* Khi mở cuộc hội thoại, tin nhắn ảnh cùng tin nhắn văn bản được tải đồng thời từ Supabase và hiển thị mượt mà trên khung chat.

---

## 3. Danh mục các file tạo mới & chỉnh sửa

| Tên File | Thư mục | Trạng thái | Nội dung chính |
|---|---|---|---|
| `V6__add_image_url_to_messages.sql` | `notification-dispatcher/.../db/migration` | **Tạo mới** | Script Flyway thêm cột `image_url`, `attachment_public_id` và ràng buộc CHECK |
| `Message.java` | `notification-dispatcher/.../entity` | Chỉnh sửa | Bổ sung `imageUrl`, `attachmentPublicId`, cập nhật constructor JPA |
| `SendMessageRequestDTO.java` | `notification-dispatcher/.../DTO` | Chỉnh sửa | Bổ sung trường ảnh, cho phép content rỗng khi gửi ảnh |
| `MessageResponseDTO.java` | `notification-dispatcher/.../DTO` | Chỉnh sửa | Bổ sung trường `imageUrl` và `attachmentPublicId` |
| `ConversationService.java` | `notification-dispatcher/.../service` | Chỉnh sửa | Cập nhật `lastMessageContent` hiển thị `[Hình ảnh]` |
| `MessageService.java` | `notification-dispatcher/.../service` | **Tạo mới** | Xử lý logic gửi, lấy và lưu tin nhắn vào CSDL |
| `MessageController.java` | `notification-dispatcher/.../controller` | **Tạo mới** | REST API `/api/messages` và `/api/conversations/{id}/messages` |
| `MessageServiceTest.java` | `notification-dispatcher/.../service` | **Tạo mới** | Bộ unit test kiểm thử gửi text, gửi ảnh, kiểm tra quyền và lấy tin nhắn |
| `types/chat.ts` | `Frontend/chatboxspringboot/types` | **Tạo mới** | Khai báo toàn bộ TypeScript interface dùng chung |
| `lib/api.ts` | `Frontend/chatboxspringboot/lib` | **Tạo mới** | Bộ hàm gọi API tập trung (Auth, User, Conversation, Message, Cloudinary) |
| `components/AuthModal.tsx` | `Frontend/chatboxspringboot/components` | **Tạo mới** | Modal đăng nhập/đăng ký chuẩn UI Messenger |
| `components/Header.tsx` | `Frontend/chatboxspringboot/components` | **Tạo mới** | Header thanh điều hướng, tìm kiếm người dùng thật, menu profile |
| `components/Sidebar.tsx` | `Frontend/chatboxspringboot/components` | **Tạo mới** | Danh sách hội thoại và bộ lọc trạng thái |
| `components/EmptyState.tsx` | `Frontend/chatboxspringboot/components` | **Tạo mới** | Màn hình minh họa khi chưa chọn cuộc trò chuyện |
| `components/ChatArea.tsx` | `Frontend/chatboxspringboot/components` | **Tạo mới** | Khung chat, render bong bóng chat, thẻ ảnh Cloudinary, bộ gõ và upload |
| `app/api/messages/route.ts` | `Frontend/chatboxspringboot/app/api` | **Tạo mới** | API Route chuyển tiếp gửi tin nhắn tới Spring Boot |
| `app/api/conversations/[id]/messages/route.ts` | `Frontend/chatboxspringboot/app/api` | **Tạo mới** | API Route lấy lịch sử tin nhắn của cuộc trò chuyện |
| `app/page.tsx` | `Frontend/chatboxspringboot/app` | Chỉnh sửa | Tái cấu trúc thành trang điều phối chính kết nối đầy đủ các component |

---

## 4. Kết quả Kiểm thử & Đánh giá Chất lượng

1. **Kiểm thử Cơ sở dữ liệu & Flyway Migration:**
   * Flyway tự động nhận diện và áp dụng script `V6` trên Supabase:
     ```text
     Migrating schema "public" to version "6 - add image url to messages"
     Successfully applied 1 migration to schema "public", now at version v6
     ```
2. **Kiểm thử Tự động Backend Spring Boot:**
   * Chạy thành công toàn bộ bộ kiểm thử với Maven:
     * `ConversationServiceTest`: 9/9 passed.
     * `MessageServiceTest`: 4/4 passed.
     * `NotificationDispatcherApplicationTests`: 2/2 passed trực tiếp trên Supabase.
   * Kết quả: `BUILD SUCCESS` (0 failures, 0 errors).
3. **Kiểm tra Biên dịch Frontend:**
   * Chạy lệnh kiểm tra kiểu tĩnh TypeScript:
     ```powershell
     npx tsc --noEmit
     ```
   * Kết quả: **0 lỗi** (Exit code: 0).
4. **Kiểm thử Trực quan trên Trình duyệt:**
   * Người dùng đăng nhập/đăng ký thành công, token được lưu an toàn.
   * Danh sách hội thoại và danh sách người dùng được tải đúng từ Supabase.
   * Upload ảnh lên Cloudinary thành công, hiển thị thẻ ảnh xem trước và lưu link ảnh trực tiếp vào database Supabase.

---

## 5. Hướng dẫn Khởi chạy Hệ thống

### 5.1. Khởi chạy Backend (Spring Boot với Profile Supabase)
Tại thư mục `notification-dispatcher`:
```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=supabase"
```
*Backend khởi chạy tại: `http://localhost:8081`.*

### 5.2. Khởi chạy Frontend (Next.js)
Tại thư mục `Frontend/chatboxspringboot`:
```powershell
npm run dev
```
*Frontend truy cập tại: `http://localhost:3000`.*
