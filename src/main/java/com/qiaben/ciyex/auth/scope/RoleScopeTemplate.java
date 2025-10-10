package com.qiaben.ciyex.auth.scope;

import com.qiaben.ciyex.entity.RoleName; // ✅ use your real role enum
import jakarta.persistence.*;

@Entity
@Table(name = "auth_role_scope_template",
        uniqueConstraints = @UniqueConstraint(name = "uk_role_scope", columnNames = {"role","scope_id"}))
public class RoleScopeTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RoleName role; // ✅ not RoleKey

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "scope_id", nullable = false, foreignKey = @ForeignKey(name = "fk_role_scope_scope"))
    private Scope scope;

    public Long getId() { return id; }
    public RoleName getRole() { return role; }
    public void setRole(RoleName role) { this.role = role; }
    public Scope getScope() { return scope; }
    public void setScope(Scope scope) { this.scope = scope; }
}
