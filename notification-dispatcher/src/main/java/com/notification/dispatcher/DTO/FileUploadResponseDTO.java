package com.notification.dispatcher.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponseDTO {
    private String url;
    private String publicId;
    private String originalFilename;
    private String format;
    private Long bytes;
    private String resourceType;
}
