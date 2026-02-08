package org.ciyex.ehr.dto;

import java.math.BigDecimal;

public record PatientAccountCreditDto(
        Long patientId,
        BigDecimal balance
) {}

