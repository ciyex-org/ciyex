package org.ciyex.ehr.menu.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "menu_org_override")
public class MenuOrgOverride {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private String orgId;

    @Column(name = "menu_code", nullable = false)
    private String menuCode = "ehr-sidebar";

    @Column(name = "item_id")
    private UUID itemId;

    @Column(nullable = false)
    private String action; // hide, modify, reorder, add

    @Column(columnDefinition = "jsonb")
    private String data;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
