package com.qiaben.ciyex.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class AssignedProviderDto {
    private Long id;
    private String externalId;
    private String fhirId;  // FHIR CareTeam resource ID (same as externalId)
    private Long patientId;
    private Long encounterId;

    private Long providerId;
    private String role;
    private String startDate;
    private String endDate;
    private String status;
    private String notes;

    // eSign / Print (server-managed)
    private Boolean eSigned;
    private OffsetDateTime signedAt;
    private String signedBy;
    private OffsetDateTime printedAt;

    private Audit audit;

    @Data
    public static class Audit {
        private String createdDate;       // yyyy-MM-dd
        private String lastModifiedDate;  // yyyy-MM-dd
    }
}
