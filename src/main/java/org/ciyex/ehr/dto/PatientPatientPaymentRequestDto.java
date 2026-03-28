package org.ciyex.ehr.dto;

import java.util.List;

public record PatientPatientPaymentRequestDto(
        String paymentMethod, // "CREDIT_CARD", "CHECK", etc.
        List<PatientPatientPaymentAllocationDto> allocations
) {}
