package com.qiaben.ciyex.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GlobalCodeDto {
    private Long id;
    private String externalId; // optional        // tenant

    @NotBlank(message = "Code type is required")
    private String codeType;   // ICD9 | ICD10 | CPT4 | HCPCS | CUSTOM

    @NotBlank(message = "Code is required")
    private String code;       // e.g. I10, 99214

    private String modifier;
    private Boolean active;

    private String description;
    private String shortDescription;
    private String category;

    private Boolean diagnosisReporting;
    private Boolean serviceReporting;

    private String relateTo;
    private BigDecimal feeStandard;

    private Audit audit;

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}
