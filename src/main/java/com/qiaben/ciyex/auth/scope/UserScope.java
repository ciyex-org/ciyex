package com.qiaben.ciyex.auth.scope;

import jakarta.persistence.*;

@Entity
@Table(
        name = "auth_user_scope",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_scope", columnNames = {"user_id","scope_id"})
)
public class UserScope {

    public enum GrantSource { EXPLICIT, TEMPLATE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Keep a simple FK by ID to avoid coupling to your User entity class name */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "scope_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_scope_scope"))
    private Scope scope;

    @Column(nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GrantSource source = GrantSource.EXPLICIT;

    // getters/setters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Scope getScope() { return scope; }
    public void setScope(Scope scope) { this.scope = scope; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public GrantSource getSource() { return source; }
    public void setSource(GrantSource source) { this.source = source; }
}
