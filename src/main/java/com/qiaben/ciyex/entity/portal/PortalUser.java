package com.qiaben.ciyex.entity.portal;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "portal_users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    /** 🔹 One-to-one link to PortalPatient */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private PortalPatient patient;

    private String firstName;
    private String lastName;
    private String middleName;
    private LocalDate dateOfBirth;
    private String phoneNumber;

    private String city;
    private String state;
    private String country;
    private String street;
    private String street2;
    private String postalCode;
    private String profileImage;

    private String securityQuestion;
    private String securityAnswer;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(nullable = false, length = 50)
    private String role;

    @PrePersist
    public void prePersist() {
        if (this.role == null) this.role = "PATIENT";
        if (this.uuid == null) this.uuid = UUID.randomUUID();
    }
}
