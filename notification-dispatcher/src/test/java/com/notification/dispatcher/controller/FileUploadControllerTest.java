package com.notification.dispatcher.controller;

import com.notification.dispatcher.DTO.FileUploadResponseDTO;
import com.notification.dispatcher.exception.GlobalExceptionHandler;
import com.notification.dispatcher.service.FileUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FileUploadControllerTest {

    private FileUploadService fileUploadService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        fileUploadService = mock(FileUploadService.class);
        mvc = MockMvcBuilders.standaloneSetup(new FileUploadController(fileUploadService))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void uploadFile_Success_Returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", MediaType.IMAGE_PNG_VALUE, "fake image content".getBytes()
        );

        FileUploadResponseDTO responseDTO = FileUploadResponseDTO.builder()
                .url("https://res.cloudinary.com/demo/image/upload/sample.png")
                .publicId("sample_id")
                .originalFilename("test.png")
                .format("png")
                .bytes(17L)
                .resourceType("image")
                .build();

        when(fileUploadService.upload(any())).thenReturn(responseDTO);

        mvc.perform(multipart("/api/files/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("https://res.cloudinary.com/demo/image/upload/sample.png"))
                .andExpect(jsonPath("$.publicId").value("sample_id"))
                .andExpect(jsonPath("$.format").value("png"));

        verify(fileUploadService).upload(any());
    }

    @Test
    void uploadFile_EmptyFile_Returns400() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.png", MediaType.IMAGE_PNG_VALUE, new byte[0]
        );

        when(fileUploadService.upload(any()))
                .thenThrow(new IllegalArgumentException("File tải lên không được để trống"));

        mvc.perform(multipart("/api/files/upload").file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));
    }

    @Test
    void deleteFile_Success_Returns200() throws Exception {
        when(fileUploadService.deleteFile("sample_id")).thenReturn(true);

        mvc.perform(delete("/api/files").param("publicId", "sample_id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xóa file thành công"))
                .andExpect(jsonPath("$.publicId").value("sample_id"));
    }

    @Test
    void deleteFile_NotFound_Returns400() throws Exception {
        when(fileUploadService.deleteFile("non_existent")).thenReturn(false);

        mvc.perform(delete("/api/files").param("publicId", "non_existent"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Xóa file thất bại hoặc public_id không tồn tại"));
    }
}
