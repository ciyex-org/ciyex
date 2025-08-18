package com.qiaben.ciyex.dto;

import lombok.Data;

import java.util.List;

@Data
public class ReviewOfSystemDto {

    private Long id;
    private Long patientId;
    private Long encounterId;
    private String systemName;
    private boolean isNegative;
    private String notes;
    private List<String> systemDetails; // List of symptoms or conditions in the review
    private String createdDate;
    private String lastModifiedDate;
}
