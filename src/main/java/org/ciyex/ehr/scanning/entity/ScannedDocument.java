package org.ciyex.ehr.scanning.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "scanned_documents")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScannedDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String originalFileName;
    private Long fileSize;
    private String mimeType;

    @Column(columnDefinition = "TEXT")
    private String fileUrl;

    @Column(columnDefinition = "TEXT")
    private String storageKey;

    private Long patientId;
    private String patientName;
    private String category;
    private LocalDate documentDate;

    @Column(columnDefinition = "TEXT")
    private String ocrText;

    private String ocrStatus;
    private Double ocrConfidence;

    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private String uploadedBy;
    private String orgAlias;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = updatedAt = LocalDateTime.now();
        if (ocrStatus == null) ocrStatus = "pending";
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
