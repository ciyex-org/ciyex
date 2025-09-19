package com.qiaben.ciyex.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GpsBillingCardDto {
    private Long id;
    private Long orgId;
    private Long userId;
    private String gpsCustomerVaultId;
    private String gpsTransactionId;
    private String brand;
    private String last4;
    private Integer expMonth;
    private Integer expYear;
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Additional fields for frontend (not persisted)
    private String firstName;
    private String lastName;
    private String street;
    private String city;
    private String state;
    private String zip;
}