package com.qiaben.ciyex.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionDto {
    private Long id;
    private Long orgId;
    private Long userId;

    private String service;
    private String billingCycle;
    private String scope;
    private String status;
    private String startDate;
    private Double price;
}
