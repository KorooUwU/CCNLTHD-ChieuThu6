// file này dùng để tạo một bảng ghi vào trong bảng Notification.
// Là DTO dùng để truyền dữ liệu giữa các module
package com.notification.dispatcher.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMessageNotificationDTO {

    private String userId;
    private String triggeredByUserId;
    private String type;

    private String messageId;

}
