package com.qiaben.ciyex.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Unified DTO for saving multiple encounter-related fields in one request.
 * Only fields that are populated/modified are saved.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GlobalEncounterSaveDto {

    private Long encounterId;

    // Chief Complaint Section - uses actual ChiefComplaintDto
    private ChiefComplaintDto chiefComplaint;

    // Assigned Provider Section - uses actual AssignedProviderDto
    private AssignedProviderDto assignedProvider;

    // HPI Section - uses actual HistoryOfPresentIllnessDto
    private HistoryOfPresentIllnessDto hpi;

    // Review of Systems Section - uses actual ReviewOfSystemDto
    private ReviewOfSystemDto reviewOfSystems;

    // Patient Medical History Section - uses actual PatientMedicalHistoryDto
    private PatientMedicalHistoryDto patientMedicalHistory;

    // Past Medical History Section - uses actual PastMedicalHistoryDto
    private PastMedicalHistoryDto pastMedicalHistory;

    // Family History Section - uses actual FamilyHistoryDto
    private FamilyHistoryDto familyHistory;

    // Social History Section - uses actual SocialHistoryDto
    private SocialHistoryDto socialHistory;

    // Physical Exam Section - uses actual PhysicalExamDto
    private PhysicalExamDto physicalExam;

    // Vitals Section - uses actual VitalsDto
    private VitalsDto vitals;

    // Procedures Section - uses actual ProcedureDto
    private ProcedureDto procedure;

    // Codes Section - uses actual CodeDto
    private CodeDto code;

    // Assessment Section - uses actual AssessmentDto
    private AssessmentDto assessment;

    // Plan Section - uses actual PlanDto
    private PlanDto plan;

    // Provider Notes Section - uses actual ProviderNoteDto
    private ProviderNoteDto providerNotes;

    // Provider Signature Section - uses actual ProviderSignatureDto
    private ProviderSignatureDto providerSignature;

    // Date/Time Finalized Section - uses actual DateTimeFinalizedDto
    private DateTimeFinalizedDto dateTimeFinalized;

    /**
     * Helper method to check if any section has data
     */
    public boolean hasAnyData() {
        return chiefComplaint != null ||
                assignedProvider != null ||
                hpi != null ||
                reviewOfSystems != null ||
                patientMedicalHistory != null ||
                pastMedicalHistory != null ||
                familyHistory != null ||
                socialHistory != null ||
                physicalExam != null ||
                vitals != null ||
                procedure != null ||
                code != null ||
                assessment != null ||
                plan != null ||
                providerNotes != null ||
                providerSignature != null ||
                dateTimeFinalized != null;
    }

    /**
     * Get a map of non-null sections (for processing)
     */
    public Map<String, Object> getPopulatedSections() {
        Map<String, Object> sections = new HashMap<>();

        if (chiefComplaint != null) sections.put("chiefComplaint", chiefComplaint);
        if (assignedProvider != null) sections.put("assignedProvider", assignedProvider);
        if (hpi != null) sections.put("hpi", hpi);
        if (reviewOfSystems != null) sections.put("reviewOfSystems", reviewOfSystems);
        if (patientMedicalHistory != null) sections.put("patientMedicalHistory", patientMedicalHistory);
        if (pastMedicalHistory != null) sections.put("pastMedicalHistory", pastMedicalHistory);
        if (familyHistory != null) sections.put("familyHistory", familyHistory);
        if (socialHistory != null) sections.put("socialHistory", socialHistory);
        if (physicalExam != null) sections.put("physicalExam", physicalExam);
        if (vitals != null) sections.put("vitals", vitals);
        if (procedure != null) sections.put("procedure", procedure);
        if (code != null) sections.put("code", code);
        if (assessment != null) sections.put("assessment", assessment);
        if (plan != null) sections.put("plan", plan);
        if (providerNotes != null) sections.put("providerNotes", providerNotes);
        if (providerSignature != null) sections.put("providerSignature", providerSignature);
        if (dateTimeFinalized != null) sections.put("dateTimeFinalized", dateTimeFinalized);

        return sections;
    }
}
