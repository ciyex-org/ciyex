package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Base class for auditable entities.
 * Provides automatic tracking of:
 * - Creation timestamp
 * - Last modification timestamp
 * - User who created the entity
 * - User who last modified the entity
 * - Tenant name (auto-populated from RequestContext)
 */
@MappedSuperclass
@EntityListeners({AuditingEntityListener.class, TenantAuditListener.class})
@Getter
@Setter
public abstract class AuditableEntity {

    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(name = "last_modified_date", nullable = false)
    private LocalDateTime lastModifiedDate;

    @CreatedBy
    @Column(name = "created_by", length = 100, updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "last_modified_by", length = 100)
    private String lastModifiedBy;

    /**
     * Tenant name - automatically populated from RequestContext
     * This field is set by TenantAuditListener before persist and update operations
     */
    @Column(name = "tenant_name", nullable = false, length = 100, updatable = false)
    private String tenantName;

    @PrePersist
    protected void prePersist() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        if (lastModifiedDate == null) {
            lastModifiedDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void preUpdate() {
        lastModifiedDate = LocalDateTime.now();
    }
}
