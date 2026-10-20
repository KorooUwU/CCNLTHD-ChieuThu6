# Hồ sơ và tìm kiếm người dùng

Hai API yêu cầu `Authorization: Bearer <accessToken>`. Dùng cổng backend đang chạy, ví dụ `http://localhost:18081`.

## Cập nhật hồ sơ

`PATCH /api/users/me`

```json
{"tenTaiKhoan": "Tên hiển thị mới"}
```

Trả `200` với UserResponseDTO của chính người đăng nhập. Tên bắt buộc, tối đa 100 ký tự; khoảng trắng đầu/cuối được loại bỏ. Chỉ tên hiển thị được cập nhật. Email, username, password và ID không thuộc hợp đồng cập nhật này. Server lấy ID từ JWT, không dùng `userId` trong query để chọn tài khoản.

## Tìm kiếm

`GET /api/users/search?keyword=bao&page=0&size=20`

Tìm chuỗi con trong username hoặc tên hiển thị, không phân biệt hoa/thường; chưa hỗ trợ tìm không dấu. Keyword bắt buộc, không trắng, tối đa 100 ký tự. `%` và `_` được xử lý như ký tự thường qua Spring Data derived query. Page từ 0 đến 10000, size từ 1 đến 50 (mặc định 20). Sắp xếp username rồi ID để phân trang ổn định khi dữ liệu không thay đổi. Kết quả có thể bao gồm chính người tìm.

```json
{
  "content": [{"id": "UUID", "tenTaiKhoan": "Bảo", "userName": "bao"}],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

Không trả email, passwordHash hoặc token trong kết quả tìm kiếm. Không có kết quả thì `content` rỗng, HTTP `200`. Đây là tìm người để bắt đầu chat, chưa bao gồm lời mời kết bạn.

## Kiểm thử Postman

1. Đăng ký A và B, đăng nhập lấy token riêng; ghi lại tên ban đầu của B.
2. Dùng token A PATCH `/api/users/me`, rồi GET `/api/users/me`: tên A đã đổi.
3. Dùng token B GET `/api/users/me`: tên B giữ nguyên.
4. Dùng token A PATCH `/api/users/me?userId=<id_B>` với tên mới: chỉ A thay đổi. Kiểm tra lại B.
5. PATCH tên trắng/quá 100 ký tự: `400`; thiếu/sai/hết hạn token: `401`.
6. Tìm theo tên mới hoặc username, kiểm tra page/size và các trường công khai. Không có token: `401`.
7. Keyword trắng, page âm, size 0 hoặc 51: `400`. Thử từ khóa chứa `%`, `_`, dấu nháy; không được mở rộng kết quả như wildcard hoặc thay đổi câu SQL.

## Kiểm thử tự động

Chạy từ `notification-dispatcher`: `./mvnw.cmd clean verify`.

`SecurityConfigTest` kiểm tra Bearer filter, danh tính cập nhật lấy từ token, validation và dữ liệu public. `UserServiceIntegrationTest` chạy trên PostgreSQL local với transaction rollback dữ liệu test, kiểm tra lưu hồ sơ, giữ nguyên tài khoản khác, tìm kiếm/phân trang và escape wildcard. Không coi các test này là minh chứng đầy đủ quyền truy cập hội thoại/tin nhắn hoặc SSE; những phần đó cần kiểm tra riêng với module tương ứng.
