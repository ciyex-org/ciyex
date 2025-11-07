package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "patient_relationships")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientRelationship {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "org_id", nullable = false)
    private Long orgId;
    
    @Column(name = "patient_id", nullable = false)
    private Long patientId;
    
    @Column(name = "related_patient_id")
    private Long relatedPatientId;
    
    @Column(name = "related_patient_name")
    private String relatedPatientName;
    
    @Column(name = "relationship_type", nullable = false)
    private String relationshipType;
    
    @Column(name = "phone_number")
    private String phoneNumber;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "address")
    private String address;
    
    @Column(name = "is_emergency_contact")
    private Boolean emergencyContact;
    
    @Column(name = "notes")
    private String notes;
    
    @Column(name = "active")
    private Boolean active;
    
    @Column(name = "created_date")
    private LocalDateTime createdDate;
    
    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;
}
