package com.notification.dispatcher.controller;

import com.notification.dispatcher.DTO.FileUploadResponseDTO;
import com.notification.dispatcher.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    /**
     * API upload ảnh / file đính kèm
     * POST /api/files/upload (multipart/form-data)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponseDTO> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        FileUploadResponseDTO response = fileUploadService.upload(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * API xóa file trên Cloudinary bằng public_id
     * DELETE /api/files?publicId=xxx
     */
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> deleteFile(@RequestParam("publicId") String publicId) {
        boolean deleted = fileUploadService.deleteFile(publicId);
        if (deleted) {
            return ResponseEntity.ok(Map.of("message", "Xóa file thành công", "publicId", publicId));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Xóa file thất bại hoặc public_id không tồn tại", "publicId", publicId));
        }
    }
}
