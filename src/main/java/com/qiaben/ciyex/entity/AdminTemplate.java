package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class AdminTemplate extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // templateId removed: IDs are now managed by the database `id` column

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

    // audit fields provided by AuditableEntity

    // Backwards-compatible accessors for existing code that expects createdAt/updatedAt
    public LocalDateTime getCreatedAt() { return getCreatedDate(); }
    public void setCreatedAt(LocalDateTime createdAt) { setCreatedDate(createdAt); }
    public LocalDateTime getUpdatedAt() { return getLastModifiedDate(); }
    public void setUpdatedAt(LocalDateTime updatedAt) { setLastModifiedDate(updatedAt); }
}
