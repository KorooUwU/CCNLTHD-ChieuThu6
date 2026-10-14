package com.notification.dispatcher.security;

import com.notification.dispatcher.DTO.TokenResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final String issuer;
    private final long ttl;

    public JwtService(JwtEncoder encoder, @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.ttl-seconds}") long ttl) {
        if (ttl < 1 || ttl > 86400) throw new IllegalArgumentException("JWT TTL must be 1..86400 seconds");
        this.encoder = encoder;
        this.issuer = issuer;
        this.ttl = ttl;
    }

    public TokenResponseDTO issue(UUID userId) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer(issuer).subject(userId.toString())
                .issuedAt(now).expiresAt(now.plusSeconds(ttl)).id(UUID.randomUUID().toString()).build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new TokenResponseDTO(token, "Bearer", ttl);
    }
}
