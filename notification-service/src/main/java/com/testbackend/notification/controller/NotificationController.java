package com.testbackend.notification.controller;

import com.testbackend.notification.common.dto.NotificationRequest;
import com.testbackend.notification.common.dto.NotificationResponse;
import com.testbackend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @PostMapping("/send")
    public ResponseEntity<NotificationResponse> sendNotification(@RequestBody NotificationRequest request) {
        NotificationResponse response = notificationService.sendNotification(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByPayment(
            @PathVariable UUID paymentId) {
        List<NotificationResponse> notifications = notificationService.getNotificationsByPayment(paymentId);
        return ResponseEntity.ok(notifications);
    }
}
