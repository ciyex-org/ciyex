package com.qiaben.ciyex.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientRelationshipDto {
    
    private Long id;
    private Long patientId;
    private Long relatedPatientId;
    private String relatedPatientName;
    private String relationshipType;
    private String phoneNumber;
    private String email;
    private String address;
    private Boolean emergencyContact;
    private String notes;
    private Boolean active;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
}
