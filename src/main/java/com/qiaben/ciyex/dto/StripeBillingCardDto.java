package com.qiaben.ciyex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    /**
     * ✅ Stored as "defaultCard" in Java,
     * ✅ Exposed as "isDefault" in JSON
     */
    @JsonProperty("isDefault")
    private boolean defaultCard;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ✅ Convenience getter for service code
    public boolean isDefault() {
        return defaultCard;
    }
}
