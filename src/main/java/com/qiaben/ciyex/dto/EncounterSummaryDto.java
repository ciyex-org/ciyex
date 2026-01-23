// src/main/java/com/qiaben/ciyex/dto/encounter/EncounterSummaryDto.java

package com.qiaben.ciyex.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EncounterSummaryDto {
    private EncounterMeta meta;

    private List<AssignedProvider> assignedProviders;
    private List<ChiefComplaint> chiefComplaints;
    private List<Vitals> vitals;
    private List<HPIEntry> hpi;
    private List<PMHEntry> pmh;
    private List<PatientMHEntry> patientMH;
    private List<FamilyHistory> familyHistory;
    private SocialHistory socialHistory;
    private List<ROSEntry> ros;
    private List<PhysicalExam> physicalExam;
    private List<Procedure> procedures;
//    private List<Code> codes;
    private List<Assessment> assessment;
    private List<Plan> plan;
    private List<ProviderNote> providerNotes;
    private ProviderSignature providerSignature;
//    private Signoff signoff;
    private DateTimeFinalized dateTimeFinalized;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class EncounterMeta {
        private String visitCategory;
        private String type;
        private String facility;
        private String dateOfService;
        private String reasonForVisit;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AssignedProvider {
        private Long id;
        private String fhirId;
        private String providerName; // or name
        private String name;
        private String role;
        private String start;
        private String end;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Vitals {
        private Long id;
        private String fhirId;
        private Double weightKg;
        private Double weightLbs;
        private Double heightCm;
        private Double heightIn;
        private Integer bpSystolic;
        private Integer bpDiastolic;
        private Integer pulse;
        private Integer respiration;
        private Double temperatureC;
        private Double temperatureF;
        private Double oxygenSaturation;
        private Double bmi;
        private String notes;
        private String recordedAt;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ChiefComplaint {
        private Long id;
        private String fhirId;
        private String title;
        private String complaint;
        private String notes;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class HPIEntry {
        private Long id;
        private String fhirId;
        private String description;
        private String text;
        private String notes;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PMHEntry {
        private Long id;
        private String fhirId;
        private String description;
        private String text;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PatientMHEntry {
        private Long id;
        private String fhirId;
        private String description;
        private String text;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class FamilyHistoryEntry {
        private String relation;
        private String diagnosisText;
        private String condition;
        private String details;
        private String diagnosisCode;
        private String notes;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class FamilyHistory {
        private Long id;
        private String fhirId;
        private List<FamilyHistoryEntry> entries;
        private String relation;
        private String condition;
        private String details;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SocialHistoryEntry {
        private Long id;
        private String category;
        private String value;
        private String details;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SocialHistory {
        private List<SocialHistoryEntry> entries;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ROSEntry {
        private Long id;
        private String fhirId;
        private String system;
        private String systemName;
        private String status;
        private Boolean isNegative;
        private String finding;
        private List<String> findings;
        private String notes;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PhysicalExamSection {
        private String sectionKey;
        private Boolean allNormal;
        private String normalText;
        private String findings;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PhysicalExam {
        private Long id;
        private String fhirId;
        private String summary;
        private List<PhysicalExamSection> sections;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Procedure {
        private Long id;
        private String fhirId;
        private String cpt4;
        private String description;
        private String procedureName;
        private Integer units;
        private Double rate;
        private String relatedIcds;
        private List<CodeItem> codeItems;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CodeItem {
        private String cpt4;
        private String description;
        private Integer units;
        private Double rate;
        private String relatedIcds;
        private String modifier1;
        private String note;
    }

//    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
//    public static class Code {
//        private Long id;
//        private String code;
//        private String description;
//    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Assessment {
        private Long id;
        private String fhirId;
        private String text;       // or "assessment"
        private String assessment;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Plan {
        private Long id;
        private String fhirId;
        private String diagnosticPlan;
        private String plan;
        private String notes;
        private String section1;
        private String section2;
        private Object sectionsJson;
        private Object followUpVisit;
        private Object returnWorkSchool;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ProviderNote {
        private Long id;
        private String fhirId;
        private String subjective;
        private String objective;
        private String assessment;
        private String plan;
        private String narrative;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ProviderSignature {
        private String signedBy;
        private String signedAt;
        private String status;
        private String signatureData;
        private String signatureFormat; // "image/png" or similar
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DateTimeFinalized {
        private String finalizedAt;
        private String lockedAt;
    }

//    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
//    public static class Signoff {
//        private String status;
//        private String signedBy;
//        private String signedAt;
//        private List<String> cosigners;
//        private String cosignedAt;
//        private String finalizedAt;
//        private String lockedAt;
//    }
}
