//package com.qiaben.ciyex.dto;
//
//import lombok.Data;
//
//@Data
//public class ProviderSignatureDto {
//    private Long id;
//    private String externalId;      // optional - FHIR id if mirrored
//
//    private Long patientId;
//    private Long encounterId;
//
//    private String signedAt;        // ISO-8601
//    private String signedBy;        // practitioner user/email/id
//    private String signerRole;      // MD | NP | RN | BILLING | ADMIN
//
//    private String signatureType;   // ELECTRONIC | WET | SMARTCARD | PIN | OTP
//    private String signatureFormat; // image/png | application/pkcs7-signature | JWS
//    private String signatureData;   // base64
//    private String signatureHash;   // SHA-256 hex of canonical payload (optional)
//
//    private String status;          // active | revoked | expired
//    private String comments;
//
//    private Audit audit;
//    @Data
//    public static class Audit {
//        private String createdDate;      // yyyy-MM-dd
//        private String lastModifiedDate; // yyyy-MM-dd
//    }
//}

package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class ProviderSignatureDto {
    private Long id;
    private String externalId;
    private String fhirId;
    private Long patientId;
    private Long encounterId;

    private String signedAt;        // ISO or any human-readable string
    private String signedBy;
    private String signerRole;

    private String signatureType;   // e.g., "drawn", "typed"
    private String signatureFormat; // e.g., "image/png"
    private String signatureData;   // base64
    private String signatureHash;

    private String status;          // e.g., "SIGNED"
    private String comments;

    private Audit audit;
    @Data
    public static class Audit {
        private String createdDate;      // yyyy-MM-dd
        private String lastModifiedDate; // yyyy-MM-dd
    }
}
