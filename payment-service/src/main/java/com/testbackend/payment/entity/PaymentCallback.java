package com.testbackend.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mst_payment_callbacks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCallback {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;
}
