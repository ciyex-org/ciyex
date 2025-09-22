package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class InventoryDto {
    private Long id;
    private Long orgId;

    private String name;       // e.g., "Syringes 5ml"
    private String category;   // "Consumable" | "Device"
    private String lot;        // LOT- / SN-
    private String expiry;     // ISO date string (yyyy-MM-dd)
    private String sku;        // e.g., "SYR-5-500"
    private Integer stock;     // qty on hand
    private String unit;       // pcs / box / pair / bottle / roll
    private Integer minStock;  // reorder threshold
    private String location;   // clinic/location name
    private String status;     // "Active" | "Inactive"
    private String supplier;  // supplier name


    private Audit audit;
    private String fhirId;     // externalId

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}
