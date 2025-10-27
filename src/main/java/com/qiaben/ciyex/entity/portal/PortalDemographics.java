package com.qiaben.ciyex.entity.portal;

import com.qiaben.ciyex.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Entity
@Table(name = "portal_demographics")
@Data
@EqualsAndHashCode(callSuper = true)
public class PortalDemographics extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identity
    private String firstName;
    private String middleName;
    private String lastName;
    private LocalDate dob;
    private String sex;
    private String maritalStatus;

    // Contact
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String phoneMobile;
    private String contactEmail;

    // Emergency
    private String emergencyContactName;
    private String emergencyContactPhone;

    // Preferences
    private boolean allowSMS;
    private boolean allowEmail;
    private boolean allowVoiceMessage;
    private boolean allowMailMessage;

    @OneToOne
    @JoinColumn(name = "patient_id", referencedColumnName = "id")
    private PortalPatient patient;
}

