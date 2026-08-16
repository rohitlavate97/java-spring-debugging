package com.eopis.notification.consumer;

import com.eopis.notification.entity.NotificationLog;
import com.eopis.notification.repository.NotificationLogRepository;
import com.eopis.order.event.OrderCreatedEvent;
import com.eopis.order.service.OrderEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationKafkaConsumer.class);
    private final NotificationLogRepository notificationLogRepository;

    public NotificationKafkaConsumer(NotificationLogRepository notificationLogRepository) {
        this.notificationLogRepository = notificationLogRepository;
    }

    @KafkaListener(
        topics = OrderEventPublisher.ORDER_CREATED_TOPIC,
        groupId = "${spring.kafka.consumer.group-id:eopis-notification-group}",
        autoStartup = "${eopis.kafka.listener.auto-startup:false}"
    )
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent via Kafka: Order #{} for Customer #{}",
                event.getOrderNumber(), event.getCustomerNumber());

        NotificationLog notif = new NotificationLog(
                event.getCustomerNumber(),
                "EMAIL",
                "ORDER_CONFIRMATION_EMAIL",
                "SENT",
                event.getEventId()
        );

        notificationLogRepository.save(notif);
        log.info("Saved notification log for Order #{}", event.getOrderNumber());
    }
}
