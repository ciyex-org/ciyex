package com.qiaben.ciyex.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CodeDto {
    private Long id;
    private String externalId;       // optional (remote/FHIR id)
    private Long orgId;              // tenant
    private Long patientId;
    private Long encounterId;

    // core
    private String codeType;         // ICD9 | ICD10 | CPT4 | HCPCS | CUSTOM
    private String code;             // e.g., I10, 99214
    private String modifier;         // e.g., 25, 59
    private Boolean active;

    private String description;      // long text
    private String shortDescription; // short label
    private String category;         // UI grouping

    private Boolean diagnosisReporting;
    private Boolean serviceReporting;

    private String relateTo;         // arbitrary relation / tag
    private BigDecimal feeStandard;  // nullable

    private Audit audit;
    @Data
    public static class Audit {
        private String createdDate;      // yyyy-MM-dd
        private String lastModifiedDate; // yyyy-MM-dd
    }
}
