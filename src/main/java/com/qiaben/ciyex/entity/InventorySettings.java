package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class InventorySettings extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private boolean lowStockAlerts;
    private boolean autoReorderSuggestions;
    private int criticalLowPercentage;

    /**
     * Persisted external identifier (FHIR-equivalent) for this settings record.
     */
    @Column(length = 128)
    private String externalId;

    // audit fields provided by AuditableEntity
}
