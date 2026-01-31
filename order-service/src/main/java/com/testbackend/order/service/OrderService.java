package com.testbackend.order.service;


import com.testbackend.order.client.PaymentServiceClient;
import com.testbackend.order.common.dto.*;
import com.testbackend.order.constant.OrderStatus;
import com.testbackend.order.entity.Order;
import com.testbackend.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentServiceClient paymentServiceClient;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        UUID orderId = UUID.randomUUID();

        Order order = Order.builder()
                .id(orderId)
                .customerId(request.getCustomerId())
                .productName(request.getProductName())
                .quantity(request.getQuantity())
                .totalAmount(request.getTotalAmount())
                .status(OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build();

        orderRepository.save(order);

        log.info("Order created with UUID: {}", orderId);

        // Initiate payment with retry mechanism
        UUID paymentId = null;
        try {
            PaymentRequest paymentRequest = PaymentRequest.builder()
                    .orderId(order.getId())
                    .amount(order.getTotalAmount())
                    .build();

            PaymentResponse paymentResponse = paymentServiceClient.processPayment(paymentRequest);

            paymentId = paymentResponse.getPaymentId();
            order.setPaymentId(paymentId);
            order.setStatus(OrderStatus.PAYMENT_PENDING);
            orderRepository.save(order);

            log.info("Payment initiated for order: {} with payment UUID: {}", orderId, paymentId);

        } catch (Exception e) {
            log.error("Failed to initiate payment for order: {}", orderId, e);
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            orderRepository.save(order);
        }

        return OrderResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus().name())
                .paymentId(paymentId)
                .message("Order created successfully")
                .build();
    }

    @Transactional
    public UpdateStatusResponse updateOrderStatus(UUID orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        OrderStatus newStatus;
        switch (status) {
            case "SUCCESS":
                newStatus = OrderStatus.PAID;
                break;
            case "FAILED":
                newStatus = OrderStatus.PAYMENT_FAILED;
                break;
            case "PENDING":
                newStatus = OrderStatus.PAYMENT_PENDING;
                break;
            default:
                newStatus = OrderStatus.PAYMENT_PENDING;
        }

        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        log.info("Order status updated: {} -> {}", orderId, newStatus);

        return UpdateStatusResponse.builder()
                .orderId(orderId)
                .status(newStatus.name())
                .message("Order status updated successfully")
                .build();
    }

    public OrderDetailsResponse getOrderDetails(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        return OrderDetailsResponse.builder()
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .productName(order.getProductName())
                .quantity(order.getQuantity())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .paymentId(order.getPaymentId())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
