package com.testbackend.payment.repository;

import com.testbackend.payment.entity.PaymentCallback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PaymentCallbackRepository extends JpaRepository<PaymentCallback, UUID> {
}

