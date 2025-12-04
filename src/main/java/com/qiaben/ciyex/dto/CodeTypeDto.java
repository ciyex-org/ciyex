package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class CodeTypeDto {
    private Long id;
    private String externalId;
    private String fhirId;
    private Long patientId;
    private Long encounterId;

    private String codeTypeKey;
    private Integer codeTypeId;
    private Integer sequenceNumber;
    private Integer modifier;
    private String justification;
    private String mask;
    private Boolean feeApplicable;
    private Boolean relatedIndicator;
    private Boolean numberOfServices;
    private Boolean diagnosisFlag;
    private Boolean active;
    private String label;
    private Boolean externalFlag;
    private Boolean claimFlag;
    private Boolean procedureFlag;
    private Boolean terminologyFlag;
    private Boolean problemFlag;
    private Boolean drugFlag;

    private Audit audit;

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}
