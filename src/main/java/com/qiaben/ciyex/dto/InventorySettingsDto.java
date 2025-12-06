package com.qiaben.ciyex.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventorySettingsDto {
    private Long id;
    private boolean lowStockAlerts;
    private boolean autoReorderSuggestions;
    private int criticalLowPercentage;

    // FHIR/external identifier (auto-generated)
    private String fhirId;

    // Audit dates from AuditableEntity
    private Audit audit;

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}
