package com.testbackend.payment.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderServiceClient {

    private final RestTemplate restTemplate;

    @Value("${order.service.url}")
    private String orderServiceUrl;

    public void updateOrderStatus(UUID orderId, String status) {
        try {
            String url = orderServiceUrl + "/api/orders/" + orderId + "/status?status=" + status;
            restTemplate.put(url, null);
            log.info("Order status updated successfully for orderId: {}", orderId);
        } catch (Exception e) {
            log.error("Failed to update order status for orderId: {}", orderId, e);
            throw new RuntimeException("Failed to update order status", e);
        }
    }
}
