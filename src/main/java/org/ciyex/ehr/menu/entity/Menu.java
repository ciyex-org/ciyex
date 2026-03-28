package org.ciyex.ehr.menu.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "menu")
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 50)
    private String location = "SIDEBAR";

    @Column(nullable = false)
    private int version = 1;

    @Column(nullable = false, length = 20)
    private String status = "PUBLISHED";

    @Column(name = "org_id", nullable = false, length = 100)
    private String orgId = "*";

    @Column(name = "practice_type_code", nullable = false, length = 100)
    private String practiceTypeCode = "*";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
