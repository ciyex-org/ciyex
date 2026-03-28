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
@Table(name = "inventory_settings")
@FilterDef(name = "orgAliasFilter", parameters = @ParamDef(name = "orgAlias", type = String.class))
@Filter(name = "orgAliasFilter", condition = "org_alias = :orgAlias")
public class InvSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "low_stock_alerts", nullable = false)
    @Builder.Default
    private Boolean lowStockAlerts = true;

    @Column(name = "auto_reorder", nullable = false)
    @Builder.Default
    private Boolean autoReorder = false;

    @Column(name = "critical_low_pct", nullable = false)
    @Builder.Default
    private Integer criticalLowPct = 10;

    @Column(name = "default_cost_method", length = 10)
    @Builder.Default
    private String defaultCostMethod = "fifo";

    @Column(name = "po_approval_required", nullable = false)
    @Builder.Default
    private Boolean poApprovalRequired = false;

    @Column(name = "po_approval_threshold", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal poApprovalThreshold = BigDecimal.ZERO;

    @Column(name = "org_alias", nullable = false, unique = true, length = 100)
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
