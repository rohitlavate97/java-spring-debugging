package com.eopis.order.service;

import com.eopis.customer.entity.Customer;
import com.eopis.customer.repository.CustomerRepository;
import com.eopis.inventory.entity.Inventory;
import com.eopis.inventory.entity.InventoryReservation;
import com.eopis.inventory.entity.Warehouse;
import com.eopis.inventory.repository.InventoryRepository;
import com.eopis.inventory.repository.InventoryReservationRepository;
import com.eopis.inventory.repository.WarehouseRepository;
import com.eopis.order.dto.CreateOrderRequest;
import com.eopis.order.dto.OrderItemRequest;
import com.eopis.order.dto.OrderResponse;
import com.eopis.order.entity.OrderStatus;
import com.eopis.order.repository.OrderRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderServiceIntegrationTest {

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

    @Autowired
    private OrderRepository orderRepository;

    private Customer testCustomer;
    private Product testProduct;
    private Warehouse testWarehouse;
    private Inventory testInventory;

    @BeforeEach
    void setUp() {
        Category category = new Category("Hardware", "CAT-HW", "Hardware devices");
        categoryRepository.save(category);

        testProduct = new Product("PROD-381", "Enterprise Developer Laptop", "High performance laptop", new BigDecimal("1000.00"), "USD");
        testProduct.setCategory(category);
        productRepository.save(testProduct);

        testWarehouse = new Warehouse("WH-17", "Springfield Distribution Center", "123 Main St");
        warehouseRepository.save(testWarehouse);

        testInventory = new Inventory(testWarehouse, testProduct, 50, 0);
        inventoryRepository.save(testInventory);

        testCustomer = new Customer("CUST-18291", "Eleanor", "Vance", "+1-555-0192", "VIP");
        customerRepository.save(testCustomer);
    }

    @Test
    @DisplayName("Successfully create order and verify inventory reservation")
    void shouldCreateOrderAndReserveInventory() {
        CreateOrderRequest request = new CreateOrderRequest(
                testCustomer.getId(),
                List.of(new OrderItemRequest(testProduct.getId(), 2)),
                null
        );

        OrderResponse response = orderService.createOrder(request);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(new BigDecimal("2000.00"), response.getSubtotalAmount().setScale(2));
        assertEquals(new BigDecimal("200.00"), response.getDiscountAmount().setScale(2)); // VIP 10%

        // Verify inventory allocation
        Inventory updatedInventory = inventoryRepository.findById(testInventory.getId()).orElseThrow();
        assertEquals(48, updatedInventory.getQuantityAvailable());
        assertEquals(2, updatedInventory.getQuantityAllocated());

        // Verify reservation record
        List<InventoryReservation> reservations = reservationRepository.findByOrderId(response.getId());
        assertEquals(1, reservations.size());
        assertEquals(2, reservations.get(0).getQuantity());
    }

    @Test
    @DisplayName("Fail order creation when requested quantity exceeds available stock")
    void shouldFailWhenStockIsInsufficient() {
        CreateOrderRequest request = new CreateOrderRequest(
                testCustomer.getId(),
                List.of(new OrderItemRequest(testProduct.getId(), 100)), // available is 50
                null
        );

        assertThrows(IllegalStateException.class, () -> orderService.createOrder(request));

        // Verify inventory was untouched
        Inventory unchangedInventory = inventoryRepository.findById(testInventory.getId()).orElseThrow();
        assertEquals(50, unchangedInventory.getQuantityAvailable());
        assertEquals(0, unchangedInventory.getQuantityAllocated());
    }
}
