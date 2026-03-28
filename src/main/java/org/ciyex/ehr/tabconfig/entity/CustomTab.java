package org.ciyex.ehr.tabconfig.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "custom_tab")
public class CustomTab {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private String orgId;

    @Column(name = "tab_key", nullable = false)
    private String tabKey;

    @Column(nullable = false)
    private String label;

    private String icon = "FileText";
    private String category = "Other";

    @Column(name = "form_schema", columnDefinition = "jsonb", nullable = false)
    private String formSchema = "{}";

    private int position;
    private boolean active = true;

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
