package com.eopis.payment.service;

import com.eopis.common.metrics.EopisMetricsService;
import com.eopis.payment.entity.Payment;
import com.eopis.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final EopisMetricsService metricsService;

    public PaymentServiceImpl(PaymentRepository paymentRepository, EopisMetricsService metricsService) {
        this.paymentRepository = paymentRepository;
        this.metricsService = metricsService;
    }

    @Override
    @Transactional
    public Payment processPayment(Long orderId, Long customerId, BigDecimal amount) {
        log.info("Initiating payment processing for Order ID: {}, Amount: ${}", orderId, amount);

        Payment payment = new Payment(
                "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                orderId,
                customerId,
                amount,
                "USD"
        );
        payment.setStatus("SUCCESS");
        payment.setGatewayReference("GW-REF-" + UUID.randomUUID().toString().substring(0, 12));

        Payment saved = paymentRepository.save(payment);
        log.info("Payment settled successfully: Payment #{}", saved.getPaymentNumber());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentByNumber(String paymentNumber) {
        return paymentRepository.findByPaymentNumber(paymentNumber)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found with number: " + paymentNumber));
    }
}
