package com.testbackend.payment.common.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PaymentRequest {
    private UUID orderId;
    private BigDecimal amount;
}
