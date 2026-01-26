package com.testbackend.notification.service;

import com.testbackend.notification.common.dto.NotificationRequest;
import com.testbackend.notification.common.dto.NotificationResponse;
import com.testbackend.notification.constant.NotificationStatus;
import com.testbackend.notification.constant.NotificationType;
import com.testbackend.notification.entity.Notification;
import com.testbackend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    @Transactional
    public NotificationResponse sendNotification(NotificationRequest request) {
        UUID notificationId = UUID.randomUUID();

        Notification notification = Notification.builder()
                .id(notificationId)
                .paymentId(request.getPaymentId())
                .orderId(request.getOrderId())
                .type(NotificationType.EMAIL)
                .status(NotificationStatus.PENDING)
                .message(buildMessage(request))
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);

        try {
            // Send email notification
            emailService.sendEmail(
                    "customer@example.com", // need update get from customer data
                    "Payment Status Update",
                    notification.getMessage()
            );

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());

            log.info("Notification sent successfully with UUID: {}", notificationId);

        } catch (Exception e) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());

            log.error("Failed to send notification: {}", notificationId, e);
        }

        notificationRepository.save(notification);

        return mapToResponse(notification);
    }

    public List<NotificationResponse> getNotificationsByPayment(UUID paymentId) {
        List<Notification> notifications = notificationRepository.findByPaymentId(paymentId);
        return notifications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private String buildMessage(NotificationRequest request) {
        String status = request.getStatus();

        switch (status) {
            case "SUCCESS":
                return String.format(
                        "Your payment for Order %s has been successfully processed. Amount: %s",
                        request.getOrderId(),
                        request.getAmount()
                );
            case "FAILED":
                return String.format(
                        "Your payment for Order %s has failed. Please try again.",
                        request.getOrderId()
                );
            case "PENDING":
                return String.format(
                        "Your payment for Order %s is being processed. We'll notify you once completed.",
                        request.getOrderId()
                );
            default:
                return String.format(
                        "Payment status update for Order %s: %s",
                        request.getOrderId(),
                        status
                );
        }
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getId())
                .paymentId(notification.getPaymentId())
                .orderId(notification.getOrderId())
                .status(notification.getStatus().name())
                .message(notification.getMessage())
                .sentAt(notification.getSentAt())
                .build();
    }
}
