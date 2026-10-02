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
// DTO nhận yêu cầu khởi tạo cuộc hội thoại mới giữa hai người dùng (User1 và User2)
public class CreateConversationRequestDTO {

    @NotBlank(message = "Partner ID không được để trống")
    private String partnerId;

}
