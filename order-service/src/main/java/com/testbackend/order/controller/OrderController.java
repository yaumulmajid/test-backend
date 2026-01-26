package com.testbackend.order.controller;

import com.testbackend.order.common.dto.CreateOrderRequest;
import com.testbackend.order.common.dto.OrderDetailsResponse;
import com.testbackend.order.common.dto.OrderResponse;
import com.testbackend.order.common.dto.UpdateStatusResponse;
import com.testbackend.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<UpdateStatusResponse> updateOrderStatus(
            @PathVariable UUID orderId,
            @RequestParam String status) {
        UpdateStatusResponse response = orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailsResponse> getOrderDetails(@PathVariable UUID orderId) {
        OrderDetailsResponse response = orderService.getOrderDetails(orderId);
        return ResponseEntity.ok(response);
    }
}
