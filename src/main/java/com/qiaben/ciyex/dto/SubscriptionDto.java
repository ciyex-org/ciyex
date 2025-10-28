package com.qiaben.ciyex.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionDto {
    private Long id;
    private UUID userId;

    private String service;
    private String billingCycle; // "Yearly" or "Monthly"

    // accept string from client (yyyy-MM-dd or ISO datetime)
    private String startDate;

    private String status;
    private BigDecimal price;        // monetary values should use BigDecimal
}
