package com.eopis.order.service;

import com.eopis.customer.entity.Customer;
import com.eopis.customer.repository.CustomerRepository;
import com.eopis.inventory.service.InventoryService;
import com.eopis.order.dto.CreateOrderRequest;
import com.eopis.order.dto.OrderItemRequest;
import com.eopis.order.dto.OrderResponse;
import com.eopis.order.entity.Order;
import com.eopis.order.entity.OrderItem;
import com.eopis.order.entity.OrderStatus;
import com.eopis.order.repository.OrderRepository;
import com.eopis.product.entity.Product;
import com.eopis.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderPricingService pricingService;
    private final InventoryService inventoryService;
    private final OrderEventPublisher eventPublisher;
    private final com.eopis.common.metrics.EopisMetricsService metricsService;

    public OrderServiceImpl(OrderRepository orderRepository,
                            CustomerRepository customerRepository,
                            ProductRepository productRepository,
                            OrderPricingService pricingService,
                            InventoryService inventoryService,
                            OrderEventPublisher eventPublisher,
                            com.eopis.common.metrics.EopisMetricsService metricsService) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.pricingService = pricingService;
        this.inventoryService = inventoryService;
        this.eventPublisher = eventPublisher;
        this.metricsService = metricsService;
    }

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Processing order placement for Customer ID: {}", request.getCustomerId());

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + request.getCustomerId()));

        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setStatus(OrderStatus.PENDING);
        order.setCouponId(request.getCouponId());

        List<OrderItem> items = new ArrayList<>();
        for (OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + itemReq.getProductId()));

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(product.getPrice());
            order.addItem(item);
            items.add(item);
        }

        OrderPricingService.PricingSummary pricing = pricingService.calculateOrderPricing(customer, items);
        order.setSubtotalAmount(pricing.getSubtotal());
        order.setDiscountAmount(pricing.getDiscountAmount());
        order.setTaxAmount(pricing.getTaxAmount());
        order.setShippingAmount(pricing.getShippingAmount());
        order.setTotalAmount(pricing.getTotalAmount());

        Order savedOrder = orderRepository.save(order);
        log.info("Order saved with ID: {}, Order Number: {}", savedOrder.getId(), savedOrder.getOrderNumber());

        // Reserve inventory in the same transaction
        List<com.eopis.inventory.entity.InventoryReservation> reservations = inventoryService.reserveStockForOrder(savedOrder);

        // Record custom metrics
        metricsService.incrementOrdersPlaced();
        metricsService.incrementInventoryReservations(reservations.size());

        // Publish OrderCreated event
        eventPublisher.publishOrderCreated(new com.eopis.order.event.OrderCreatedEvent(
                savedOrder.getId(),
                savedOrder.getOrderNumber(),
                customer.getId(),
                customer.getCustomerNumber(),
                savedOrder.getTotalAmount()
        ));

        return OrderResponse.fromEntity(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));
        return OrderResponse.fromEntity(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerId(Long customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel order in status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        inventoryService.releaseReservation(orderId);
        log.info("Order #{} cancelled and inventory released", order.getOrderNumber());
    }
}
