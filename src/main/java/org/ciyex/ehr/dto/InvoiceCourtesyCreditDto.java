package org.ciyex.ehr.dto;

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
}

