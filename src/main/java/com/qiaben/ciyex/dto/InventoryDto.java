package com.qiaben.ciyex.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryDto {
    private Long id;

    @NotBlank(message = "Name is mandatory")
    private String name;       // e.g., "Syringes 5ml"

    @NotBlank(message = "Category is mandatory")
    private String category;   // "Consumable" | "Device"

    @NotBlank(message = "Lot is mandatory")
    private String lot;        // LOT- / SN-

    private String expiry;     // ISO date string (yyyy-MM-dd)

    @NotBlank(message = "SKU is mandatory")
    private String sku;        // e.g., "SYR-5-500"

    @NotNull(message = "Stock is mandatory")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock;     // qty on hand

    @NotBlank(message = "Unit is mandatory")
    private String unit;       // pcs / box / pair / bottle / roll

    @NotNull(message = "Min stock is mandatory")
    @Min(value = 0, message = "Min stock cannot be negative")
    private Integer minStock;  // reorder threshold

    @NotBlank(message = "Location is mandatory")
    private String location;   // clinic/location name

    private String status;     // "Active" | "Inactive"

    @NotBlank(message = "Supplier is mandatory")
    private String supplier;  // supplier name

    private Audit audit;
    private String fhirId;     // externalId

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}
