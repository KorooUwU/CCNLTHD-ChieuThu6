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
// DTO quản lý và broadcast trạng thái phiên kết nối của người dùng (ONLINE, OFFLINE)
public class UserPresenceDTO {

    private String userId;
    private String status; // ONLINE, OFFLINE, AWAY
    private LocalDateTime lastActiveAt;

}
