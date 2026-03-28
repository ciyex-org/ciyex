package org.ciyex.ehr.usermgmt.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Data @Entity @Table(name = "role_permission_config")
@Builder @NoArgsConstructor @AllArgsConstructor
public class RolePermissionConfig {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;

    @Column(name = "role_label", nullable = false, length = 100)
    private String roleLabel;

    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> permissions;

    @Column(name = "is_system")
    private Boolean isSystem;

    @Column(name = "is_active")
    private Boolean isActive;

    /** SMART on FHIR API-level scopes (e.g. "SCOPE_user/Coverage.write"). Org-customisable. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "smart_scopes", columnDefinition = "jsonb")
    private List<String> smartScopes;

    @Column(name = "org_alias", nullable = false, length = 100)
    private String orgAlias;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 100;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}
