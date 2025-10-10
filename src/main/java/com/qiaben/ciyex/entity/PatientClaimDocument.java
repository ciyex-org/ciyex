package com.qiaben.ciyex.entity;



import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "patient_claim_documents")
public class PatientClaimDocument {
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
    @Column(nullable = false) private Instant createdAt = Instant.now();

    public enum Type { ATTACHMENT, EOB }

    // getters/setters…
}

