package com.notification.dispatcher.controller;

import com.notification.dispatcher.DTO.AttachmentResponseDTO;
import com.notification.dispatcher.entity.MessageAttachment;
import com.notification.dispatcher.service.AttachmentService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * REST API cho upload và download tệp đính kèm tin nhắn.
 *
 * <p>API Contract:
 * <ul>
 *   <li>POST /api/attachments/upload – upload file (multipart/form-data)</li>
 *   <li>GET  /api/attachments/{id}/download – download file</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    /**
     * Upload tệp đính kèm.
     *
     * <p>Request: {@code POST /api/attachments/upload}
     * Content-Type: multipart/form-data
     * Form field: file (required)
     *
     * <p>Response 201: {@link AttachmentResponseDTO}
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentResponseDTO upload(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {
        UUID uploaderId = UUID.fromString(jwt.getSubject());
        return attachmentService.upload(file, uploaderId);
    }

    /**
     * Download tệp đính kèm.
     *
     * <p>Request: {@code GET /api/attachments/{id}/download}
     * Header: Authorization: Bearer <token>
     *
     * <p>Response 200: file bytes với Content-Disposition và Content-Type đúng
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID requesterId = UUID.fromString(jwt.getSubject());
        Resource resource = attachmentService.download(id, requesterId);
        MessageAttachment meta = attachmentService.getAttachment(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(meta.getMimeType()));
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(meta.getOriginalName(), StandardCharsets.UTF_8)
                        .build());

        return ResponseEntity.ok().headers(headers).body(resource);
    }
}
