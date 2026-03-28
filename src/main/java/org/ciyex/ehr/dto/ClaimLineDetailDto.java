package org.ciyex.ehr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for claim line details showing DOS, code, description, provider, and total submitted amount
 */
public record ClaimLineDetailDto(
        Long lineId,
        LocalDate dos,
        String code,
        String description,
        String provider,
        BigDecimal totalSubmittedAmount
) {}

