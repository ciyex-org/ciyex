package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orgId;

    private String name;
    private String category;
    private String lot;
    private String expiry;
    private String sku;
    private Integer stock;
    private String unit;
    private Integer minStock;
    private String location;
    private String status;

    private String createdDate;
    private String lastModifiedDate;

    private String externalId; // FHIR / external system id
}
