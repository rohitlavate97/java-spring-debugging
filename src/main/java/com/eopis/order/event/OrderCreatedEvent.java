package com.eopis.order.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class OrderCreatedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;
    private Long orderId;
    private String orderNumber;
    private Long customerId;
    private String customerNumber;
    private BigDecimal totalAmount;
    private OffsetDateTime timestamp;

    public OrderCreatedEvent() {
    }

    public OrderCreatedEvent(Long orderId, String orderNumber, Long customerId, String customerNumber, BigDecimal totalAmount) {
        this.eventId = UUID.randomUUID().toString();
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.customerId = customerId;
        this.customerNumber = customerNumber;
        this.totalAmount = totalAmount;
        this.timestamp = OffsetDateTime.now();
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getCustomerNumber() { return customerNumber; }
    public void setCustomerNumber(String customerNumber) { this.customerNumber = customerNumber; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public OffsetDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }
}
