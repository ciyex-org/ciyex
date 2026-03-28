package org.ciyex.ehr.settings.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ui_color_config")
public class UiColorConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private String orgId;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "entity_key", nullable = false, length = 200)
    private String entityKey;

    @Column(name = "entity_label", length = 300)
    private String entityLabel;

    @Column(name = "bg_color", nullable = false, length = 20)
    @Builder.Default
    private String bgColor = "#6B7280";

    @Column(name = "border_color", nullable = false, length = 20)
    @Builder.Default
    private String borderColor = "#4B5563";

    @Column(name = "text_color", nullable = false, length = 20)
    @Builder.Default
    private String textColor = "#FFFFFF";

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
