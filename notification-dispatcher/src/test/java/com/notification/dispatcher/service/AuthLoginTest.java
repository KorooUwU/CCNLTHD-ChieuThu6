package com.notification.dispatcher.service;

import com.notification.dispatcher.DTO.*;
import com.notification.dispatcher.entity.TaiKhoan;
import com.notification.dispatcher.repository.TaiKhoanRepository;
import com.notification.dispatcher.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Optional;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class AuthLoginTest {
    @Test
    void correctPasswordIssuesTokenForSavedUuid() {
        var repo = mock(TaiKhoanRepository.class);
        var jwt = mock(JwtService.class);
        var encoder = new BCryptPasswordEncoder();
        var user = new TaiKhoan("A", "a@example.com", "user_a", encoder.encode("secret123"));
        user.setId(UUID.randomUUID());
        when(repo.findByUserName("user_a")).thenReturn(Optional.of(user));
        when(jwt.issue(user.getId())).thenReturn(new TokenResponseDTO("token", "Bearer", 900));
        assertEquals("token", new AuthService(repo, encoder, jwt)
                .login(new LoginRequestDTO(" user_a ", "secret123")).accessToken());
        verify(jwt).issue(user.getId());
    }

    @Test
    void unknownUserAndWrongPasswordHaveSameErrorAndNoToken() {
        var repo = mock(TaiKhoanRepository.class);
        var jwt = mock(JwtService.class);
        var encoder = new BCryptPasswordEncoder();
        var service = new AuthService(repo, encoder, jwt);
        var missing = assertThrows(BadCredentialsException.class,
                () -> service.login(new LoginRequestDTO("unknown", "wrongpass")));
        when(repo.findByUserName("user_a")).thenReturn(Optional.of(
                new TaiKhoan("A", "a@example.com", "user_a", encoder.encode("secret123"))));
        var wrong = assertThrows(BadCredentialsException.class,
                () -> service.login(new LoginRequestDTO("user_a", "wrongpass")));
        assertEquals(missing.getMessage(), wrong.getMessage());
        verifyNoInteractions(jwt);
    }
}
