package com.notification.dispatcher.controller;

import com.notification.dispatcher.DTO.CreateUserRequestDTO;
import com.notification.dispatcher.DTO.UserResponseDTO;
import com.notification.dispatcher.service.AuthService;
import com.notification.dispatcher.exception.DuplicateAccountException;
import com.notification.dispatcher.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Standalone MVC tests verify routing, validation and JSON, not security or database.
class AuthControllerTest {
    private AuthService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(AuthService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void validRegistrationReturnsCreatedWithoutPassword() throws Exception {
        when(service.register(any())).thenReturn(new UserResponseDTO(
                "11111111-1111-1111-1111-111111111111", "User A",
                "user_a@example.com", "user_a", Instant.parse("2026-10-13T00:00:00Z")));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenTaiKhoan":"User A","email":"user_a@example.com",
                                 "userName":"user_a","password":"secret123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userName").value("user_a"))
                .andExpect(jsonPath("$.id").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(service).register(argThat(request ->
                "user_a".equals(request.getUserName()) && "secret123".equals(request.getPassword())));
    }

    @Test
    void invalidFieldsReturnBadRequestWithoutCallingService() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenTaiKhoan":" ","email":"invalid",
                                 "userName":"a","password":"123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details").value(org.hamcrest.Matchers.containsString("password:")))
                .andExpect(jsonPath("$.details").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("123"))))
                .andExpect(jsonPath("$.timestamp").exists());
        verifyNoInteractions(service);
    }

    @Test
    void missingBodyReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST_BODY"));
        verifyNoInteractions(service);
    }

    @Test
    void duplicateAccountReturnsConflict() throws Exception {
        when(service.register(any())).thenThrow(new DuplicateAccountException("Username đã được sử dụng"));
        validRegistrationRequest()
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_ACCOUNT"));
    }

    @Test
    void invalidArgumentReturnsSafeBadRequest() throws Exception {
        when(service.register(any())).thenThrow(new IllegalArgumentException("private input"));
        validRegistrationRequest()
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private input"))));
    }

    @Test
    void unexpectedFailureDoesNotExposeInternalError() throws Exception {
        when(service.register(any())).thenThrow(new IllegalStateException("private database detail"));
        validRegistrationRequest()
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private database detail"))));
    }

    @Test
    void unsupportedMethodRemainsMethodNotAllowed() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/register"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", org.hamcrest.Matchers.containsString("POST")))
                .andExpect(jsonPath("$.errorCode").value("HTTP_ERROR"));
    }

    private org.springframework.test.web.servlet.ResultActions validRegistrationRequest() throws Exception {
        return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"tenTaiKhoan":"User A","email":"user_a@example.com",
                         "userName":"user_a","password":"secret123"}
                        """));
    }
}
