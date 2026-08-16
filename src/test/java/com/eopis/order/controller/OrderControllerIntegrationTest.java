package com.eopis.order.controller;

import com.eopis.customer.entity.Customer;
import com.eopis.customer.repository.CustomerRepository;
import com.eopis.inventory.entity.Inventory;
import com.eopis.inventory.entity.Warehouse;
import com.eopis.inventory.repository.InventoryRepository;
import com.eopis.inventory.repository.WarehouseRepository;
import com.eopis.order.dto.CreateOrderRequest;
import com.eopis.order.dto.OrderItemRequest;
import com.eopis.product.entity.Category;
import com.eopis.product.entity.Product;
import com.eopis.product.repository.CategoryRepository;
import com.eopis.product.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    private ObjectMapper objectMapper;

    private Customer customer;
    private Product product;

    @BeforeEach
    void setUp() {
        Category cat = categoryRepository.findByCode("CAT-ORD-CTRL")
                .orElseGet(() -> categoryRepository.save(new Category("Order Ctrl Cat", "CAT-ORD-CTRL", "Desc")));

        product = productRepository.findBySku("PROD-ORD-CTRL-1")
                .orElseGet(() -> {
                    Product p = new Product("PROD-ORD-CTRL-1", "Order Ctrl Product", "Desc", new BigDecimal("150.00"), "USD");
                    p.setCategory(cat);
                    return productRepository.save(p);
                });

        Warehouse warehouse = warehouseRepository.findByCode("WH-ORD-CTRL")
                .orElseGet(() -> warehouseRepository.save(new Warehouse("WH-ORD-CTRL", "Order Ctrl WH", "Address")));

        inventoryRepository.findByWarehouseIdAndProductId(warehouse.getId(), product.getId())
                .orElseGet(() -> inventoryRepository.save(new Inventory(warehouse, product, 50, 0)));

        customer = customerRepository.findByCustomerNumber("CUST-ORD-CTRL")
                .orElseGet(() -> customerRepository.save(new Customer("CUST-ORD-CTRL", "Ctrl", "User", "555-0199", "REGULAR")));
    }

    @Test
    @WithMockUser(username = "user@eopis.local", roles = {"CUSTOMER"})
    @DisplayName("Verify creating an order via POST /api/orders produces 201 CREATED")
    void shouldCreateOrderViaHttpEndpoint() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                customer.getId(),
                List.of(new OrderItemRequest(product.getId(), 2)),
                null
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}
