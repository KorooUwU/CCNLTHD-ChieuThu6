package com.notification.dispatcher.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// DTO chuẩn hóa phản hồi lỗi tập trung cho cả HTTP REST và kênh WebSocket STOMP
public class ErrorResponseDTO {

    private String errorCode;
    private String errorMessage;
    private String details;
    private LocalDateTime timestamp;

}
