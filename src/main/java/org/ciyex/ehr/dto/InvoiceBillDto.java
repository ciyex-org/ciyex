package org.ciyex.ehr.dto;

import org.ciyex.ehr.dto.InvoiceStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceBillDto {
    private Long id;
    private UUID userId;
    private Long subscriptionId;

    // Use BigDecimal for money
    private BigDecimal amount;
    private InvoiceStatus status;

    private String externalId;
    private String fhirId;
    private String invoiceNumber;
    private String invoiceUrl;
    private String receiptUrl;

    // Serialize dates without time component (date-only)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDateTime dueDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDateTime paidAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDateTime updatedAt;
}
