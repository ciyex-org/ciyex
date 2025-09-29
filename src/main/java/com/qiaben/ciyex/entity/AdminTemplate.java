package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "admin_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "template_id", unique = true)
    private String templateId;

    @PrePersist
    public void ensureTemplateId() {
        if (this.templateId == null || this.templateId.trim().isEmpty()) {
            // create a short, readable id with low collision risk
            this.templateId = "TPL-" + UUID.randomUUID().toString()
                    .replaceAll("-", "")
                    .substring(0, 8)
                    .toUpperCase();
        }
    }

    @Column(name = "locations", nullable = false)
    private String locations;

    public void setLocations(String value) {
        if (value != null && value.startsWith("classpath:")) {
            this.locations = value; // keep as-is
        } else {
            this.locations = value;
        }
    }

    @Column(name = "practice_type", nullable = false)
    private String practiceType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
