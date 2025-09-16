//package com.qiaben.ciyex.dto;
//
//import lombok.Data;
//
//@Data
//public class PatientMedicalHistoryDto {
//    private Long id;                // DB id (for reads)
//    private String externalId;      // FHIR id (optional)
//    private Long orgId;             // tenant
//    private Long patientId;
//    private Long encounterId;
//
//    private String description;     // the medical history text
//
//    private Audit audit;
//
//    @Data
//    public static class Audit {
//        // Use yyyy-MM-dd (string) in DTO to align with your preference
//        private String createdDate;
//        private String lastModifiedDate;
//    }
//}



package com.qiaben.ciyex.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
public class PatientMedicalHistoryDto {
    private Long id;
    private Long patientId;
    private Long encounterId;
    private Long orgId;
    private String externalId;

    private String medicalCondition;
    private String conditionName;
    private String status;
    private Boolean isChronic;

    private LocalDateTime diagnosisDate;
    private LocalDate onsetDate;
    private LocalDate resolvedDate;

    private LocalDate createdDate;
    private LocalDate lastModifiedDate;

    private String treatmentDetails;
    private String diagnosisDetails;
    private String notes;
    private String description;

    // server-managed eSign/print
    private Boolean eSigned;
    private OffsetDateTime signedAt;
    private String signedBy;
    private OffsetDateTime printedAt;

    private Audit audit;
    @Data
    public static class Audit {
        private String createdDate;      // yyyy-MM-dd (from created_at)
        private String lastModifiedDate; // yyyy-MM-dd (from updated_at)
    }
}
