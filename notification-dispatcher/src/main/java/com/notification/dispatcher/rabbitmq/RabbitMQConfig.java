package com.notification.dispatcher.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình RabbitMQ Message Broker.
 *
 * <p>Kiến trúc Exchange & Queues:
 * <ul>
 *   <li><b>TopicExchange:</b> {@code notification.exchange} làm trung tâm phân phối</li>
 *   <li><b>Queue:</b> {@code notification.dispatch.queue} nhận các sự kiện để dispatch tới SSE</li>
 *   <li><b>Routing Keys:</b>
 *     <ul>
 *       <li>{@code notification.#} – các thông báo hệ thống và người dùng</li>
 *       <li>{@code chat.#} – các sự kiện tin nhắn, typing, read receipt</li>
 *     </ul>
 *   </li>
 * </ul>
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "notification.exchange";
    public static final String DISPATCH_QUEUE = "notification.dispatch.queue";
    public static final String ROUTING_KEY_NOTIFICATION = "notification.#";
    public static final String ROUTING_KEY_CHAT = "chat.#";

    @Bean
    public TopicExchange notificationExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_NAME)
                .durable(true)
                .build();
    }

    @Bean
    public Queue dispatchQueue() {
        return QueueBuilder.durable(DISPATCH_QUEUE)
                .build();
    }

    @Bean
    public Binding notificationBinding(Queue dispatchQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(dispatchQueue)
                .to(notificationExchange)
                .with(ROUTING_KEY_NOTIFICATION);
    }

    @Bean
    public Binding chatBinding(Queue dispatchQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(dispatchQueue)
                .to(notificationExchange)
                .with(ROUTING_KEY_CHAT);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jacksonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jacksonMessageConverter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jacksonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jacksonMessageConverter);
        return factory;
    }
}
