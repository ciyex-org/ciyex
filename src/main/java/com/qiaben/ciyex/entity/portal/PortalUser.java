package com.qiaben.ciyex.entity.portal;

import com.qiaben.ciyex.enums.PortalStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ✅ Entity representing users who register and log in through the patient portal.
 * Automatically approved on registration and linked to a PortalPatient record.
 */
@Entity
@Table(name = "portal_users") // Removed schema="public" (not needed for single schema setups)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PortalUser extends com.qiaben.ciyex.entity.AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "phone_number")
    private String phoneNumber;

    /** ✅ User account status (auto-approved on registration) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PortalStatus status = PortalStatus.APPROVED;

    /** ✅ Keycloak user ID (sub claim from Keycloak token) */
    @Column(name = "keycloak_user_id", unique = true)
    private String keycloakUserId;

    /** ✅ Audit and approval tracking */
    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "rejected_date")
    private LocalDateTime rejectedDate;

    @Column(name = "rejected_by")
    private Long rejectedBy;

    @Column(length = 500)
    private String reason; // Admin notes or rejection reason

    @Column(nullable = false, unique = true, updatable = false)
    @Builder.Default
    private UUID uuid = UUID.randomUUID();

    /** ✅ One-to-one link to portal patient */
    @OneToOne(mappedBy = "portalUser", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PortalPatient portalPatient;
}
