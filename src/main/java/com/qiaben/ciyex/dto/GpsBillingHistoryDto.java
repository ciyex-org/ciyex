package com.qiaben.ciyex.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GpsBillingHistoryDto {
    private Long id;
    private Long orgId;
    private Long userId;
    private String gpsTransactionId;
    private String gpsCustomerVaultId;
    private BigDecimal amount;
    private String status;
    private String responseMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Additional fields for payment processing
    private String firstName;
    private String lastName;
    private String street;
    private String city;
    private String state;
    private String zip;
}