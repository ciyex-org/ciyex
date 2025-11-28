package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class OrderDto {
    private Long id;
    private String orderNumber;
    private String supplier;
    private String date;
    private String status;
    private Integer stock;
    private String itemName;
    private Double amount;
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
