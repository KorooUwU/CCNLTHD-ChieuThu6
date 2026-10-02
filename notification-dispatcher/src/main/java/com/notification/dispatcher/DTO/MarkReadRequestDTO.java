package com.notification.dispatcher.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// DTO này dùng để xác định thông báo đã được đọc hay chưa
public class MarkReadRequestDTO {

    @NotBlank(message = "Notification ID không được để trống")
    private String notificationId;

}

