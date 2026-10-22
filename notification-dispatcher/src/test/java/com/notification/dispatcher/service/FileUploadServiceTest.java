package com.notification.dispatcher.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.notification.dispatcher.DTO.FileUploadResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FileUploadServiceTest {

    private Cloudinary cloudinary;
    private Uploader uploader;
    private FileUploadService fileUploadService;

    @BeforeEach
    void setUp() {
        cloudinary = mock(Cloudinary.class);
        uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        fileUploadService = new FileUploadService(cloudinary);
    }

    @Test
    void upload_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", "fake image content".getBytes()
        );

        Map<String, Object> mockResult = Map.of(
                "secure_url", "https://res.cloudinary.com/demo/image/upload/sample.png",
                "public_id", "chatboxspringboot/sample",
                "format", "png",
                "resource_type", "image",
                "bytes", 1024L
        );

        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(mockResult);

        FileUploadResponseDTO response = fileUploadService.upload(file);

        assertNotNull(response);
        assertEquals("https://res.cloudinary.com/demo/image/upload/sample.png", response.getUrl());
        assertEquals("chatboxspringboot/sample", response.getPublicId());
        assertEquals("avatar.png", response.getOriginalFilename());
        assertEquals("png", response.getFormat());
        assertEquals(1024L, response.getBytes());
        assertEquals("image", response.getResourceType());
        verify(uploader, times(1)).upload(any(byte[].class), anyMap());
    }

    @Test
    void upload_EmptyFile_ThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.png", "image/png", new byte[0]
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> fileUploadService.upload(emptyFile)
        );

        assertEquals("File tải lên không được để trống", ex.getMessage());
        verifyNoInteractions(uploader);
    }

    @Test
    void upload_NullFile_ThrowsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> fileUploadService.upload(null)
        );

        assertEquals("File tải lên không được để trống", ex.getMessage());
        verifyNoInteractions(uploader);
    }

    @Test
    void uploadFile_HelperMethod_ReturnsSecureUrl() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "dummy pdf content".getBytes()
        );

        Map<String, Object> mockResult = Map.of(
                "secure_url", "https://res.cloudinary.com/demo/raw/upload/doc.pdf",
                "public_id", "chatboxspringboot/doc"
        );

        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(mockResult);

        String url = fileUploadService.uploadFile(file);

        assertEquals("https://res.cloudinary.com/demo/raw/upload/doc.pdf", url);
    }

    @Test
    void deleteFile_Success() throws Exception {
        when(uploader.destroy(eq("sample_public_id"), anyMap())).thenReturn(Map.of("result", "ok"));

        boolean result = fileUploadService.deleteFile("sample_public_id");

        assertTrue(result);
        verify(uploader, times(1)).destroy(eq("sample_public_id"), anyMap());
    }

    @Test
    void deleteFile_FailureOrNotFound() throws Exception {
        when(uploader.destroy(eq("invalid_id"), anyMap())).thenReturn(Map.of("result", "not found"));

        boolean result = fileUploadService.deleteFile("invalid_id");

        assertFalse(result);
    }

    @Test
    void deleteFile_BlankOrNullId_ReturnsFalse() {
        assertFalse(fileUploadService.deleteFile(null));
        assertFalse(fileUploadService.deleteFile("   "));
        verifyNoInteractions(uploader);
    }
}
