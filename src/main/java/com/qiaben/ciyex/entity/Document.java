package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orgId;
    private Long patientId;
    private String category; // e.g., "medical_records"
    private String type; // e.g., "lab_report"
    private String fileName;
    private String contentType; // Original content type
    private String fhirExternalId;
    private String createdDate;
    private String lastModifiedDate;
}