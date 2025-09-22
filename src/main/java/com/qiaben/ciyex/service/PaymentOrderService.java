package com.qiaben.ciyex.service;

import com.qiaben.ciyex.entity.PaymentOrder;
import com.qiaben.ciyex.entity.PaymentOrderStatus;
import com.qiaben.ciyex.repository.PaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentOrderService {

    private final PaymentOrderRepository repository;

    /**
     * Create a new PaymentOrder with PENDING status
     */
    public PaymentOrder createPendingOrder(String paymentIntentId, Long amount) {
        PaymentOrder order = new PaymentOrder();
        order.setStripePaymentIntentId(paymentIntentId);
        order.setAmount(amount);
        order.setStatus(PaymentOrderStatus.PENDING);
        return repository.save(order);
    }

    /**
     * Mark an existing PaymentOrder as PAID
     */
    public void markOrderAsPaid(String paymentIntentId) {
        Optional<PaymentOrder> orderOpt = repository.findByStripePaymentIntentId(paymentIntentId);
        orderOpt.ifPresent(order -> {
            order.setStatus(PaymentOrderStatus.PAID);
            repository.save(order);
        });
    }

    /**
     * Mark an existing PaymentOrder as FAILED
     */
    public void markOrderAsFailed(String paymentIntentId) {
        Optional<PaymentOrder> orderOpt = repository.findByStripePaymentIntentId(paymentIntentId);
        orderOpt.ifPresent(order -> {
            order.setStatus(PaymentOrderStatus.FAILED);
            repository.save(order);
        });
    }

    /**
     * Fetch PaymentOrder by ID
     */
    public Optional<PaymentOrder> getOrderById(Long id) {
        return repository.findById(id);
    }
}
