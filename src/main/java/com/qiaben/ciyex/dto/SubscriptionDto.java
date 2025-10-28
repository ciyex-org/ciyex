package com.qiaben.ciyex.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionDto {
    private Long id;
    private Long orgId;
    private Long userId;

    private String service;
    private String billingCycle; // "Yearly" or "Monthly"

    // accept string from client (yyyy-MM-dd or ISO datetime)
    private String startDate;

    private String status;
    private BigDecimal price;        // monetary values should use BigDecimal
}
