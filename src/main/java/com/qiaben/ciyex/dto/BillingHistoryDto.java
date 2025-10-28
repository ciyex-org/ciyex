package com.qiaben.ciyex.dto;

import com.qiaben.ciyex.entity.BillingHistory.BillingProvider;
import com.qiaben.ciyex.entity.BillingHistory.BillingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingHistoryDto {
    private Long id;
    private Long orgId;
    private Long userId;

    /** Payment provider: STRIPE or GPS */
    private BillingProvider provider;

    /** Stripe specific fields */
    private String stripePaymentIntentId;
    private String stripePaymentMethodId;

    /** GPS specific fields */
    private String gpsTransactionId;
    private String gpsCustomerVaultId;

    /** Common payment fields */
    private BigDecimal amount;     // Use BigDecimal for precision
    private BillingStatus status;  // Enum instead of String
    private String responseMessage;
    private String responseCode;

    /** Foreign key to InvoiceBill */
    private Long invoiceBillId;

    /** External invoice/payment references */
    private String externalId;   // invoice external id/number
    private String invoiceUrl;   // hosted invoice link (Stripe/GPS)
    private String receiptUrl;   // receipt link (Stripe/GPS)

    /** Audit fields */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Optional extra fields for payment processing (GPS) */
    private String firstName;
    private String lastName;
    private String street;
    private String city;
    private String state;
    private String zip;
}
