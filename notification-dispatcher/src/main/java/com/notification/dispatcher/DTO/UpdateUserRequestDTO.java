package com.notification.dispatcher.DTO;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cập nhật thông tin cá nhân. Tất cả trường đều optional (null = không đổi).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequestDTO {

    /** Tên hiển thị mới, null hoặc blank = giữ nguyên */
    @Size(max = 100, message = "Tên tài khoản tối đa 100 ký tự")
    private String tenTaiKhoan;
}
