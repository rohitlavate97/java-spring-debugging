package com.eopis.payment.controller;

import com.eopis.payment.dto.PaymentRequestDto;
import com.eopis.payment.entity.Payment;
import com.eopis.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<Payment> processPayment(@Valid @RequestBody PaymentRequestDto request) {
        Payment payment = paymentService.processPayment(request.getOrderId(), request.getCustomerId(), request.getAmount());
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    @GetMapping("/{paymentNumber}")
    public ResponseEntity<Payment> getPaymentByNumber(@PathVariable String paymentNumber) {
        return ResponseEntity.ok(paymentService.getPaymentByNumber(paymentNumber));
    }
}
