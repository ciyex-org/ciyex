package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class SlotDto {
    private Long id;          // local DB id
    private Long orgId;       // tenant/org
    private Long providerId;  // practitioner
    private String externalId;// FHIR Slot id

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
