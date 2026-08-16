package com.eopis.order.service;

import com.eopis.order.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);
    public static final String ORDER_CREATED_TOPIC = "order-created-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventPublisher(@Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderCreated(OrderCreatedEvent event) {
        if (kafkaTemplate != null) {
            try {
                log.info("Publishing OrderCreatedEvent to Kafka topic '{}': Order #{}",
                        ORDER_CREATED_TOPIC, event.getOrderNumber());
                kafkaTemplate.send(ORDER_CREATED_TOPIC, event.getOrderNumber(), event);
            } catch (Exception e) {
                log.warn("Failed to publish OrderCreatedEvent to Kafka (will proceed): {}", e.getMessage());
            }
        } else {
            log.info("KafkaTemplate not configured; simulated event published for Order #{}", event.getOrderNumber());
        }
    }
}
