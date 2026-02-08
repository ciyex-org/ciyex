package org.ciyex.ehr.service;

import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.GlobalEncounterSaveDto;
import org.ciyex.ehr.dto.ChiefComplaintDto;
import org.ciyex.ehr.dto.AssignedProviderDto;
import org.ciyex.ehr.dto.CodeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * FHIR-only Global Encounter Save Service.
 * Processes only the sections that have data and delegates to appropriate FHIR services.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalEncounterSaveService {

    private final EncounterService encounterService;
    private final PatientService patientService;
    private final ChiefComplaintService chiefComplaintService;
    private final AssignedProviderService assignedProviderService;
    private final HistoryOfPresentIllnessService historyOfPresentIllnessService;
    private final ReviewOfSystemService reviewOfSystemService;
    private final PatientMedicalHistoryService patientMedicalHistoryService;
    private final PastMedicalHistoryService pastMedicalHistoryService;
    private final FamilyHistoryService familyHistoryService;
    private final SocialHistoryService socialHistoryService;
    private final PhysicalExamService physicalExamService;
    private final VitalsService vitalsService;
    private final ProcedureService procedureService;
    private final CodeService codeService;
    private final AssessmentService assessmentService;
    private final PlanService planService;
    private final ProviderNoteService providerNoteService;
    private final ProviderSignatureService providerSignatureService;
    private final DateTimeFinalizedService dateTimeFinalizedService;

    /**
     * Save all populated sections independently
     * Only saves sections that have data (not null)
     * 
     * IMPORTANT: No method-level @Transactional to prevent entire transaction
     * from being marked as rollback-only on first error.
     * Each section call manages its own transaction independently.
     * This allows partial success and prevents "Transaction marked as rollback-only" errors.
     */
    public ApiResponse<Map<String, Object>> globalSave(Long patientId, Long encounterId, 
                                                        GlobalEncounterSaveDto globalSaveDto) {
        try {
            // Step 1: Validate Patient and Encounter exist
            validatePatientAndEncounter(patientId, encounterId);

            // Step 3: Check if any data is provided
            if (!globalSaveDto.hasAnyData()) {
                log.warn("Global save called with no data populated for patientId: {}, encounterId: {}", patientId, encounterId);
                return ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .message("No data to save. Please fill at least one section.")
                        .build();
            }

            Map<String, Object> savedSections = new java.util.HashMap<>();
            int sectionsProcessed = 0;

            // Step 4: Save each populated section
            log.info("Starting global save for patientId: {}, encounterId: {}", patientId, encounterId);

            if (globalSaveDto.getChiefComplaint() != null) {
                try {
                    chiefComplaintService.create(patientId, encounterId, globalSaveDto.getChiefComplaint());
                    savedSections.put("chiefComplaint", "Saved");
                    sectionsProcessed++;
                } catch (Exception e) {
                    log.error("Error saving Chief Complaint", e);
                    savedSections.put("chiefComplaint", "Error: " + e.getMessage());
                }
            }

            if (globalSaveDto.getAssignedProvider() != null) {
                try {
                    assignedProviderService.create(patientId, encounterId, globalSaveDto.getAssignedProvider());
                    savedSections.put("assignedProvider", "Saved");
                    sectionsProcessed++;
                } catch (Exception e) {
                    log.error("Error saving Assigned Provider", e);
                    savedSections.put("assignedProvider", "Error: " + e.getMessage());
                }
            }

            if (globalSaveDto.getHpi() != null) {
                try {
                    historyOfPresentIllnessService.create(patientId, encounterId, globalSaveDto.getHpi());
                    savedSections.put("hpi", "Saved");
                    sectionsProcessed++;
                } catch (Exception e) {
                    log.error("Error saving HPI", e);
                    savedSections.put("hpi", "Error: " + e.getMessage());
                }
            }

            if (globalSaveDto.getReviewOfSystems() != null) {
                try {
                    reviewOfSystemService.create(patientId, encounterId, globalSaveDto.getReviewOfSystems());
                    savedSections.put("reviewOfSystems", "Saved");
                    sectionsProcessed++;
                } catch (Exception e) {
                    log.error("Error saving Review of Systems", e);
                    savedSections.put("reviewOfSystems", "Error: " + e.getMessage());
                }
            }

            if (globalSaveDto.getPatientMedicalHistory() != null) {
                try {
                    patientMedicalHistoryService.create(patientId, encounterId, globalSaveDto.getPatientMedicalHistory());
                    savedSections.put("patientMedicalHistory", "Saved");
                    sectionsProcessed++;
                } catch (Exception e) {
                    log.error("Error saving Patient Medical History", e);
                    savedSections.put("patientMedicalHistory", "Error: " + e.getMessage());
                }
            }

            if (globalSaveDto.getPastMedicalHistory() != null) {
                try {
                    pastMedicalHistoryService.create(patientId, encounterId, globalSaveDto.getPastMedicalHistory());
                    savedSections.put("pastMedicalHistory", "Saved");
                    sectionsProcessed++;
                } catch (Exception e) {
                    log.error("Error saving Past Medical History", e);
                    savedSections.put("pastMedicalHistory", "Error: " + e.getMessage());
                }
            }

            if (globalSaveDto.getFamilyHistory() != null) {
                try {
                    familyHistoryService.create(patientId, encounterId, globalSaveDto.getFamilyHistory());
                    savedSections.put("familyHistory", "Saved");
                    sectionsProcessed++;
                } catch (Exception e) {
                    log.error("Error saving Family History", e);
                    savedSections.put("familyHistory", "Error: " + e.getMessage());
                }
            }

            if (globalSaveDto.getSocialHistory() != null) {
                try {
                    socialHistoryService.create(patientId, encounterId, globalSaveDto.getSocialHistory());
                    savedSections.put("socialHistory", "Saved");
                    sectionsProcessed++;
                } catch (Exception e) {
                    log.error("Error saving Social History", e);
                    savedSections.put("socialHistory", "Error: " + e.getMessage());
                }
            }

            if (globalSaveDto.getPhysicalExam() != null) {
                try {
                    physicalExamService.create(patientId, encounterId, globalSaveDto.getPhysicalExam());
                    savedSections.put("physicalExam", "Saved");
                    sectionsProcessed++;
                } catch (Exception e) {
                    log.error("Error saving Physical Exam", e);
                    savedSections.put("physicalExam", "Error: " + e.getMessage());
                }
            }

            if (globalSaveDto.getVitals() != null) {
                try {
                    vitalsService.create(patientId, encounterId, globalSaveDto.getVitals());
                    savedSections.put("vitals", "Saved");
                    sectionsProcessed++;
                } catch (Exception e) {
                    log.error("Error saving Vitals", e);
                    savedSections.put("vitals", "Error: " + e.getMessage());
                }
            }

            if (globalSaveDto.getProcedure() != null) {
                try {
                    procedureService.create(patientId, encounterId, globalSaveDto.getProcedure());
                    savedSections.put("procedure", "Saved");
                    sectionsProcessed++;
                } catch (Exception e) {
                    log.error("Error saving Procedure", e);
                    savedSections.put("procedure", "Error: " + e.getMessage());
                }
            }

            if (globalSaveDto.getCode() != null) {
                try {
                    // Transform CodeDto: map codeSystem field to codeType
                    CodeDto transformedCode = transformCodeDto(globalSaveDto.getCode());
                    codeService.create(patientId, encounterId, transformedCode);
                    savedSections.put("code", "Saved");
                    sectionsProcessed++;
                } catch (Exception e) {
                    log.error("Error saving Code", e);
                    savedSections.put("code", "Error: " + e.getMessage());
                }
            }

            if (globalSaveDto.getAssessment() != null) {
                try {
                    assessmentService.create(patientId, encounterId, globalSaveDto.getAssessment());
                    savedSections.put("assessment", "Saved");
                    sectionsProcessed++;
                } catch (Exception e) {
                    log.error("Error saving Assessment", e);
                    savedSections.put("assessment", "Error: " + e.getMessage());
                }
            }

            if (globalSaveDto.getPlan() != null) {
                try {
                    planService.create(patientId, encounterId, globalSaveDto.getPlan());
                    savedSections.put("plan", "Saved");
                    sectionsProcessed++;
                } catch (Exception e) {
                    log.error("Error saving Plan", e);
                    savedSections.put("plan", "Error: " + e.getMessage());
                }
            }

            if (globalSaveDto.getProviderNotes() != null) {
                try {
                    providerNoteService.create(patientId, encounterId, globalSaveDto.getProviderNotes());
                    savedSections.put("providerNotes", "Saved");
                    sectionsProcessed++;
                } catch (Exception e) {
                    log.error("Error saving Provider Notes", e);
                    savedSections.put("providerNotes", "Error: " + e.getMessage());
                }
            }

            if (globalSaveDto.getProviderSignature() != null) {
                try {
                    providerSignatureService.create(patientId, encounterId, globalSaveDto.getProviderSignature());
                    savedSections.put("providerSignature", "Saved");
                    sectionsProcessed++;
                } catch (Exception e) {
                    log.error("Error saving Provider Signature", e);
                    savedSections.put("providerSignature", "Error: " + e.getMessage());
                }
            }

            if (globalSaveDto.getDateTimeFinalized() != null) {
                try {
                    dateTimeFinalizedService.create(patientId, encounterId, globalSaveDto.getDateTimeFinalized());
                    savedSections.put("dateTimeFinalized", "Saved");
                    sectionsProcessed++;
                } catch (Exception e) {
                    log.error("Error saving Date/Time Finalized", e);
                    savedSections.put("dateTimeFinalized", "Error: " + e.getMessage());
                }
            }

            log.info("Global save completed: {} sections processed for patientId: {}, encounterId: {}", 
                    sectionsProcessed, patientId, encounterId);

            return ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message(String.format("Successfully saved %d section(s)", sectionsProcessed))
                    .data(savedSections)
                    .build();

        } catch (IllegalArgumentException ex) {
            log.error("Validation error in global save: {}", ex.getMessage());
            return ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message(ex.getMessage())
                    .build();
        } catch (Exception ex) {
            log.error("Unexpected error in global save", ex);
            return ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Error during global save: " + ex.getMessage())
                    .build();
        }
    }

    /**
     * Validate patient and encounter existence using FHIR services
     */
    protected void validatePatientAndEncounter(Long patientId, Long encounterId) {
        if (patientService.getById(patientId) == null) {
            throw new IllegalArgumentException(
                String.format("Patient not found with ID: %d. Please provide a valid Patient ID.", patientId)
            );
        }

        if (encounterService.getEncounter(patientId, encounterId) == null) {
            throw new IllegalArgumentException(
                String.format("Encounter not found with ID: %d for Patient ID: %d.", encounterId, patientId)
            );
        }
    }

    /**
     * Transform CodeDto to map API field "codeSystem" to expected field "codeType"
     * and normalize format (e.g., "ICD-10" -> "ICD10")
     */
    private CodeDto transformCodeDto(CodeDto codeDto) {
        if (codeDto == null) {
            return null;
        }
        
        // Normalize codeType: remove hyphens and uppercase
        // Handles: ICD-10 -> ICD10, ICD-9 -> ICD9, etc.
        if (codeDto.getCodeType() != null) {
            String normalized = codeDto.getCodeType().toUpperCase().replaceAll("-", "");
            codeDto.setCodeType(normalized);
        }
        
        return codeDto;
    }
}
