package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "patients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "org_id", nullable = false)
    private Long orgId; // Tenant identifier

    private String firstName;
    private String lastName;
    private String middleName;
    private String gender;
    private String dateOfBirth;
    private String phoneNumber;
    private String email;
    private String address;
    private String medicalRecordNumber; // Unique identifier for the patient
    private String createdDate;
    private String lastModifiedDate;
}