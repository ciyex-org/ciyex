package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.PaymentRequestDto;
import com.qiaben.ciyex.entity.InvoiceBill;
import com.qiaben.ciyex.entity.InvoiceStatus;
import com.qiaben.ciyex.entity.PaymentOrder;
import com.qiaben.ciyex.entity.PaymentOrderStatus;
import com.qiaben.ciyex.repository.InvoiceBillRepository;
import com.qiaben.ciyex.repository.BillingHistoryRepository;
import com.qiaben.ciyex.entity.BillingHistory;
import com.qiaben.ciyex.entity.BillingHistory.BillingProvider;
import com.qiaben.ciyex.entity.BillingHistory.BillingStatus;
import com.qiaben.ciyex.repository.PaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentOrderService {

    private final PaymentOrderRepository repository;
    private final InvoiceBillRepository invoiceBillRepository;
    private final BillingHistoryRepository billingHistoryRepository;
    private final StripeService stripeService;

    /**
     * Calculate total amount from invoiceIds (in cents).
     */
    public long calculateTotalAmount(PaymentRequestDto request) {
        List<InvoiceBill> invoices;

        if (request.isPayAll()) {
            if (request.getOrgId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OrgId is required when payAll = true");
            }
            invoices = invoiceBillRepository.findByOrgId(request.getOrgId());
        } else if (request.getInvoiceIds() != null && !request.getInvoiceIds().isEmpty()) {
            invoices = invoiceBillRepository.findAllById(request.getInvoiceIds());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No invoiceIds provided");
        }

        long totalCents = invoices.stream()
                .filter(inv -> inv.getStatus() != null && inv.getStatus() == InvoiceStatus.UNPAID)
                .map(InvoiceBill::getAmount)
                .filter(amount -> amount != null)
                .map(amount -> amount.multiply(BigDecimal.valueOf(100)))
                .mapToLong(BigDecimal::longValue)
                .sum();

        if (totalCents <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No unpaid invoices available for payment");
        }

        return totalCents;
    }

    /**
     * Create a new PaymentOrder with PENDING status.
     */
    public PaymentOrder createPendingOrder(
            String paymentIntentId,
            Long amount, // in cents
            String method,
            String cardId,
            List<Long> invoiceIds,
            String receiptUrl,
            Long orgId
    ) {
        PaymentOrder order = new PaymentOrder();
        order.setStripePaymentIntentId(paymentIntentId);
        order.setOrgId(orgId);
        order.setAmount(amount);
        order.setStatus(PaymentOrderStatus.PENDING);
        order.setMethod(method);
        order.setCardId(cardId);
        order.setReceiptUrl(receiptUrl);

        // store invoiceIds as comma-separated string for traceability
        String invoiceIdsStr = (invoiceIds != null && !invoiceIds.isEmpty())
                ? invoiceIds.stream().map(String::valueOf).collect(Collectors.joining(","))
                : "";
        order.setInvoiceIds(invoiceIdsStr);

        // fetch invoice numbers for display (INV-00001, etc.)
        if (invoiceIds != null && !invoiceIds.isEmpty()) {
            List<InvoiceBill> invoices = invoiceBillRepository.findAllById(invoiceIds);
            String invoiceNumbers = invoices.stream()
                    .map(InvoiceBill::getInvoiceNumber)
                    .collect(Collectors.joining(", "));
            order.setInvoiceNumber(invoiceNumbers);
        } else {
            order.setInvoiceNumber(generateInvoiceNumber(null)); // fallback if no invoices
        }

        LocalDateTime now = LocalDateTime.now();
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        // 🔹 Default due date: +30 days
        order.setDueDate(now.plusDays(30));

        return repository.save(order);
    }

    /**
     * Mark an existing PaymentOrder as PAID.
     * Also update linked invoices as PAID.
     */
    public void markOrderAsPaid(String paymentIntentId) {
        Optional<PaymentOrder> orderOpt = repository.findByStripePaymentIntentId(paymentIntentId);
        orderOpt.ifPresent(order -> {
            // Try to populate receiptUrl from Stripe if missing
            if ((order.getReceiptUrl() == null || order.getReceiptUrl().isBlank()) && "STRIPE".equalsIgnoreCase(order.getMethod())) {
                try {
                    String fetched = stripeService.fetchReceiptUrlForPaymentIntent(order.getOrgId(), paymentIntentId);
                    if (fetched != null && !fetched.isBlank()) {
                        order.setReceiptUrl(fetched);
                        repository.save(order);
                    }
                } catch (Exception e) {
                    // non-fatal: log and continue
                    // logger not available here; use System.err to avoid adding new logging dependency
                    System.err.println("Warning: could not fetch receipt URL for intent " + paymentIntentId + ": " + e.getMessage());
                }
            }
            order.setStatus(PaymentOrderStatus.PAID);
            LocalDateTime now = LocalDateTime.now();
            order.setUpdatedAt(now);
            order.setPaidAt(now);
            repository.save(order);
            // 🔹 Update invoices linked to this order
            if (order.getInvoiceIds() != null && !order.getInvoiceIds().isEmpty()) {
                List<Long> invoiceIds = Arrays.stream(order.getInvoiceIds().split(","))
                        .filter(s -> !s.isBlank())
                        .map(Long::valueOf)
                        .collect(Collectors.toList());

                List<InvoiceBill> invoices = invoiceBillRepository.findAllById(invoiceIds);
                invoices.forEach(inv -> {
                    inv.setStatus(InvoiceStatus.PAID);
                    // If the payment order has a receipt URL (e.g., from Stripe), propagate it to the invoice
                    if (order.getReceiptUrl() != null && !order.getReceiptUrl().isBlank()) {
                        inv.setReceiptUrl(order.getReceiptUrl());
                    }
                    inv.setUpdatedAt(now);
                    inv.setPaidAt(now);
                });
                invoiceBillRepository.saveAll(invoices);

                // Update existing INVOICE billing history records to reflect Stripe payment
                invoices.forEach(inv -> {
                    billingHistoryRepository.findByInvoiceBill_Id(inv.getId()).ifPresentOrElse(history -> {
                        history.setProvider(BillingProvider.STRIPE);
                        history.setStripePaymentIntentId(paymentIntentId);
                        history.setStatus(BillingStatus.SUCCEEDED);
                        // Prefer receipt from order if present, otherwise use invoice.receiptUrl
                        if (order.getReceiptUrl() != null && !order.getReceiptUrl().isBlank()) {
                            history.setReceiptUrl(order.getReceiptUrl());
                        } else {
                            history.setReceiptUrl(inv.getReceiptUrl());
                        }
                        history.setInvoiceUrl(inv.getInvoiceUrl());
                        history.setAmount(inv.getAmount());
                        history.setUpdatedAt(now);
                        billingHistoryRepository.save(history);
                    }, () -> {
                        // fallback: create a new billing history if none exists
                        BillingHistory bh = BillingHistory.builder()
                                .orgId(inv.getOrgId())
                                .userId(inv.getUserId())
                                .provider(BillingProvider.STRIPE)
                                .stripePaymentIntentId(paymentIntentId)
                                .amount(inv.getAmount())
                                .status(BillingStatus.SUCCEEDED)
                                .invoiceBill(inv)
                                .invoiceUrl(inv.getInvoiceUrl())
                                .receiptUrl(order.getReceiptUrl() != null && !order.getReceiptUrl().isBlank() ? order.getReceiptUrl() : inv.getReceiptUrl())
                                .createdAt(now)
                                .updatedAt(now)
                                .build();
                        billingHistoryRepository.save(bh);
                    });
                });
            } else if (order.getOrgId() != null) {
                // if invoiceIds empty, treat as pay-all: mark all unpaid invoices for org as PAID
                List<InvoiceBill> invoices = invoiceBillRepository.findByOrgId(order.getOrgId());
                invoices.stream()
                        .filter(inv -> inv.getStatus() == InvoiceStatus.UNPAID)
                        .forEach(inv -> {
                            inv.setStatus(InvoiceStatus.PAID);
                            inv.setUpdatedAt(now);
                            inv.setPaidAt(now);
                        });
                invoiceBillRepository.saveAll(invoices);

                // Update existing INVOICE billing history records to reflect Stripe pay-all
                invoices.stream()
                        .filter(inv -> inv.getStatus() == InvoiceStatus.PAID)
                        .forEach(inv -> {
                            billingHistoryRepository.findByInvoiceBill_Id(inv.getId()).ifPresentOrElse(history -> {
                                history.setProvider(BillingProvider.STRIPE);
                                history.setStripePaymentIntentId(paymentIntentId);
                                history.setStatus(BillingStatus.SUCCEEDED);
                                history.setReceiptUrl(inv.getReceiptUrl());
                                history.setInvoiceUrl(inv.getInvoiceUrl());
                                history.setAmount(inv.getAmount());
                                history.setUpdatedAt(now);
                                billingHistoryRepository.save(history);
                            }, () -> {
                                BillingHistory bh = BillingHistory.builder()
                                        .orgId(inv.getOrgId())
                                        .userId(inv.getUserId())
                                        .provider(BillingProvider.STRIPE)
                                        .stripePaymentIntentId(paymentIntentId)
                                        .amount(inv.getAmount())
                                        .status(BillingStatus.SUCCEEDED)
                                        .invoiceBill(inv)
                                        .invoiceUrl(inv.getInvoiceUrl())
                                        .receiptUrl(inv.getReceiptUrl())
                                        .createdAt(now)
                                        .updatedAt(now)
                                        .build();
                                billingHistoryRepository.save(bh);
                            });
                        });
            }
        });
    }

    /**
     * Mark an existing PaymentOrder as FAILED.
     */
    public void markOrderAsFailed(String paymentIntentId) {
        Optional<PaymentOrder> orderOpt = repository.findByStripePaymentIntentId(paymentIntentId);
        orderOpt.ifPresent(order -> {
            order.setStatus(PaymentOrderStatus.FAILED);
            order.setUpdatedAt(LocalDateTime.now());
            repository.save(order);
        });
    }

    /**
     * Fetch PaymentOrder by ID.
     */
    public Optional<PaymentOrder> getOrderById(Long id) {
        return repository.findById(id);
    }

    /**
     * Fetch all orders by status.
     */
    public List<PaymentOrder> getOrdersByStatus(PaymentOrderStatus status) {
        return repository.findByStatus(status);
    }

    /**
     * Fetch all orders by method (STRIPE, GPS).
     */
    public List<PaymentOrder> getOrdersByMethod(String method) {
        return repository.findByMethod(method);
    }

    /**
     * Fetch all orders that include a given invoiceId.
     */
    public List<PaymentOrder> getOrdersByInvoiceId(Long invoiceId) {
        return repository.findByInvoiceIdsContaining(String.valueOf(invoiceId));
    }

    /**
     * Generate a fallback invoice number if invoices are missing.
     */
    private String generateInvoiceNumber(Long id) {
        if (id == null) {
            return "INV-TEMP-" + System.currentTimeMillis();
        }
        return String.format("INV-%05d", id);
    }
}
