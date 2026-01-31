package com.testbackend.payment.service;

import com.testbackend.payment.client.OrderServiceClient;
import com.testbackend.payment.common.dto.*;
import com.testbackend.payment.entity.PaymentCallback;
import com.testbackend.payment.repository.PaymentCallbackRepository;
import com.testbackend.payment.repository.PaymentRepository;
import com.testbackend.payment.client.NotificationServiceClient;
import com.testbackend.payment.constant.PaymentStatus;
import com.testbackend.payment.entity.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentCallbackRepository callbackRepository;
    private final OrderServiceClient orderServiceClient;
    private final NotificationServiceClient notificationServiceClient;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        // Generate UUID as primary key
        UUID paymentId = UUID.randomUUID();

        Payment payment = Payment.builder()
                .id(paymentId)
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        log.info("Payment created with UUID: {}", paymentId);

        return PaymentResponse.builder()
                .paymentId(paymentId)
                .status(payment.getStatus().name())
                .message("Payment initiated successfully")
                .build();
    }

    @Transactional
    public CallbackResponse handleCallback(PaymentCallbackRequest callback, String idempotencyKey) {
        // Convert idempotency key to UUID
        UUID idempotencyUUID = UUID.fromString(idempotencyKey);

        // Check if callback already processed (idempotency)
        Optional<PaymentCallback> existingCallback = callbackRepository.findById(idempotencyUUID);

        if (existingCallback.isPresent()) {
            log.warn("Duplicate callback detected with idempotency key: {}", idempotencyKey);
            return CallbackResponse.builder()
                    .success(true)
                    .message("Callback already processed")
                    .build();
        }

        // Find payment
        Payment payment = paymentRepository.findById(callback.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Payment not found: " + callback.getPaymentId()));

        // Prevent double charge - check if already completed
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.warn("Payment already completed: {}", payment.getId());
            return CallbackResponse.builder()
                    .success(true)
                    .message("Payment already processed")
                    .build();
        }

        // Save callback record for idempotency (using idempotency key as ID)
        PaymentCallback callbackRecord = PaymentCallback.builder()
                .id(idempotencyUUID)
                .paymentId(callback.getPaymentId())
                .status(callback.getStatus())
                .transactionId(callback.getTransactionId())
                .processedAt(LocalDateTime.now())
                .build();
        callbackRepository.save(callbackRecord);

        // Update payment status
        payment.setStatus(PaymentStatus.valueOf(callback.getStatus()));
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Notify order service
        try {
            orderServiceClient.updateOrderStatus(payment.getOrderId(), callback.getStatus());
        } catch (Exception e) {
            log.error("Failed to update order service", e);
        }

        // Send notification
        try {
            NotificationRequest notifRequest = NotificationRequest.builder()
                    .paymentId(payment.getId())
                    .orderId(payment.getOrderId())
                    .status(callback.getStatus())
                    .amount(payment.getAmount())
                    .build();
            notificationServiceClient.sendNotification(notifRequest);
        } catch (Exception e) {
            log.error("Failed to send notification", e);
        }

        log.info("Payment callback processed successfully: {}", payment.getId());

        return CallbackResponse.builder()
                .success(true)
                .message("Callback processed successfully")
                .build();
    }

    public PaymentStatusResponse getPaymentStatus(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        return PaymentStatusResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .build();
    }
}