package com.eopis.order.service;

import com.eopis.customer.entity.Customer;
import com.eopis.customer.repository.CustomerRepository;
import com.eopis.inventory.entity.Inventory;
import com.eopis.inventory.entity.Warehouse;
import com.eopis.inventory.repository.InventoryRepository;
import com.eopis.inventory.repository.InventoryReservationRepository;
import com.eopis.inventory.repository.WarehouseRepository;
import com.eopis.order.dto.CreateOrderRequest;
import com.eopis.order.dto.OrderItemRequest;
import com.eopis.product.entity.Category;
import com.eopis.product.entity.Product;
import com.eopis.product.repository.CategoryRepository;
import com.eopis.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ConcurrentOrderPlacementTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryReservationRepository reservationRepository;

    private Customer customer;
    private Product product;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        Category category = categoryRepository.findByCode("CAT-CONCURRENT")
                .orElseGet(() -> categoryRepository.save(new Category("Concurrent Cat", "CAT-CONCURRENT", "Desc")));

        product = productRepository.findBySku("PROD-CONCURRENT-1")
                .orElseGet(() -> {
                    Product p = new Product("PROD-CONCURRENT-1", "Concurrent Test Product", "Desc", new BigDecimal("100.00"), "USD");
                    p.setCategory(category);
                    return productRepository.save(p);
                });

        Warehouse warehouse = warehouseRepository.findByCode("WH-CONCURRENT")
                .orElseGet(() -> warehouseRepository.save(new Warehouse("WH-CONCURRENT", "Concurrent Warehouse", "Address")));

        inventory = inventoryRepository.findByWarehouseIdAndProductId(warehouse.getId(), product.getId())
                .orElseGet(() -> inventoryRepository.save(new Inventory(warehouse, product, 10, 0)));
        
        // Reset inventory stock to 10
        inventory.setQuantityAvailable(10);
        inventory.setQuantityAllocated(0);
        inventoryRepository.save(inventory);

        customer = customerRepository.findByCustomerNumber("CUST-CONCURRENT")
                .orElseGet(() -> customerRepository.save(new Customer("CUST-CONCURRENT", "Concurrent", "User", "123", "REGULAR")));
    }

    @Test
    @DisplayName("Verify thread safety: 10 concurrent threads placing orders for 10 available items")
    void shouldHandleConcurrentOrdersWithoutOverselling() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successfulOrders = new AtomicInteger(0);
        AtomicInteger failedOrders = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // wait for simultaneous start trigger
                    CreateOrderRequest request = new CreateOrderRequest(
                            customer.getId(),
                            List.of(new OrderItemRequest(product.getId(), 2)), // each wants 2 items (total demand = 20, supply = 10)
                            null
                    );
                    orderService.createOrder(request);
                    successfulOrders.incrementAndGet();
                } catch (Exception e) {
                    failedOrders.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startLatch.countDown();
        finishLatch.await();
        executor.shutdown();

        // Check inventory consistency
        Inventory finalInventory = inventoryRepository.findById(inventory.getId()).orElseThrow();

        // Supply was 10, each order took 2. At most 5 orders could have succeeded.
        assertTrue(successfulOrders.get() <= 5, "Successful orders must not exceed available stock capacity");
        assertEquals(10, successfulOrders.get() + failedOrders.get(), "All threads must complete");
        assertTrue(finalInventory.getQuantityAvailable() >= 0, "Available stock must never drop below 0 (no overselling)");
        assertEquals(10, finalInventory.getQuantityAvailable() + finalInventory.getQuantityAllocated(), "Total inventory must be conserved");
    }
}
