package com.qiaben.ciyex.entity.portal;

import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * Entity representing detailed patient information for portal users
 * Stored in public schema, linked to PortalUser
 */
@Entity
@Table(name = "portal_patients", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PortalPatient extends com.qiaben.ciyex.entity.AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne
    @JoinColumn(name = "portal_user_id", nullable = false, unique = true)
    private PortalUser portalUser;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(length = 10)
    private String gender;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(length = 100)
    private String city;

    @Column(length = 50)
    private String state;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(length = 50)
    @Builder.Default
    private String country = "USA";

    @Column(name = "emergency_contact_name")
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;

    @Column(name = "emergency_contact_relationship", length = 100)
    private String emergencyContactRelationship;

    @Column(name = "medical_record_number", length = 50)
    private String medicalRecordNumber;

    @Column(name = "ehr_patient_id")
    private Long ehrPatientId; // Links to tenant schema patient after approval

    // audit fields provided by AuditableEntity
}