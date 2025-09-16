


package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class ProcedureDto {
    private Long id;
    private String externalId;           // optional FHIR id
    private Long orgId;
    private Long patientId;
    private Long encounterId;

    private String cpt4;                 // CPT code
    private String description;
    private Integer units;               // Unit(s)
    private String rate;                 // you can swap to BigDecimal if preferred

    private String relatedIcds;          // comma-separated ICD codes (or change to List<String>)
    private String hospitalBillingStart; // yyyy-MM-dd
    private String hospitalBillingEnd;   // yyyy-MM-dd

    private String modifier1;
    private String modifier2;
    private String modifier3;
    private String modifier4;

    private String note;

    private Audit audit;
    @Data
    public static class Audit {
        private String createdDate;       // yyyy-MM-dd
        private String lastModifiedDate;  // yyyy-MM-dd
    }
}
