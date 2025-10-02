package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
