package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "patient_claim_documents")
@EqualsAndHashCode(callSuper = true)
public class PatientClaimDocument extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "claim_id", nullable = false)
    private PatientClaim claim;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Type type; // ATTACHMENT or EOB

    @Column(nullable = false) private String fileName;
    @Column(nullable = false) private String contentType;
    @Column(nullable = false) private long size;
    @Column(nullable = false) private String storageKey; // S3 key or filesystem path

    public enum Type { ATTACHMENT, EOB }

    // Delegating accessors for backward compatibility
    public Instant getCreatedAt() {
        if (getCreatedDate() == null) return null;
        return getCreatedDate().toInstant(ZoneOffset.UTC);
    }

    public void setCreatedAt(Instant createdAt) {
        if (createdAt == null) {
            setCreatedDate(null);
        } else {
            setCreatedDate(LocalDateTime.ofInstant(createdAt, ZoneOffset.UTC));
        }
    }

    // getters/setters…
}

