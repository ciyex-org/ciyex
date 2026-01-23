package com.qiaben.ciyex.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderDto {
    private Long id;
    
    @NotBlank(message = "Order number is required")
    private String orderNumber;
    
    @NotBlank(message = "Supplier is required")
    private String supplier;
    
    private String date;
    
    @NotBlank(message = "Status is required")
    private String status;
    
    private Integer stock;
    
    @NotBlank(message = "Item name is required")
    private String itemName;
    
    private Double amount;
    
    @NotBlank(message = "Category is required")
    private String category;

    private Audit audit;
    private String externalId;
    private String fhirId;

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}
