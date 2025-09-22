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
    private String category;
    private String type;
    private String fileName;
    private String contentType;
    private String description;
    private String fhirExternalId;
    private String createdDate;
    private String lastModifiedDate;

    // S3 details
    private String s3Bucket;
    private String s3Key;

    // Encryption metadata (Base64 encoded)
    @Column(length = 512)
    private String encryptionKey;
    private String iv;
}