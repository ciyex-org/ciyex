package com.qiaben.ciyex.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GpsPaymentDto {
    private Long id;
    private Long orgId;
    private Long userId;
    private Long cardId;
    private String gpsTransactionId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
