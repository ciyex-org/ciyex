package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class SlotDto {
    private Long id; // Database ID
    private String externalId; // ID from external storage (e.g., FHIR Slot ID)
    private Long orgId; // Tenant identifier
    private Long providerId;
    private Long locationId;
    private String startTime;
    private String endTime;
    private String status;
    private Audit audit;

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}