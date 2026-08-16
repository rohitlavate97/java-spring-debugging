package com.eopis.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class EopisMetricsService {

    private final Counter ordersPlacedCounter;
    private final Counter inventoryReservationsCounter;
    private final Counter chaosFaultsInjectedCounter;

    public EopisMetricsService(MeterRegistry meterRegistry) {
        this.ordersPlacedCounter = Counter.builder("eopis.orders.placed.total")
                .description("Total number of orders successfully placed")
                .register(meterRegistry);

        this.inventoryReservationsCounter = Counter.builder("eopis.inventory.reservations.total")
                .description("Total number of inventory lines reserved")
                .register(meterRegistry);

        this.chaosFaultsInjectedCounter = Counter.builder("eopis.chaos.faults.injected.total")
                .description("Total number of chaos faults injected by AOP aspect")
                .register(meterRegistry);
    }

    public void incrementOrdersPlaced() {
        ordersPlacedCounter.increment();
    }

    public void incrementInventoryReservations(int count) {
        inventoryReservationsCounter.increment(count);
    }

    public void incrementChaosFaults() {
        chaosFaultsInjectedCounter.increment();
    }
}
