package com.notification.dispatcher.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.notification.dispatcher.DTO.FileUploadResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final Cloudinary cloudinary;

    /**
     * Upload file lên Cloudinary và trả về chi tiết thông tin file (DTO)
     */
    public FileUploadResponseDTO upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File tải lên không được để trống");
        }

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String publicId = UUID.randomUUID().toString();

        Map<?, ?> uploadParams = ObjectUtils.asMap(
                "folder", "chatboxspringboot",
                "public_id", publicId,
                "resource_type", "auto"
        );

        @SuppressWarnings("rawtypes")
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);

        String secureUrl = uploadResult.get("secure_url") != null ? uploadResult.get("secure_url").toString() : "";
        String returnedPublicId = uploadResult.get("public_id") != null ? uploadResult.get("public_id").toString() : publicId;
        String format = uploadResult.get("format") != null ? uploadResult.get("format").toString() : "";
        String resourceType = uploadResult.get("resource_type") != null ? uploadResult.get("resource_type").toString() : "auto";
        Long bytes = uploadResult.get("bytes") instanceof Number ? ((Number) uploadResult.get("bytes")).longValue() : file.getSize();

        return FileUploadResponseDTO.builder()
                .url(secureUrl)
                .publicId(returnedPublicId)
                .originalFilename(originalFilename)
                .format(format)
                .bytes(bytes)
                .resourceType(resourceType)
                .build();
    }

    /**
     * Upload file và chỉ trả về secure_url (phục vụ lưu nhanh vào CSDL)
     */
    public String uploadFile(MultipartFile file) throws IOException {
        FileUploadResponseDTO response = upload(file);
        return response.getUrl();
    }

    /**
     * Xóa file trên Cloudinary bằng public_id (khi xóa hoặc thu hồi tin nhắn)
     */
    public boolean deleteFile(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return false;
        }
        try {
            @SuppressWarnings("rawtypes")
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            return "ok".equalsIgnoreCase(String.valueOf(result.get("result")));
        } catch (Exception e) {
            log.error("Lỗi khi xóa file trên Cloudinary với public_id: {}", publicId, e);
            return false;
        }
    }
}
