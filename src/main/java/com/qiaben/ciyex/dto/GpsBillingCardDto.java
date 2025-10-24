package com.qiaben.ciyex.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GpsBillingCardDto {
    private Long id;
    private Long userId;
    private String gpsCustomerVaultId;

    private String brand;
    private String last4;
    private Integer expMonth;
    private Integer expYear;
    private boolean isDefault;

    // Cardholder info
    private String firstName;
    private String lastName;

    // Billing address
    private String address;  // full address (optional)
    private String street;
    private String city;
    private String state;
    private String zip;

    // Raw card data (used only for tokenization)
    private String cardNumber;
    private String cvv;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
