//package com.qiaben.ciyex.dto;
//
//import lombok.Data;
//
//@Data
//public class AssessmentDto {
//    private Long id;              // DB id
//    private String externalId;    // optional FHIR id
//
//    private Long patientId;
//    private Long encounterId;
//
//    // Free text fields commonly seen in the Assessment/ICD area
//    private String assessmentSummary;    // overall provider assessment
//    private String planSummary;          // plan / actions
//    private String notes;                // general notes
//
//
//    /**
//     * JSON string to keep the screen's checklists and sections in a single entity/table.
//     * Example JSON:
//     * {
//     *   "activeProblems": ["HTN", "DM2"],
//     *   "currentProblems": ["Cough"],
//     *   "functionalStatus": "Independent",
//     *   "cognitiveStatus": "Normal",
//     *   "labResult": "HbA1c 7.8",
//     *   "accidents": "None",
//     *   "checklist": {
//     *     "reviewedPFSH": true,
//     *     "obtainedOldRecords": true,
//     *     "summarizedOldRecords": true,
//     *     "interpretedTests": true,
//     *     "discussedReviewedTests": true,
//     *     "reviewedGrowthChart": false,
//     *     "reviewedAllergies": true,
//     *     "sharedEncounterNoteWithPatient": false,
//     *     "tobaccoCessationInterventionProvided": true,
//     *     "medicationsReconciled": true,
//     *     "reportDictated": false,
//     *     "sexuallyActive": true,
//     *     "sexuallyInactive": false
//     *   },
//     *   "icdCodes": ["I10","E11.9"]
//     * }
//     */
//    private String sectionsJson;
//
//    private Audit audit;
//
//    @Data
//    public static class Audit {
//        private String createdDate;       // yyyy-MM-dd
//        private String lastModifiedDate;  // yyyy-MM-dd
//    }
//}

package com.qiaben.ciyex.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class AssessmentDto {
    private Long id;
    private String externalId;
    private Long patientId;
    private Long encounterId;

    // Simple string fields
    private String diagnosisCode;
    private String diagnosisName;
    private String status;
    private String priority;
    private String assessmentText;
    private String notes;

    // eSign / Print (read-only from client)
    private Boolean eSigned;
    private OffsetDateTime signedAt;
    private String signedBy;
    private OffsetDateTime printedAt;

    private Audit audit;

    @Data
    public static class Audit {
        private String createdDate;       // yyyy-MM-dd
        private String lastModifiedDate;  // yyyy-MM-dd
    }
}
