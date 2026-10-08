package com.notification.dispatcher.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// DTO trả về thông tin tóm tắt người dùng (dùng cho danh sách tìm kiếm, danh bạ)
public class UserSummaryDTO {

    private String id;
    private String tenTaiKhoan;
    private String userName;
}
