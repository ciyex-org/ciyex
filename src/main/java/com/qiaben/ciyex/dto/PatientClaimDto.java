package com.qiaben.ciyex.dto;

import com.qiaben.ciyex.entity.PatientClaim.Status;
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
        Status status,
        int attachments,
        boolean eobAttached,
        LocalDate createdOn,
        boolean hasAttachment,
        boolean hasEob
) {}
