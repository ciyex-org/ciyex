package com.qiaben.ciyex.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingAutoPayDto {
    private Long id;
    private UUID userId;
    private Boolean enabled;
    private LocalDate startDate;
    private String frequency;
    private Double maxAmount;
    private Long cardId;
}
