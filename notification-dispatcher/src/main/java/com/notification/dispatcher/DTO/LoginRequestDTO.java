package com.notification.dispatcher.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(
        @NotBlank @Size(max = 50) String userName,
        @NotBlank @Size(max = 100) String password) {
    @Override
    public String toString() { return "LoginRequestDTO[credentials redacted]"; }
}
