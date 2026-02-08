package org.ciyex.ehr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PatientDepositDto(
        Long id,
        Long patientId,
        BigDecimal amount,
        LocalDate depositDate,
        String description,
        String paymentMethod
) {}

