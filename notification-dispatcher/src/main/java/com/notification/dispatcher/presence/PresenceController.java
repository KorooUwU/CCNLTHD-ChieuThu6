package com.notification.dispatcher.presence;

import com.notification.dispatcher.DTO.UserPresenceDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST API tra cứu trạng thái Online/Offline của người dùng.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET /api/presence/me} – trạng thái của chính mình</li>
 *   <li>{@code GET /api/presence/{userId}} – trạng thái của một user cụ thể</li>
 *   <li>{@code POST /api/presence/batch} – trạng thái của nhiều user (body: list of UUID strings)</li>
 *   <li>{@code GET /api/presence/online} – danh sách tất cả userId đang ONLINE</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/presence")
public class PresenceController {

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    /**
     * Lấy trạng thái presence của người dùng hiện tại (từ JWT).
     */
    @GetMapping("/me")
    public ResponseEntity<UserPresenceDTO> getMyPresence(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(presenceService.getPresence(userId));
    }

    /**
     * Lấy trạng thái presence của một người dùng cụ thể theo UUID.
     *
     * @param userId UUID người dùng cần truy vấn
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserPresenceDTO> getUserPresence(@PathVariable UUID userId) {
        return ResponseEntity.ok(presenceService.getPresence(userId));
    }

    /**
     * Lấy trạng thái presence của nhiều người dùng cùng lúc.
     * Body: danh sách UUID string, ví dụ: {@code ["uuid1", "uuid2", "uuid3"]}
     *
     * @param userIdStrings danh sách UUID dạng string
     * @return map userId string → UserPresenceDTO
     */
    @PostMapping("/batch")
    public ResponseEntity<Map<String, UserPresenceDTO>> getBatchPresence(
            @RequestBody List<String> userIdStrings) {

        List<UUID> userIds = userIdStrings.stream()
                .map(UUID::fromString)
                .collect(Collectors.toList());

        return ResponseEntity.ok(presenceService.getPresenceBatch(userIds));
    }

    /**
     * Lấy danh sách UUID (dạng string) của tất cả người dùng đang ONLINE.
     */
    @GetMapping("/online")
    public ResponseEntity<List<String>> getOnlineUsers() {
        Set<UUID> onlineIds = presenceService.getOnlineUserIds();
        List<String> result = onlineIds.stream()
                .map(UUID::toString)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
}
