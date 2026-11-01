package com.notification.dispatcher.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.notification.dispatcher.DTO.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        var register = PathPatternRequestMatcher.withDefaults()
                .matcher(HttpMethod.POST, "/api/auth/register");
        var login = PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/auth/login");

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                // Authentication uses an explicit Bearer header, never browser cookies.
                .csrf(AbstractHttpConfigurer::disable)
                .oauth2ResourceServer(resource -> resource.jwt(org.springframework.security.config.Customizer.withDefaults())
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setHeader("WWW-Authenticate", "Bearer");
                            writeError(response, objectMapper, 401, "UNAUTHORIZED", "Token không hợp lệ hoặc đã hết hạn");
                        }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(register, login).permitAll()
                        // SSE endpoint: cho phép kết nối HTTP GET ban đầu (xác thực qua Bearer hoặc ?token= query param)
                        .requestMatchers("/api/notifications/stream", "/api/sse/**").permitAll()
                        .requestMatchers("/api/presence/**").authenticated()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setHeader("WWW-Authenticate", "Bearer");
                            writeError(response, objectMapper, 401, "UNAUTHORIZED", "Bạn cần đăng nhập");
                        })
                        .accessDeniedHandler((request, response, exception) -> writeError(
                                response, objectMapper, 403, "FORBIDDEN", "Bạn không có quyền thực hiện yêu cầu này")));

        return http.build();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        var configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOriginPatterns(java.util.List.of("http://localhost:*", "http://127.0.0.1:*"));
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.List.of("*"));
        configuration.setAllowCredentials(true);
        var source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void writeError(HttpServletResponse response, ObjectMapper objectMapper,
            int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        var body = new ErrorResponseDTO(code, message, null, LocalDateTime.now(ZoneOffset.UTC));
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    @Bean 
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
