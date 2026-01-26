package com.testbackend.payment.client;


import com.testbackend.payment.common.dto.NotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceClient {

    private final RestTemplate restTemplate;

    @Value("${notification.service.url}")
    private String notificationServiceUrl;

    public void sendNotification(NotificationRequest request) {
        try {
            String url = notificationServiceUrl + "/api/notifications/send";
            restTemplate.postForObject(url, request, Void.class);
            log.info("Notification sent successfully for paymentId: {}", request.getPaymentId());
        } catch (Exception e) {
            log.error("Failed to send notification for paymentId: {}", request.getPaymentId(), e);
            throw new RuntimeException("Failed to send notification", e);
        }
    }
}
