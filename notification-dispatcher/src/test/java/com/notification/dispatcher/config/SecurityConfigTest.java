package com.notification.dispatcher.config;

import com.notification.dispatcher.controller.AuthController;
import com.notification.dispatcher.DTO.UserResponseDTO;
import com.notification.dispatcher.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthService service;

    @Test
    void registrationWorksWithoutAuthenticationOrCsrf() throws Exception {
        when(service.register(any())).thenReturn(new UserResponseDTO(
                "11111111-1111-1111-1111-111111111111", "User A",
                "user@example.com", "user_a", Instant.now()));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"tenTaiKhoan":"User A","email":"user@example.com",
                         "userName":"user_a","password":"secret123"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Set-Cookie"));
        verify(service).register(any());
    }

    @Test
    void protectedEndpointReturnsJson401() throws Exception {
        mvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(header().doesNotExist("Location"));
    }

    @Test
    void registrationIsPublicOnlyForPost() throws Exception {
        mvc.perform(get("/api/auth/register"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingCsrfOnOtherWriteEndpointsReturnsJson403() throws Exception {
        mvc.perform(post("/api/users/me").with(user("user_a")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        verifyNoInteractions(service);
    }
}
