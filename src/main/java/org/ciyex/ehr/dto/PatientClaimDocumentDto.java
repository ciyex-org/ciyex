package org.ciyex.ehr.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class PatientClaimDocumentDto {
    private Long id;
    private Long claimId;
    private String type;
    private String fileName;
    private String contentType;
    private long size;
    private String storageKey;
    private Instant createdAt;
}
