package com.eopis.order.service;

import com.eopis.order.dto.CreateOrderRequest;
import com.eopis.order.dto.OrderResponse;
import com.eopis.order.entity.Order;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request);
    OrderResponse getOrderById(Long orderId);
    List<OrderResponse> getOrdersByCustomerId(Long customerId);
    void cancelOrder(Long orderId);
}
