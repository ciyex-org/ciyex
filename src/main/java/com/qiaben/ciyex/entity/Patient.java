package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "patients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Patient extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "status")
    private String status;

     // Tenant identifier

    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
    private String lastName;
    @Column(name = "middle_name")
    private String middleName;
    private String gender;
    @Column(name = "date_of_birth")
    private String dateOfBirth;
    @Column(name = "phone_number")
    private String phoneNumber;
    private String email;
    private String address;
    @Column(name = "medical_record_number")
    private String medicalRecordNumber; // Unique identifier for the patient
    // audit fields provided by AuditableEntity
}

