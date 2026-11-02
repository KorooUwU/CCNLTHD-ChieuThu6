package com.notification.dispatcher.service;

import com.notification.dispatcher.DTO.AttachmentResponseDTO;
import com.notification.dispatcher.entity.MessageAttachment;
import com.notification.dispatcher.entity.TaiKhoan;
import com.notification.dispatcher.repository.AttachmentRepository;
import com.notification.dispatcher.repository.TaiKhoanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

/**
 * Xử lý nghiệp vụ upload và download tệp đính kèm.
 *
 * <p>Validation thực hiện theo thứ tự:
 * <ol>
 *   <li>File không được null/rỗng</li>
 *   <li>Kích thước ≤ 10 MB</li>
 *   <li>MIME type thuộc danh sách cho phép</li>
 *   <li>Phần mở rộng khớp với MIME type</li>
 * </ol>
 *
 * <p>File lưu tại: {@code <uploadDir>/<uploaderId>/<uuid>.<ext>}
 */
@Service
public class AttachmentService {

    private static final Logger log = LoggerFactory.getLogger(AttachmentService.class);

    /** Giới hạn 10 MB */
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;

    /** MIME types được phép */
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            // Hình ảnh
            "image/jpeg", "image/png", "image/gif", "image/webp",
            // Tài liệu
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            // Text
            "text/plain"
    );

    private final AttachmentRepository attachmentRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final Path uploadRootPath;

    public AttachmentService(AttachmentRepository attachmentRepository,
                             TaiKhoanRepository taiKhoanRepository,
                             @Value("${app.upload.dir:uploads}") String uploadDir) {
        this.attachmentRepository = attachmentRepository;
        this.taiKhoanRepository = taiKhoanRepository;
        this.uploadRootPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadRootPath);
        } catch (IOException e) {
            throw new IllegalStateException("Không thể tạo thư mục upload: " + uploadDir, e);
        }
    }

    /**
     * Upload tệp đính kèm: validate → lưu file → lưu metadata vào DB.
     *
     * @param file       MultipartFile từ request
     * @param uploaderId UUID người upload
     * @return AttachmentResponseDTO metadata sau khi lưu thành công
     */
    @Transactional
    public AttachmentResponseDTO upload(MultipartFile file, UUID uploaderId) {
        // 1. Validate không rỗng
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }

        // 2. Validate kích thước
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File vượt quá giới hạn 10 MB (kích thước: " + file.getSize() + " bytes)");
        }

        // 3. Validate MIME type
        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Loại file không được hỗ trợ: " + mimeType + ". Cho phép: ảnh (jpg/png/gif/webp), PDF, Word, Excel, TXT");
        }

        // 4. Tên file an toàn: lấy extension từ tên gốc, ghép với UUID mới
        String originalName = sanitizeFilename(file.getOriginalFilename());
        String extension = getExtension(originalName);
        validateExtensionMatchesMime(extension, mimeType);

        String storedName = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);

        // 5. Tạo thư mục theo uploaderId để tránh quá nhiều file trong 1 folder
        Path userDir = uploadRootPath.resolve(uploaderId.toString());
        try {
            Files.createDirectories(userDir);
        } catch (IOException e) {
            throw new IllegalStateException("Không thể tạo thư mục upload cho user", e);
        }

        Path targetPath = userDir.resolve(storedName);

        // 6. Lưu file vào filesystem
        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("File uploaded: storedName={}, size={}, uploader={}", storedName, file.getSize(), uploaderId);
        } catch (IOException e) {
            throw new IllegalStateException("Không thể lưu file: " + e.getMessage(), e);
        }

        // 7. Lưu metadata vào DB
        TaiKhoan uploader = taiKhoanRepository.findById(uploaderId)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

        MessageAttachment attachment = MessageAttachment.builder()
                .uploader(uploader)
                .originalName(originalName)
                .storedName(storedName)
                .mimeType(mimeType)
                .fileSize(file.getSize())
                .storagePath(targetPath.toString())
                .build();

        attachment = attachmentRepository.save(attachment);

        return toDTO(attachment);
    }

    /**
     * Download tệp đính kèm: kiểm tra quyền → trả về Resource.
     *
     * @param attachmentId UUID của attachment
     * @param requesterId  UUID người yêu cầu (phải là uploader hoặc thành viên conversation)
     * @return Resource trỏ đến file trên disk
     */
    @Transactional(readOnly = true)
    public Resource download(UUID attachmentId, UUID requesterId) {
        MessageAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Tệp đính kèm không tồn tại"));

        // Authorization: chỉ uploader được download (đơn giản hóa cho 1-1 chat)
        // Mở rộng: check conversation membership nếu cần
        if (!attachment.getUploader().getId().equals(requesterId)) {
            // Cho phép thành viên cùng conversation download
            if (attachment.getMessage() != null) {
                UUID user1 = attachment.getMessage().getConversation().getUser1().getId();
                UUID user2 = attachment.getMessage().getConversation().getUser2().getId();
                if (!requesterId.equals(user1) && !requesterId.equals(user2)) {
                    throw new IllegalArgumentException("Bạn không có quyền tải tệp này");
                }
            } else {
                throw new IllegalArgumentException("Bạn không có quyền tải tệp này");
            }
        }

        try {
            Path filePath = Paths.get(attachment.getStoragePath());
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalArgumentException("File không tồn tại hoặc không đọc được");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Đường dẫn file không hợp lệ");
        }
    }

    /**
     * Lấy metadata của attachment (để lấy originalName, mimeType khi download).
     */
    @Transactional(readOnly = true)
    public MessageAttachment getAttachment(UUID attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Tệp đính kèm không tồn tại"));
    }

    // ─── Private helpers ────────────────────────────────────────────────────────

    private String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) return "file";
        // Loại bỏ path traversal và ký tự đặc biệt
        String safe = name.replaceAll("[^\\w.\\-]", "_");
        // Giới hạn độ dài
        return safe.length() > 200 ? safe.substring(0, 200) : safe;
    }

    private String getExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) return "";
        return filename.substring(idx + 1).toLowerCase();
    }

    private void validateExtensionMatchesMime(String ext, String mimeType) {
        boolean valid = switch (mimeType.toLowerCase()) {
            case "image/jpeg"    -> ext.equals("jpg") || ext.equals("jpeg");
            case "image/png"     -> ext.equals("png");
            case "image/gif"     -> ext.equals("gif");
            case "image/webp"    -> ext.equals("webp");
            case "application/pdf" -> ext.equals("pdf");
            case "application/msword" -> ext.equals("doc");
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ext.equals("docx");
            case "application/vnd.ms-excel" -> ext.equals("xls");
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> ext.equals("xlsx");
            case "text/plain"    -> ext.equals("txt");
            default -> false;
        };
        if (!valid) {
            throw new IllegalArgumentException(
                    "Phần mở rộng ." + ext + " không khớp với MIME type " + mimeType);
        }
    }

    private AttachmentResponseDTO toDTO(MessageAttachment a) {
        return AttachmentResponseDTO.builder()
                .attachmentId(a.getId().toString())
                .originalName(a.getOriginalName())
                .mimeType(a.getMimeType())
                .fileSize(a.getFileSize())
                .downloadUrl("/api/attachments/" + a.getId() + "/download")
                .uploadedAt(a.getUploadedAt())
                .build();
    }
}
