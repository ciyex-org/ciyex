package com.qiaben.ciyex.entity.portal;

import com.qiaben.ciyex.enums.PortalStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing portal users who register from the public portal
 * Stored in public schema before approval
 */
@Entity
@Table(name = "portal_users", schema = "public")
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PortalStatus status = PortalStatus.PENDING;

    

    @Column(length = 500)
    private String reason; // Rejection reason or admin notes

    // audit fields provided by AuditableEntity

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "rejected_date")
    private LocalDateTime rejectedDate;

    @Column(name = "rejected_by")
    private Long rejectedBy;

    @Column(nullable = false, unique = true, updatable = false)
    @Builder.Default
    private UUID uuid = UUID.randomUUID();

    // One-to-one relationship with PortalPatient
    @OneToOne(mappedBy = "portalUser", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PortalPatient portalPatient;

    // Last-modified handled by AuditableEntity (@LastModifiedDate)
}
