package com.notification.dispatcher.service;

import com.notification.dispatcher.DTO.UpdateProfileRequestDTO;
import com.notification.dispatcher.entity.TaiKhoan;
import com.notification.dispatcher.repository.TaiKhoanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
@Transactional
class UserServiceIntegrationTest {
    @Autowired UserService service;
    @Autowired TaiKhoanRepository repository;
    @Autowired EntityManager entityManager;

    private TaiKhoan account(String name, String username) {
        return repository.saveAndFlush(new TaiKhoan(name, username + "@example.test", username, "test-only-hash"));
    }

    @Test
    void updatePersistsOnlyOwnersNameAndPreservesOtherAccount() {
        String prefix = "p" + UUID.randomUUID().toString().replace("-", "");
        var a = account("A", prefix + "a");
        var b = account("B", prefix + "b");
        service.updateProfile(a.getId(), new UpdateProfileRequestDTO("  Updated A  "));
        entityManager.flush();
        entityManager.clear();
        var saved = repository.findById(a.getId()).orElseThrow();
        assertEquals("Updated A", saved.getTenTaiKhoan());
        assertEquals(a.getEmail(), saved.getEmail());
        assertEquals(a.getUserName(), saved.getUserName());
        assertEquals(a.getPasswordHash(), saved.getPasswordHash());
        assertEquals("B", repository.findById(b.getId()).orElseThrow().getTenTaiKhoan());
    }

    @Test
    void searchIsCaseInsensitivePaginatedAndTreatsWildcardsLiterally() {
        String prefix = "p" + UUID.randomUUID().toString().replace("-", "");
        account(prefix + " Person", prefix + "a");
        account("Other", prefix + "b");
        account("Literal " + prefix + "%_", "x" + prefix);
        var first = service.search(prefix.toUpperCase(), 0, 1);
        var second = service.search(prefix, 1, 1);
        assertEquals(3, first.totalElements());
        assertEquals(3, first.totalPages());
        assertEquals(1, first.content().size());
        assertNotEquals(first.content().getFirst().getId(), second.content().getFirst().getId());
        assertEquals(1, service.search(prefix + "%_", 0, 20).totalElements());
        assertTrue(service.search(prefix, 10, 20).content().isEmpty());
    }

    @Test
    void invalidSearchAndMissingOwnerAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.search(" ", 0, 20));
        assertThrows(IllegalArgumentException.class, () -> service.search("a", -1, 20));
        assertThrows(IllegalArgumentException.class, () -> service.search("a", 0, 51));
        assertThrows(BadCredentialsException.class, () -> service.updateProfile(
                UUID.randomUUID(), new UpdateProfileRequestDTO("Name")));
    }
}
