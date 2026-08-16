package com.eopis.payment.service;

import com.eopis.payment.entity.Payment;
import java.math.BigDecimal;

public interface PaymentService {
    Payment processPayment(Long orderId, Long customerId, BigDecimal amount);
    Payment getPaymentByNumber(String paymentNumber);
}
