package com.qiaben.ciyex.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for handling payment requests without Jakarta Validation dependencies.
 * Manual validation is implemented inside validate().
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestDto {

    /**
     * Payment method: e.g., "STRIPE" or "GPS".
     */
    // paymentMethod field declared further down with JSON aliases

    /**
     * Card identifier (if applicable).
     * Example: "card_1234" for Stripe, or card ID for GPS.
     */
    private String cardId;

    /**
     * Specific invoice IDs to pay.
     * Required unless {@code payAll = true}.
     */
    @JsonProperty("invoice_ids")
    @JsonAlias({"invoiceIds", "invoices", "invoice_ids"})
    private List<Long> invoiceIds;

    /**
     * Optional: bill date for reference.
     */
    private LocalDate billDate;

    /**
     * Whether to pay all unpaid invoices for the org.
     * If true, {@link #orgId} is required.
     */
    @JsonProperty("pay_all")
    @JsonAlias({"payAll", "pay_all"})
    private boolean payAll;

    /**
     * Organization ID.
     * Required when {@code payAll = true}.
     */
    @JsonProperty("org_id")
    @JsonAlias({"orgId", "org_id"})
    private Long orgId;

    @JsonProperty("payment_method")
    @JsonAlias({"paymentMethod", "payment_method"})
    private String paymentMethod;

    // --- Manual validation helper ---
    public void validate() {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new IllegalArgumentException("Payment method is required (e.g., STRIPE or GPS)");
        }

        if (!payAll && (invoiceIds == null || invoiceIds.isEmpty())) {
            throw new IllegalArgumentException("At least one invoice ID must be provided unless payAll=true");
        }

        if (payAll && orgId == null) {
            throw new IllegalArgumentException("OrgId is required when payAll=true");
        }
    }

    @Override
    public String toString() {
        return "PaymentRequestDto{" +
                "paymentMethod='" + paymentMethod + '\'' +
                ", cardId='" + cardId + '\'' +
                ", invoiceIds=" + invoiceIds +
                ", billDate=" + billDate +
                ", payAll=" + payAll +
                ", orgId=" + orgId +
                '}';
    }
}
