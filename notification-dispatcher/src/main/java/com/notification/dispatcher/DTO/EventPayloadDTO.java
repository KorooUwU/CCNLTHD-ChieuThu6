package com.notification.dispatcher.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Envelope DTO chứa payload của sự kiện truyền tải qua RabbitMQ và dispatch tới SSE client.
 *
 * @param <T> kiểu dữ liệu payload cụ thể (MessageResponseDTO, NotificationResponseDTO, TypingEventDTO,...)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventPayloadDTO<T> implements Serializable {

    private String eventId;
    /** Loại sự kiện: NEW_MESSAGE, NOTIFICATION, TYPING, MESSAGE_READ, PRESENCE,... */
    private String eventType;
    /** UUID người nhận (dạng string), null nếu là broadcast cho toàn bộ */
    private String recipientId;
    /** UUID người gửi (dạng string) */
    private String senderId;
    /** ID cuộc trò chuyện liên quan (nếu có) */
    private String conversationId;
    /** Thời điểm phát sinh sự kiện */
    private LocalDateTime timestamp;
    /** Dữ liệu chi tiết của sự kiện */
    private T data;
}
