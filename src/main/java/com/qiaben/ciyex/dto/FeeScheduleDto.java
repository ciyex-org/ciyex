

package com.qiaben.ciyex.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class FeeScheduleDto {
    private Long id;
    private String externalId;     // optional (remote/FHIR id)
    private Long orgId;
    private Long patientId;
    private Long encounterId;

    private String name;           // e.g., "Encounter Fee Schedule"
    private String payer;          // optional payer/plan
    private String currency;       // "USD"
    private String effectiveFrom;  // yyyy-MM-dd
    private String effectiveTo;    // yyyy-MM-dd
    private String status;         // active | inactive | archived
    private String notes;          // TEXT

    private List<FeeScheduleEntryDto> entries; // for reads

    @Data
    public static class FeeScheduleEntryDto {
        private Long id;
        private Long scheduleId;     // parent id (filled on read)

        private String codeType;     // ICD9 | ICD10 | CPT4 | HCPCS | CUSTOM
        private String code;         // e.g., 99214 / I10
        private String modifier;     // e.g., 25 (optional)
        private String description;  // optional

        private String unit;         // "visit" / "unit" (optional)
        private String currency;     // "USD"
        private BigDecimal amount;   // 239.00
        private Boolean active;      // default true
        private String notes;        // TEXT
    }

    @Data
    public static class Audit {
        private String createdDate;      // yyyy-MM-dd
        private String lastModifiedDate; // yyyy-MM-dd
    }

    private Audit audit;
}
