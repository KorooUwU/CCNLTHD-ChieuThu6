package com.notification.dispatcher.DTO;

public record TokenResponseDTO(String accessToken, String tokenType, long expiresIn) {
    @Override
    public String toString() { return "TokenResponseDTO[token redacted]"; }
}
