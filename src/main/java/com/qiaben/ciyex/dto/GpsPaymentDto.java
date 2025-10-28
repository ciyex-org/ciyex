package com.qiaben.ciyex.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GpsPaymentDto {
    private Long id;
    private Long orgId;
    private Long userId;

    /** Numeric card identifier (GPS style) */
    private Long cardId;

    /** String-based card reference (Stripe style, e.g., "card_1234") */
    private String cardRef;

    /** GPS transaction ID returned from gateway */
    private String gpsTransactionId;

    private BigDecimal amount;
    private String status;       // e.g. SUCCESS, FAILED, PENDING
    private String currency;
        private String receiptUrl;
    private String description;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
