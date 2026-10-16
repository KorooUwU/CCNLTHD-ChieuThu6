package com.notification.dispatcher.exception;

import com.notification.dispatcher.DTO.ErrorResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<Object> handleAuthentication(org.springframework.security.core.AuthenticationException exception) {
        return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Thông tin đăng nhập không hợp lệ", null);
    }
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DuplicateAccountException.class)
    public ResponseEntity<Object> handleDuplicateAccount(DuplicateAccountException exception) {
        return error(HttpStatus.CONFLICT, "DUPLICATE_ACCOUNT", exception.getMessage(), null);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        // Return field names and validation messages, never rejected values/passwords.
        String details = exception.getBindingResult().getFieldErrors().stream()
                .map(field -> field.getField() + ": " + field.getDefaultMessage())
                .sorted()
                .distinct()
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Dữ liệu không hợp lệ", details);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY",
                "Body bị thiếu hoặc JSON không hợp lệ", null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleInvalidArgument(IllegalArgumentException exception) {
        // Do not expose arbitrary exception messages containing internal/input data.
        return error(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", "Dữ liệu không hợp lệ", null);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ErrorResponseDTO response = new ErrorResponseDTO("HTTP_ERROR",
                "Yêu cầu không thể được xử lý", null, LocalDateTime.now(ZoneOffset.UTC));
        return new ResponseEntity<>(response, headers, status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpectedException(Exception exception) {
        log.error("Unexpected error processing API request", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Có lỗi nội bộ, vui lòng thử lại sau", null);
    }

    private ResponseEntity<Object> error(HttpStatus status, String code, String message, String details) {
        return ResponseEntity.status(status).body(new ErrorResponseDTO(
                code, message, details, LocalDateTime.now(ZoneOffset.UTC)));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Object> handleUserNotFound(UserNotFoundException exception) {
        return error(
                HttpStatus.NOT_FOUND,
                "USER_NOT_FOUND",
                "Không tìm thấy người dùng",
                null
        );
    }

    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<Object> handleConversationNotFound(
            ConversationNotFoundException exception) {

        return error(
                HttpStatus.NOT_FOUND,
                "CONVERSATION_NOT_FOUND",
                "Không tìm thấy cuộc hội thoại",
                null
        );
    }
}
