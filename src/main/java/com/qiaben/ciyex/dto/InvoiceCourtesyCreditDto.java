package com.qiaben.ciyex.dto;

import com.qiaben.ciyex.entity.InvoiceCourtesyCredit;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InvoiceCourtesyCreditDto(
    Long id,
    Long patientId,
    Long invoiceId,
    String adjustmentType,
    BigDecimal amount,
    String description,
    Boolean isActive,
    LocalDateTime createdAt,
    String createdBy,
    LocalDateTime lastModifiedAt,
    String lastModifiedBy
) {
    public static InvoiceCourtesyCreditDto from(InvoiceCourtesyCredit entity) {
        return new InvoiceCourtesyCreditDto(
            entity.getId(),
            entity.getPatientId(),
            entity.getInvoiceId(),
            entity.getAdjustmentType(),
            entity.getAmount(),
            entity.getDescription(),
            entity.getIsActive(),
            entity.getCreatedDate(),
            entity.getCreatedBy(),
            entity.getLastModifiedDate(),
            entity.getLastModifiedBy()
        );
    }
}

