package com.notification.dispatcher.rabbitmq;

import com.notification.dispatcher.DTO.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Producer gửi các sự kiện nghiệp vụ (tin nhắn, thông báo, typing, presence) vào RabbitMQ.
 */
@Service
public class RabbitMQProducer {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitMQProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publish tin nhắn chat mới vào RabbitMQ exchange.
     */
    public void sendChatMessage(UUID recipientId, MessageResponseDTO message, UUID senderId, UUID conversationId) {
        EventPayloadDTO<MessageResponseDTO> event = EventPayloadDTO.<MessageResponseDTO>builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("NEW_MESSAGE")
                .recipientId(recipientId != null ? recipientId.toString() : null)
                .senderId(senderId != null ? senderId.toString() : null)
                .conversationId(conversationId != null ? conversationId.toString() : null)
                .timestamp(LocalDateTime.now())
                .data(message)
                .build();

        String routingKey = "chat.message." + conversationId;
        publish(routingKey, event);
    }

    /**
     * Publish thông báo (Notification) vào RabbitMQ exchange.
     */
    public void sendNotification(UUID recipientId, NotificationResponseDTO notification, UUID senderId) {
        EventPayloadDTO<NotificationResponseDTO> event = EventPayloadDTO.<NotificationResponseDTO>builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("NOTIFICATION")
                .recipientId(recipientId.toString())
                .senderId(senderId != null ? senderId.toString() : null)
                .conversationId(notification.getConversationId())
                .timestamp(LocalDateTime.now())
                .data(notification)
                .build();

        String routingKey = "notification.user." + recipientId;
        publish(routingKey, event);
    }

    /**
     * Publish sự kiện đối phương đang soạn tin nhắn vào RabbitMQ exchange.
     */
    public void sendTyping(TypingEventDTO typingEvent, UUID recipientId) {
        EventPayloadDTO<TypingEventDTO> event = EventPayloadDTO.<TypingEventDTO>builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("TYPING")
                .recipientId(recipientId != null ? recipientId.toString() : null)
                .senderId(typingEvent.getSenderId())
                .conversationId(typingEvent.getConversationId())
                .timestamp(LocalDateTime.now())
                .data(typingEvent)
                .build();

        String routingKey = "chat.typing." + typingEvent.getConversationId();
        publish(routingKey, event);
    }

    /**
     * Publish sự kiện đã đọc tin nhắn vào RabbitMQ exchange.
     */
    public void sendMessageRead(MessageReadEventDTO readEvent, UUID recipientId) {
        EventPayloadDTO<MessageReadEventDTO> event = EventPayloadDTO.<MessageReadEventDTO>builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("MESSAGE_READ")
                .recipientId(recipientId != null ? recipientId.toString() : null)
                .senderId(readEvent.getReaderId())
                .conversationId(readEvent.getConversationId())
                .timestamp(LocalDateTime.now())
                .data(readEvent)
                .build();

        String routingKey = "chat.read." + readEvent.getConversationId();
        publish(routingKey, event);
    }

    /**
     * Publish trạng thái online/offline vào RabbitMQ exchange (broadcast).
     */
    public void sendPresence(UserPresenceDTO presence) {
        EventPayloadDTO<UserPresenceDTO> event = EventPayloadDTO.<UserPresenceDTO>builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("PRESENCE")
                .recipientId(null) // broadcast
                .senderId(presence.getUserId())
                .timestamp(LocalDateTime.now())
                .data(presence)
                .build();

        String routingKey = "chat.presence";
        publish(routingKey, event);
    }

    /**
     * Publish sự kiện cập nhật danh sách chat (inbox) vào RabbitMQ.
     * Được gửi đến cả 2 phía (sender và recipient) để đồng bộ inbox realtime.
     *
     * @param recipientId  UUID người nhận event này
     * @param conversation thông tin cuộc trò chuyện đã cập nhật
     */
    public void sendConversationUpdated(UUID recipientId, ConversationUpdatedEventDTO conversation) {
        EventPayloadDTO<ConversationUpdatedEventDTO> event = EventPayloadDTO.<ConversationUpdatedEventDTO>builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("CONVERSATION_UPDATED")
                .recipientId(recipientId.toString())
                .timestamp(LocalDateTime.now())
                .data(conversation)
                .build();

        String routingKey = "notification.conversation." + recipientId;
        publish(routingKey, event);
    }

    private void publish(String routingKey, EventPayloadDTO<?> event) {
        log.debug("Publishing to RabbitMQ [{}]: eventType={}, recipientId={}",
                routingKey, event.getEventType(), event.getRecipientId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, routingKey, event);
    }
}
