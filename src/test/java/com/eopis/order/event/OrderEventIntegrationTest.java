package com.eopis.order.event;

import com.eopis.notification.consumer.NotificationKafkaConsumer;
import com.eopis.notification.entity.NotificationLog;
import com.eopis.notification.repository.NotificationLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderEventIntegrationTest {

    @Autowired
    private NotificationKafkaConsumer notificationConsumer;

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Test
    @DisplayName("Verify OrderCreatedEvent consumption and NotificationLog creation")
    void shouldProcessOrderCreatedEvent() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                984321L,
                "ORD-984321",
                18291L,
                "CUST-18291",
                new BigDecimal("1263.60")
        );

        notificationConsumer.handleOrderCreated(event);

        List<NotificationLog> logs = notificationLogRepository.findByRecipient("CUST-18291");
        assertNotNull(logs);
        assertEquals(1, logs.size());
        assertEquals("ORDER_CONFIRMATION_EMAIL", logs.get(0).getTemplate());
        assertEquals("SENT", logs.get(0).getStatus());
        assertEquals(event.getEventId(), logs.get(0).getCorrelationId());
    }
}
