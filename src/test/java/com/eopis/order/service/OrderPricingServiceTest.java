package com.eopis.order.service;

import com.eopis.customer.entity.Customer;
import com.eopis.order.entity.OrderItem;
import com.eopis.product.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderPricingServiceTest {

    private OrderPricingService pricingService;

    @BeforeEach
    void setUp() {
        pricingService = new OrderPricingService();
    }

    @Test
    @DisplayName("Calculate pricing for VIP customer order with multiple items")
    void shouldCalculateCorrectPricingForVipCustomerOrder() {
        // Given
        Customer vipCustomer = new Customer("CUST-18291", "Eleanor", "Vance", "+1-555-0192", "VIP");

        Product laptop = new Product("PROD-381", "Enterprise Developer Laptop", "High performance laptop", new BigDecimal("1200.00"), "USD");
        Product mouse = new Product("PROD-102", "Ergonomic Wireless Mouse", "Precision wireless mouse", new BigDecimal("50.00"), "USD");

        OrderItem item1 = new OrderItem(laptop, 1, new BigDecimal("1200.00"), null);
        OrderItem item2 = new OrderItem(mouse, 2, new BigDecimal("50.00"), null);

        List<OrderItem> items = List.of(item1, item2);

        // When
        OrderPricingService.PricingSummary summary = pricingService.calculateOrderPricing(vipCustomer, items);

        // Expected Calculations:
        // Subtotal: (1200.00 * 1) + (50.00 * 2) = 1300.00
        // VIP Discount (10%): 1300.00 * 0.10 = 130.00
        // Discounted Subtotal: 1300.00 - 130.00 = 1170.00
        // Tax (8%): 1170.00 * 0.08 = 93.60
        // Shipping: Free (>= 100.00) = 0.00
        // Total: 1170.00 + 93.60 + 0.00 = 1263.60

        // Then
        assertEquals(new BigDecimal("1300.00"), summary.getSubtotal().setScale(2));
        assertEquals(new BigDecimal("130.00"), summary.getDiscountAmount().setScale(2));
        assertEquals(new BigDecimal("93.60"), summary.getTaxAmount().setScale(2));
        assertEquals(new BigDecimal("0.00"), summary.getShippingAmount().setScale(2));
        assertEquals(new BigDecimal("1263.60"), summary.getTotalAmount().setScale(2));
    }
}
