package com.eopis.notification.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_logs")
public class NotificationLog {

    @Id
    private UUID id;

    @Column(nullable = false, length = 255)
    private String recipient;

    @Column(nullable = false, length = 50)
    private String channel;

    @Column(nullable = false, length = 100)
    private String template;

    @Column(nullable = false, length = 50)
    private String status = "SENT";

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "sent_at", nullable = false)
    private OffsetDateTime sentAt;

    public NotificationLog() {
    }

    public NotificationLog(String recipient, String channel, String template, String status, String correlationId) {
        this.id = UUID.randomUUID();
        this.recipient = recipient;
        this.channel = channel;
        this.template = template;
        this.status = status != null ? status : "SENT";
        this.correlationId = correlationId;
        this.sentAt = OffsetDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.sentAt == null) {
            this.sentAt = OffsetDateTime.now();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public OffsetDateTime getSentAt() { return sentAt; }
    public void setSentAt(OffsetDateTime sentAt) { this.sentAt = sentAt; }
}
