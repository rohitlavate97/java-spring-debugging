package com.eopis.notification.repository;

import com.eopis.notification.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {
    List<NotificationLog> findByRecipient(String recipient);
    List<NotificationLog> findByCorrelationId(String correlationId);
}
