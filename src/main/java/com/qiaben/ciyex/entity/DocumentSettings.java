package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "document_settings",
        uniqueConstraints = @UniqueConstraint(name = "uq_document_settings_org", columnNames = "org_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class DocumentSettings extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    

    @Column(name = "max_upload_bytes", nullable = false)
    private long maxUploadBytes;

    @Column(name = "enable_audio", nullable = false)
    private boolean enableAudio;

    // NEW: encryption flag
    @Column(name = "encryption_enabled", nullable = false)
    private boolean encryptionEnabled;  // default false

    // JSON array of strings ["JPG","PNG","PDF"]
    @Column(name = "allowed_file_types_json", columnDefinition = "TEXT", nullable = false)
    private String allowedFileTypesJson;

    // JSON array of objects [{ "name":"Advance Directive", "active":true }]
    @Column(name = "categories_json", columnDefinition = "TEXT", nullable = false)
    private String categoriesJson;

    private String updatedBy;
    private Instant updatedAt;
}
