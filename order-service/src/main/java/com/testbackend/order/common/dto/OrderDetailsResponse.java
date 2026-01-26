package com.testbackend.order.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailsResponse {
    private UUID orderId;
    private String customerId;
    private String productName;
    private Integer quantity;
    private BigDecimal totalAmount;
    private String status;
    private UUID paymentId;
    private LocalDateTime createdAt;
}
