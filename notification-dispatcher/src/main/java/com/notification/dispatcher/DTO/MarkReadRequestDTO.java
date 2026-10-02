package com.notification.dispatcher.DTO;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class MarkReadRequestDTO {
    @NotBlank(message = "ID thông báo ko được để trống")
    private String idNotification;
}

