package org.ciyex.ehr.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "inventory_item")
@FilterDef(name = "orgAliasFilter", parameters = @ParamDef(name = "orgAlias", type = String.class))
@Filter(name = "orgAliasFilter", condition = "org_alias = :orgAlias")
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 100)
    private String sku;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private InventoryCategory category;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String unit = "pcs";

    @Column(name = "cost_per_unit", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal costPerUnit = BigDecimal.ZERO;

    @Column(name = "stock_on_hand", nullable = false)
    @Builder.Default
    private Integer stockOnHand = 0;

    @Column(name = "min_stock", nullable = false)
    @Builder.Default
    private Integer minStock = 0;

    @Column(name = "max_stock")
    private Integer maxStock;

    @Column(name = "reorder_point", nullable = false)
    @Builder.Default
    private Integer reorderPoint = 0;

    @Column(name = "reorder_qty")
    private Integer reorderQty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private InventoryLocation location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private InvSupplier supplier;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "active";

    @Column(name = "item_type", nullable = false, length = 30)
    @Builder.Default
    private String itemType = "consumable";

    @Column(length = 100)
    private String barcode;

    @Column(length = 200)
    private String manufacturer;

    @Column(name = "cost_method", length = 10)
    @Builder.Default
    private String costMethod = "fifo";

    @Column(name = "org_alias", nullable = false, length = 100)
    private String orgAlias;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
