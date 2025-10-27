package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EqualsAndHashCode(callSuper = true)
public class Inventory extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String name;
    private String category;
    private String lot;
    private String expiry;
    private String sku;
    private Integer stock;       // current stock on hand
    private String unit;
    private Integer minStock;    // minimum required stock
    private String location;
    private String status;       // e.g., OK, LOW, CRITICAL, EXPIRED
    private String supplier;  // supplier name


    // audit fields provided by AuditableEntity

    private String externalId;   // FHIR / external system id

    // 🔗 Orders placed for this inventory item
    @OneToMany(mappedBy = "inventory", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders;
}
