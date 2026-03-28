package org.ciyex.ehr.tabconfig.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "tab_field_config")
public class TabFieldConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tab_key", nullable = false)
    private String tabKey;

    @Column(name = "practice_type_code", nullable = false)
    private String practiceTypeCode = "*";

    @Column(name = "org_id", nullable = false)
    private String orgId = "*";

    @Column(name = "fhir_resources", columnDefinition = "jsonb", nullable = false)
    private String fhirResources = "[]";

    @Column(name = "field_config", columnDefinition = "jsonb", nullable = false)
    private String fieldConfig = "{}";

    // Layout columns (unified from tab_config)
    @Column(name = "label")
    private String label;

    @Column(name = "icon")
    private String icon = "FileText";

    @Column(name = "category")
    private String category = "Other";

    @Column(name = "category_position")
    private Integer categoryPosition = 0;

    @Column(name = "position")
    private Integer position = 0;

    @Column(name = "visible")
    private Boolean visible = true;

    @Column(name = "api_base_path")
    private String apiBasePath;

    @Column(name = "required_permission")
    private String requiredPermission;

    @Column(nullable = false)
    private int version = 1;

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
