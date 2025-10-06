package com.qiaben.ciyex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StripeBillingCardDto {
    private Long id;

    private Long userId;

    private Long orgId;

    private String stripePaymentMethodId;

    private String stripeCustomerId;

    private String brand;

    private String last4;

    private Integer expMonth;

    private Integer expYear;

    @JsonProperty("isDefault") // ensures JSON uses "isDefault" key
    private boolean isDefault;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
