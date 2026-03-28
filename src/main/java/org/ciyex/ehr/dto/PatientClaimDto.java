package org.ciyex.ehr.dto;

import java.time.LocalDate;

public record PatientClaimDto(
        Long id,
        Long invoiceId,
        Long patientId,
        String payerName,
        String treatingProviderId,
        String billingEntity,
        String type,
        String notes,
        PatientClaimStatus status,
        int attachments,
        boolean eobAttached,
        LocalDate createdOn,
        boolean hasAttachment,
        boolean hasEob,
        String patientName,
        String planName,
        String provider,
        String policyNumber,
        String diagnosisCode

) {}
