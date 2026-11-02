package com.notification.dispatcher.DTO;

import lombok.*;
import java.time.Instant;

/**
 * DTO trả về metadata tệp đính kèm sau khi upload thành công.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponseDTO {

    private String attachmentId;
    private String originalName;
    private String mimeType;
    private long fileSize;
    /** URL tải file: GET /api/attachments/{id}/download */
    private String downloadUrl;
    private Instant uploadedAt;
}
