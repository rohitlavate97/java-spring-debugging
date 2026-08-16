package com.eopis.order.service;

import com.eopis.customer.entity.Customer;
import com.eopis.order.entity.OrderItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class OrderPricingService {

    private static final BigDecimal VIP_DISCOUNT_PERCENTAGE = new BigDecimal("0.10"); // 10%
    private static final BigDecimal ENTERPRISE_DISCOUNT_PERCENTAGE = new BigDecimal("0.20"); // 20%
    private static final BigDecimal TAX_RATE = new BigDecimal("0.08"); // 8%
    private static final BigDecimal STANDARD_SHIPPING = new BigDecimal("15.00");
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("100.00");

    public PricingSummary calculateOrderPricing(Customer customer, List<OrderItem> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem item : items) {
            BigDecimal itemTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            item.setTotalPrice(itemTotal);
            subtotal = subtotal.add(itemTotal);
        }

        BigDecimal discountAmount = calculateTierDiscount(customer, subtotal);
        BigDecimal discountedSubtotal = subtotal.subtract(discountAmount);

        BigDecimal taxAmount = discountedSubtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal shippingAmount = calculateShipping(discountedSubtotal);

        BigDecimal totalAmount = discountedSubtotal.add(taxAmount).add(shippingAmount);

        return new PricingSummary(subtotal, discountAmount, taxAmount, shippingAmount, totalAmount);
    }

    private BigDecimal calculateTierDiscount(Customer customer, BigDecimal subtotal) {
        if (customer == null || customer.getTier() == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal discountRate = BigDecimal.ZERO;
        String tier = customer.getTier();

        if ("VIP".equalsIgnoreCase(tier)) {
            discountRate = VIP_DISCOUNT_PERCENTAGE;
        } else if ("ENTERPRISE".equalsIgnoreCase(tier)) {
            discountRate = ENTERPRISE_DISCOUNT_PERCENTAGE;
        }

        BigDecimal rawDiscount = subtotal.multiply(discountRate);
        return rawDiscount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateShipping(BigDecimal amount) {
        if (amount.compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return STANDARD_SHIPPING.setScale(2, RoundingMode.HALF_UP);
    }

    public static class PricingSummary {
        private final BigDecimal subtotal;
        private final BigDecimal discountAmount;
        private final BigDecimal taxAmount;
        private final BigDecimal shippingAmount;
        private final BigDecimal totalAmount;

        public PricingSummary(BigDecimal subtotal, BigDecimal discountAmount, BigDecimal taxAmount, BigDecimal shippingAmount, BigDecimal totalAmount) {
            this.subtotal = subtotal;
            this.discountAmount = discountAmount;
            this.taxAmount = taxAmount;
            this.shippingAmount = shippingAmount;
            this.totalAmount = totalAmount;
        }

        public BigDecimal getSubtotal() { return subtotal; }
        public BigDecimal getDiscountAmount() { return discountAmount; }
        public BigDecimal getTaxAmount() { return taxAmount; }
        public BigDecimal getShippingAmount() { return shippingAmount; }
        public BigDecimal getTotalAmount() { return totalAmount; }
    }
}
