package com.notification.dispatcher.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberRequestDTO {
    @NotBlank(message = "User ID không được để trống")
    private String userId;
}
