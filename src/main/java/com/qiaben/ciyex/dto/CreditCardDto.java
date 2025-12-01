package com.qiaben.ciyex.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditCardDto {

    private Long id;

    private String externalId;

    private String fhirId;

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotBlank(message = "Card holder name is required")
    @Size(max = 100, message = "Card holder name must not exceed 100 characters")
    private String cardHolderName;

    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "^\\d{13,16}$", message = "Card number must be 13-16 digits")
    private String cardNumber;

    @Size(max = 20, message = "Card type must not exceed 20 characters")
    private String cardType; // VISA, MASTERCARD, AMEX, DISCOVER

    @NotNull(message = "Expiry month is required")
    @Min(value = 1, message = "Expiry month must be between 1 and 12")
    @Max(value = 12, message = "Expiry month must be between 1 and 12")
    private Integer expiryMonth;

    @NotNull(message = "Expiry year is required")
    @Min(value = 2025, message = "Expiry year must be current year or later")
    private Integer expiryYear;

    @NotBlank(message = "CVV is required")
    @Pattern(regexp = "^\\d{3,4}$", message = "CVV must be 3 or 4 digits")
    private String cvv;

    private String billingAddress;

    @Size(max = 50, message = "Billing city must not exceed 50 characters")
    private String billingCity;

    @Size(max = 50, message = "Billing state must not exceed 50 characters")
    private String billingState;

    @Size(max = 10, message = "Billing zip must not exceed 10 characters")
    private String billingZip;

    @Size(max = 50, message = "Billing country must not exceed 50 characters")
    private String billingCountry;

    private Boolean isDefault;

    private Boolean isActive;

    private String token;

    // For response - masked card number
    private String maskedCardNumber;

    // For response - expiration status
    private Boolean isExpired;

    private Audit audit;

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
        private String createdBy;
        private String updatedBy;
    }
}
