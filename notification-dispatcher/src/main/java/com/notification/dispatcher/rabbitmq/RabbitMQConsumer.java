package com.notification.dispatcher.rabbitmq;

import com.notification.dispatcher.DTO.EventPayloadDTO;
import com.notification.dispatcher.sse.SseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumer lắng nghe các sự kiện từ RabbitMQ dispatch queue và đẩy qua SSE tới client.
 */
@Component
public class RabbitMQConsumer {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQConsumer.class);

    private final SseService sseService;

    public RabbitMQConsumer(SseService sseService) {
        this.sseService = sseService;
    }

    /**
     * Nhận sự kiện từ RabbitMQ queue và dispatch tới SSE Emitters tương ứng.
     */
    @RabbitListener(queues = RabbitMQConfig.DISPATCH_QUEUE)
    public void consumeEvent(EventPayloadDTO<?> event) {
        log.info("Received event from RabbitMQ: type={}, eventId={}, recipientId={}",
                event.getEventType(), event.getEventId(), event.getRecipientId());

        if (event.getRecipientId() != null) {
            try {
                UUID recipientUuid = UUID.fromString(event.getRecipientId());
                sseService.sendToUser(recipientUuid, event.getEventType(), event.getData());
            } catch (IllegalArgumentException e) {
                log.error("Recipient ID không phải UUID hợp lệ: {}", event.getRecipientId());
            }
        } else {
            // Không có recipient cụ thể -> broadcast tới toàn bộ client đang online
            sseService.broadcast(event.getEventType(), event.getData());
        }

        // Với tin nhắn mới, đồng bộ cả cho người gửi (nếu mở nhiều tab/thiết bị)
        if ("NEW_MESSAGE".equals(event.getEventType()) && event.getSenderId() != null) {
            try {
                UUID senderUuid = UUID.fromString(event.getSenderId());
                sseService.sendToUser(senderUuid, "MESSAGE_SENT", event.getData());
            } catch (IllegalArgumentException ignored) {
            }
        }
    }
}
