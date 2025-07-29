package com.qiaben.ciyex.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_org_roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserOrgRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference("user-userOrgRoles")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    @JsonBackReference("org-userOrgRoles")
    private Org org;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    private RoleName role;
}
