package com.notification.dispatcher.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// DTO trả về thông tin chi tiết tài khoản người dùng
public class UserResponseDTO {

    private String id;
    private String tenTaiKhoan;
    private String email;
    private String userName;
    private Instant createdAt;
}
