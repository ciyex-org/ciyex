package com.qiaben.ciyex.entity.portal.entity;

import jakarta.persistence.*;
import lombok.*;

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

    /** 🔹 Unique email for login */
    @Column(nullable = false, unique = true)
    private String email;

    /** 🔹 Encrypted password */
    @Column(nullable = false)
    private String password;

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

    /** 🔹 Unique identifier for external integrations */
    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    /** 🔹 Associated organization (patient chooses on signup) */
    @Column(name = "org_id", nullable = false)
    private Long orgId;

    /** 🔹 Always PATIENT role inside portal */
    @Column(nullable = false, length = 50)
    private String role;

    /** 🔹 Default handling */
    @PrePersist
    public void prePersist() {
        if (this.role == null) {
            this.role = "PATIENT";
        }
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID();
        }
    }
}
