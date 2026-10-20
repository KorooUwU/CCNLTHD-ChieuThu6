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

@WebMvcTest(controllers = {AuthController.class, com.notification.dispatcher.controller.UserController.class},
        properties = {"app.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", "debug=false"})
@Import({SecurityConfig.class, JwtConfig.class, com.notification.dispatcher.security.JwtService.class})
class SecurityConfigTest {
    @Autowired
    private com.notification.dispatcher.security.JwtService tokens;
    @Autowired
    private org.springframework.security.oauth2.jwt.JwtEncoder encoder;
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthService service;

    @MockitoBean
    private com.notification.dispatcher.service.UserService userService;

    @Test
    void profileAndSearchRequireAuthentication() throws Exception {
        mvc.perform(patch("/api/users/me").contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenTaiKhoan\":\"New name\"}")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/search").param("keyword", "user")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/users/me").header("Authorization", "Bearer broken")
                .contentType(MediaType.APPLICATION_JSON).content("{\"tenTaiKhoan\":\"A\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(userService);
    }

    @Test
    void updateUsesTokenIdentityAndOnlyDisplayName() throws Exception {
        var id = java.util.UUID.randomUUID();
        when(userService.updateProfile(eq(id), any())).thenReturn(new UserResponseDTO(
                id.toString(), "New name", "a@example.com", "user_a", Instant.now()));
        mvc.perform(patch("/api/users/me").param("userId", java.util.UUID.randomUUID().toString())
                .header("Authorization", "Bearer " + tokens.issue(id).accessToken())
                .contentType(MediaType.APPLICATION_JSON).content("{\"tenTaiKhoan\":\"New name\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        verify(userService).updateProfile(eq(id), argThat(r -> r.tenTaiKhoan().equals("New name")));
    }

    @Test
    void blankProfileIsRejected() throws Exception {
        mvc.perform(patch("/api/users/me").header("Authorization", "Bearer " + tokens.issue(java.util.UUID.randomUUID()).accessToken())
                .contentType(MediaType.APPLICATION_JSON).content("{\"tenTaiKhoan\":\"   \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(userService);
    }

    @Test
    void searchReturnsOnlyPublicFields() throws Exception {
        var summary = new com.notification.dispatcher.DTO.UserSummaryDTO("id", "Name", "user");
        when(userService.search("user", 0, 20)).thenReturn(new com.notification.dispatcher.DTO.UserSearchResponseDTO(
                java.util.List.of(summary), 0, 20, 1, 1));
        mvc.perform(get("/api/users/search").param("keyword", "user")
                .header("Authorization", "Bearer " + tokens.issue(java.util.UUID.randomUUID()).accessToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].userName").value("user"))
                .andExpect(jsonPath("$.content[0].email").doesNotExist())
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
    }

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
    void protectedWriteWithoutTokenReturns401() throws Exception {
        mvc.perform(post("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
        verifyNoInteractions(service);
    }

    @Test
    void validTokenUsesItsOwnIdentityIgnoringQueryUserId() throws Exception {
        var id = java.util.UUID.randomUUID();
        when(service.me(id)).thenReturn(new UserResponseDTO(id.toString(), "A", "a@example.com", "user_a", Instant.now()));
        mvc.perform(get("/api/users/me").param("userId", java.util.UUID.randomUUID().toString())
                        .header("Authorization", "Bearer " + tokens.issue(id).accessToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        verify(service).me(id);
    }

    @Test
    void malformedTokenReturns401() throws Exception {
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer broken"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
        verifyNoInteractions(service);
    }

    @Test
    void expiredTokenReturns401() throws Exception {
        var claims = org.springframework.security.oauth2.jwt.JwtClaimsSet.builder()
                .issuer("chat-system").subject(java.util.UUID.randomUUID().toString())
                .issuedAt(Instant.now().minusSeconds(120)).expiresAt(Instant.now().minusSeconds(60)).build();
        var header = org.springframework.security.oauth2.jwt.JwsHeader
                .with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build();
        var token = encoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(header, claims)).getTokenValue();
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void loginIsPublic() throws Exception {
        when(service.login(any())).thenReturn(new com.notification.dispatcher.DTO.TokenResponseDTO("test-token", "Bearer", 900));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"user_a\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void wrongCredentialsReturnJson401() throws Exception {
        when(service.login(any())).thenThrow(new org.springframework.security.authentication.BadCredentialsException("private"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"user_a\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void tokenSignedByDifferentKeyReturns401() throws Exception {
        var keyBytes = new byte[32];
        new java.security.SecureRandom().nextBytes(keyBytes);
        var other = new org.springframework.security.oauth2.jwt.NimbusJwtEncoder(
                new com.nimbusds.jose.jwk.source.ImmutableSecret<>(new javax.crypto.spec.SecretKeySpec(keyBytes, "HmacSHA256")));
        var token = signedToken(other, "chat-system");
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void wrongIssuerReturns401() throws Exception {
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + signedToken(encoder, "other-app")))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    private String signedToken(org.springframework.security.oauth2.jwt.JwtEncoder signer, String issuer) {
        var claims = org.springframework.security.oauth2.jwt.JwtClaimsSet.builder()
                .issuer(issuer).subject(java.util.UUID.randomUUID().toString())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(900)).build();
        return signer.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(
                        org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
