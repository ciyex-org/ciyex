package org.ciyex.ehr.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;
///
@Data
public class InvoiceDto {
    private Long id;
    private String externalId;      // FHIR Invoice id (optional)
    private Long patientId;
    private Long encounterId;

    @NotBlank(message = "Invoice number is mandatory")
    private String invoiceNumber;

    @NotBlank(message = "Status is mandatory")
    private String status;          // draft|issued|balanced|cancelled|entered-in-error

    @NotBlank(message = "Currency is mandatory")
    private String currency;        // USD

    @NotBlank(message = "Issue date is mandatory")
    private String issueDate;       // yyyy-MM-dd

    @NotBlank(message = "Due date is mandatory")
    private String dueDate;         // yyyy-MM-dd

    @NotBlank(message = "Payer is mandatory")
    private String payer;           // person/org to bill

    private String notes;

    private String totalGross;      // string for transport; BigDecimal in entity
    private String totalNet;

    @Valid
    private List<LineDto> lines;    // charge lines (optional)

    private List<PaymentDto> payments; // payments applied (optional)

    private Audit audit;

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }

    @Data
    public static class LineDto {
        private Long id;

        @NotBlank(message = "Description is mandatory")
        private String description;

        @NotNull(message = "Quantity is mandatory")
        private Integer quantity;

        @NotBlank(message = "Unit price is mandatory")
        private String unitPrice;

        @NotBlank(message = "Amount is mandatory")
        private String amount;     // quantity * price - adj

        @NotBlank(message = "Code is mandatory")
        private String code;       // optional code
    }

    @Data
    public static class PaymentDto {
        private Long id;
        private String date;       // yyyy-MM-dd
        private String amount;
        private String method;     // cash|card|eft|adjustment
        private String reference;  // txn id / check #
        private String note;
    }
}
