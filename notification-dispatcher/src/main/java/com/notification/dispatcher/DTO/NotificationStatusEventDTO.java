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
// DTO theo dõi vòng đời phát tán thông báo (PENDING, DISPATCHED, DELIVERED, FAILED)
public class NotificationStatusEventDTO {

    private String idNotification;
    private String status; // PENDING, DISPATCHED, DELIVERED, FAILED
    private LocalDateTime updatedAt;
    private String failureReason;

}
