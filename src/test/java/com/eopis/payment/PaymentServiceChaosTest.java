package com.eopis.payment;

import com.eopis.common.chaos.ChaosProperties;
import com.eopis.payment.entity.Payment;
import com.eopis.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentServiceChaosTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ChaosProperties chaosProperties;

    @Test
    @DisplayName("Verify normal payment execution without chaos flags")
    void shouldProcessPaymentNormally() {
        Payment payment = paymentService.processPayment(984321L, 18291L, new BigDecimal("1263.60"));

        assertNotNull(payment);
        assertNotNull(payment.getId());
        assertEquals("SUCCESS", payment.getStatus());
        assertTrue(payment.getPaymentNumber().startsWith("PAY-"));
    }

    @Test
    @DisplayName("Verify Chaos AOP fault injection can inject simulated failures on demand")
    void shouldInjectChaosFailureWhenEnabled() {
        try {
            // Enable Chaos
            chaosProperties.setEnabled(true);
            ChaosProperties.FaultConfig fault = new ChaosProperties.FaultConfig();
            fault.setEnabled(true);
            fault.setFailureRatePercent(100.0); // 100% failure for test
            chaosProperties.getFaults().put("payment-test-fault", fault);

            RuntimeException thrown = assertThrows(RuntimeException.class, () ->
                    paymentService.processPayment(984321L, 18291L, new BigDecimal("500.00")));
            
            assertTrue(thrown.getMessage().contains("Simulated Chaos Failure"));
        } finally {
            // Reset Chaos state
            chaosProperties.setEnabled(false);
            chaosProperties.getFaults().clear();
        }
    }
}
