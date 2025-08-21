package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class SignoffDto {
    private Long id;
    private String externalId;          // optional FHIR id (Provenance/Composition.attester)
    private Long orgId;
    private Long patientId;
    private Long encounterId;

    // What is being signed off
    private String targetType;          // NOTE | REPORT | PLAN | CLAIM | INVOICE | OTHER
    private Long   targetId;            // ID in your DB for the target resource
    private String targetVersion;       // optional version string of target

    // Who/when/how
    private String status;              // draft | in_review | finalized | amended | revoked
    private String signedBy;            // Practitioner/username
    private String signerRole;          // MD | RN | ADMIN | BILLING etc
    private String signedAt;            // ISO-8601 date-time (Z or offset)
    private String signatureType;       // ELECTRONIC | WET | PIN | SMARTCARD
    private String signatureData;       // optional Base64 or CMS/JOSE blob (truncate at UI)

    // Integrity/attestation
    private String contentHash;         // SHA-256 hex of the finalized content
    private String attestationText;     // "I attest that..."
    private String comments;

    private Audit audit;
    @Data
    public static class Audit {
        private String createdDate;      // yyyy-MM-dd
        private String lastModifiedDate; // yyyy-MM-dd
    }
}
