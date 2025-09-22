package com.qiaben.ciyex.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingCardDto {
    private Long id;
    private Long orgId;
    private Long userId;

    /** Stripe PaymentMethod ID (pm_xxx) */
    private String stripePaymentMethodId;

    /** Stripe Customer ID (cus_xxx) */
    private String stripeCustomerId;

    /** Card details snapshot */
    private String brand;
    private String last4;
    private Integer expMonth;
    private Integer expYear;

    /** Track default card per user */
    private Boolean isDefault;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
