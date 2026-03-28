package org.ciyex.ehr.dto;

import lombok.Data;

@Data
public class SlotDto {
    private Long id;          // local DB id
    private String tenantName;       // tenant/org
    private Long providerId;  // practitioner
    private String externalId;// FHIR Slot id
    private String fhirId;    // FHIR ID (same as externalId)

    private String start;     // ISO-8601 datetime
    private String end;       // ISO-8601 datetime
    private String status;    // free | busy | busy-unavailable | busy-tentative
    private String comment;   // optional notes

    private Audit audit;

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}
