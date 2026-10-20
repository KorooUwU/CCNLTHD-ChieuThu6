package com.notification.dispatcher.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequestDTO(@NotBlank @Size(max = 100) String tenTaiKhoan) {}
