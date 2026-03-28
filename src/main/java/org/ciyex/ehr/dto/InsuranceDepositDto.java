package org.ciyex.ehr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InsuranceDepositDto(
    Long id,
    Long patientId,
    Long policyId,
    BigDecimal depositAmount,
    LocalDate depositDate,
    String paymentMethod,
    String providerId,
    String description
) {}
