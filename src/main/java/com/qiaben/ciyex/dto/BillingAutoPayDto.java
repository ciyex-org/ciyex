package com.qiaben.ciyex.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingAutoPayDto {
    private Long id;
    private Long userId;
    private Boolean enabled;
    private LocalDate startDate;
    private String frequency;
    private Double maxAmount;
    private Long cardId;
}
