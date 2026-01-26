package com.testbackend.order.client;

import com.testbackend.order.common.dto.PaymentRequest;
import com.testbackend.order.common.dto.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class PaymentServiceClient {

    private final RestTemplate restTemplate;
    private final String paymentServiceUrl;

    public PaymentServiceClient(
            RestTemplate restTemplate,
            @Value("${payment.service.url}") String paymentServiceUrl) {
        this.restTemplate = restTemplate;
        this.paymentServiceUrl = paymentServiceUrl;
    }

    public PaymentResponse processPayment(PaymentRequest request) {
        String url = paymentServiceUrl + "/api/payments/process";
        log.info("Calling payment service at: {}", url);

        return restTemplate.postForObject(url, request, PaymentResponse.class);
    }
}