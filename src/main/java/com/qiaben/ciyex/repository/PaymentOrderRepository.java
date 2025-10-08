package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.PaymentOrder;
import com.qiaben.ciyex.entity.PaymentOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    // 🔹 Find by Stripe PaymentIntent ID (used in webhook/payment confirmation)
    Optional<PaymentOrder> findByStripePaymentIntentId(String paymentIntentId);

    // 🔹 Find all orders with a given status (PENDING, PAID, FAILED)
    List<PaymentOrder> findByStatus(PaymentOrderStatus status);

    // 🔹 Find all orders by payment method (STRIPE, GPS, etc.)
    List<PaymentOrder> findByMethod(String method);

    // 🔹 Check if an invoiceId was included in a bulk payment order
    List<PaymentOrder> findByInvoiceIdsContaining(String invoiceId);
}
