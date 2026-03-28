package org.ciyex.ehr.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvItemDto {
    private Long id;

    @NotBlank(message = "Name is mandatory")
    private String name;

    @NotBlank(message = "SKU is mandatory")
    private String sku;

    private String description;

    @NotBlank(message = "Unit is mandatory")
    private String unit;

    private BigDecimal costPerUnit;

    @NotNull(message = "Stock is mandatory")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stockOnHand;

    @NotNull(message = "Min stock is mandatory")
    @Min(value = 0, message = "Min stock cannot be negative")
    private Integer minStock;

    private Integer maxStock;
    private Integer reorderPoint;
    private Integer reorderQty;

    private String status;
    private String itemType;
    private String barcode;
    private String manufacturer;
    private String costMethod;

    // Category (flattened)
    private Long categoryId;
    private String categoryName;

    // Location (flattened)
    private Long locationId;
    private String locationName;

    // Supplier (flattened)
    private Long supplierId;
    private String supplierName;

    private String createdAt;
    private String updatedAt;
}
