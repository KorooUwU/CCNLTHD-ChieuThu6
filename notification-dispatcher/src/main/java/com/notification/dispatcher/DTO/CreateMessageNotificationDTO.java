// file này dùng để tạo một bảng ghi vào trong bảng Notification.
// Là DTO dùng để truyền dữ liệu giữa các module
package com.notification.dispatcher.DTO;

import lombok.Data;

@Data 
public class CreateMessageNotificationDTO {

    private String IdUser;
    private String triggeredByUserId;
    private String type;

    private String messageId;

}
