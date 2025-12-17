package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class PatientHistoryDto {
    private Long id;
    private Long patientId;
    private Object historyData;

    private String externalId;
    private String fhirId;
    private String createdDate;
    private String lastModifiedDate;
    private String createdBy;
    private String lastModifiedBy;

}