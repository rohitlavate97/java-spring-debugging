package com.eopis.observability;

import com.eopis.common.metrics.EopisMetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MetricsAndLoggingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private EopisMetricsService metricsService;

    @Test
    @DisplayName("Verify custom Micrometer metrics increment properly")
    void shouldIncrementCustomMetrics() {
        double initialOrders = meterRegistry.get("eopis.orders.placed.total").counter().count();
        double initialReservations = meterRegistry.get("eopis.inventory.reservations.total").counter().count();

        metricsService.incrementOrdersPlaced();
        metricsService.incrementInventoryReservations(3);

        assertEquals(initialOrders + 1.0, meterRegistry.get("eopis.orders.placed.total").counter().count());
        assertEquals(initialReservations + 3.0, meterRegistry.get("eopis.inventory.reservations.total").counter().count());
    }

    @Test
    @DisplayName("Verify CorrelationIdFilter injects X-Correlation-ID and X-Request-ID into HTTP responses")
    void shouldPropagateCorrelationIdHeader() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .header("X-Correlation-ID", "custom-test-correlation-id-12345"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-ID", "custom-test-correlation-id-12345"))
                .andExpect(header().exists("X-Request-ID"));
    }
}
