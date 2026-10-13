package com.notification.dispatcher.service;

import com.notification.dispatcher.DTO.CreateUserRequestDTO;
import com.notification.dispatcher.entity.TaiKhoan;
import com.notification.dispatcher.exception.DuplicateAccountException;
import com.notification.dispatcher.repository.TaiKhoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private TaiKhoanRepository repository;
    private BCryptPasswordEncoder encoder;
    private AuthService service;

    @BeforeEach
    void setUp() {
        repository = mock(TaiKhoanRepository.class);
        encoder = new BCryptPasswordEncoder();
        service = new AuthService(repository, encoder);
    }

    private CreateUserRequestDTO request() {
        return new CreateUserRequestDTO(" User A ", " user@example.com ", " user_a ", " secret123 ");
    }

    @Test
    void registerStoresHashAndReturnsSavedIdentity() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-09T00:00:00Z");
        when(repository.saveAndFlush(any(TaiKhoan.class))).thenAnswer(invocation -> {
            TaiKhoan user = invocation.getArgument(0);
            assertNotEquals(request().getPassword(), user.getPasswordHash());
            assertTrue(encoder.matches(request().getPassword(), user.getPasswordHash()));
            assertFalse(encoder.matches("secret123", user.getPasswordHash()));
            user.setId(id);
            user.setCreatedAt(createdAt);
            return user;
        });
        var response = service.register(request());
        assertEquals(id.toString(), response.getId());
        assertEquals(createdAt, response.getCreatedAt());
        assertEquals("User A", response.getTenTaiKhoan());
        assertEquals("user_a", response.getUserName());
        assertEquals("user@example.com", response.getEmail());
    }

    @Test
    void duplicateUsernameDoesNotSave() {
        when(repository.existsByUserName("user_a")).thenReturn(true);
        assertThrows(DuplicateAccountException.class, () -> service.register(request()));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void duplicateEmailDoesNotSave() {
        when(repository.existsByEmail("user@example.com")).thenReturn(true);
        assertThrows(DuplicateAccountException.class, () -> service.register(request()));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void concurrentUniqueConflictBecomesDuplicateAccount() {
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException(
                "unique violation", new SQLException("duplicate", "23505")));
        assertThrows(DuplicateAccountException.class, () -> service.register(request()));
    }

    @Test
    void otherDatabaseErrorsAreNotReportedAsDuplicates() {
        var failure = new DataIntegrityViolationException(
                "foreign key violation", new SQLException("invalid reference", "23503"));
        when(repository.saveAndFlush(any())).thenThrow(failure);
        assertSame(failure, assertThrows(DataIntegrityViolationException.class, () -> service.register(request())));
    }

    @Test
    void passwordLimitUsesUtf8Bytes() {
        var request = request();
        request.setPassword("é".repeat(37));
        assertThrows(IllegalArgumentException.class, () -> service.register(request));
        verify(repository, never()).saveAndFlush(any());
    }
}
