package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByStripePaymentIntentId(String paymentIntentId);
}
