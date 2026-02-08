package org.ciyex.ehr.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.MedicalProblemDto;
import org.ciyex.ehr.fhir.FhirClientService;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MedicalProblem Service - FHIR Only.
 * All medical problem data is stored in HAPI FHIR server as Condition resources.
 */
@Service
@Slf4j
public class MedicalProblemService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final String CONDITION_CATEGORY_PROBLEM = "problem-list-item";

    @Autowired
    public MedicalProblemService(FhirClientService fhirClientService, PracticeContextService practiceContextService) {
        this.fhirClientService = fhirClientService;
        this.practiceContextService = practiceContextService;
    }

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // ✅ Create medical problems for a patient
    public MedicalProblemDto create(MedicalProblemDto dto) {
        if (dto.getPatientId() == null)
            throw new IllegalArgumentException("patientId is required");

        log.info("Creating medical problems in FHIR for patient: {}", dto.getPatientId());

        List<MedicalProblemDto.MedicalProblemItem> createdItems = new ArrayList<>();

        if (dto.getProblemsList() != null) {
            for (var item : dto.getProblemsList()) {
                Condition fhirCondition = toFhirCondition(item, dto.getPatientId());
                MethodOutcome outcome = fhirClientService.create(fhirCondition, getPracticeId());
                String fhirId = outcome.getId().getIdPart();

                item.setFhirId(fhirId);
                item.setExternalId(fhirId);
                item.setPatientId(dto.getPatientId());
                createdItems.add(item);

                log.info("Created FHIR Condition (problem) with ID: {}", fhirId);
            }
        }

        MedicalProblemDto result = new MedicalProblemDto();
        result.setPatientId(dto.getPatientId());
        result.setProblemsList(createdItems);
        if (!createdItems.isEmpty()) {
            result.setFhirId(createdItems.get(0).getFhirId());
            result.setExternalId(createdItems.get(0).getFhirId());
        }
        return result;
    }

    // ✅ Get all medical problems for a patient
    public MedicalProblemDto getByPatientId(Long patientId) {
        log.debug("Getting FHIR Conditions (problems) for patient: {}", patientId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Condition.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .where(new TokenClientParam("category").exactly()
                        .systemAndCode("http://terminology.hl7.org/CodeSystem/condition-category", CONDITION_CATEGORY_PROBLEM))
                
                .returnBundle(Bundle.class)
                .execute();

        List<MedicalProblemDto.MedicalProblemItem> items = extractProblemItems(bundle);

        MedicalProblemDto dto = new MedicalProblemDto();
        dto.setPatientId(patientId);
        dto.setProblemsList(items);
        if (!items.isEmpty()) {
            dto.setFhirId(items.get(0).getFhirId());
            dto.setExternalId(items.get(0).getFhirId());
        }
        return dto;
    }

    // ✅ Update medical problems for a patient (replace all)
    public MedicalProblemDto updateByPatientId(Long patientId, MedicalProblemDto dto) {
        log.info("Updating medical problems in FHIR for patient: {}", patientId);
        deleteByPatientId(patientId);
        dto.setPatientId(patientId);
        return create(dto);
    }

    // ✅ Delete all medical problems for a patient
    public void deleteByPatientId(Long patientId) {
        log.info("Deleting all FHIR Conditions (problems) for patient: {}", patientId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Condition.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .where(new TokenClientParam("category").exactly()
                        .systemAndCode("http://terminology.hl7.org/CodeSystem/condition-category", CONDITION_CATEGORY_PROBLEM))
                
                .returnBundle(Bundle.class)
                .execute();

        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Condition) {
                    String fhirId = entry.getResource().getIdElement().getIdPart();
                    fhirClientService.delete(Condition.class, fhirId, getPracticeId());
                    log.debug("Deleted FHIR Condition: {}", fhirId);
                }
            }
        }
    }

    // ✅ Get single problem item
    public MedicalProblemDto.MedicalProblemItem getItem(Long patientId, Long problemId) {
        String fhirId = String.valueOf(problemId);
        try {
            Condition fhirCondition = fhirClientService.read(Condition.class, fhirId, getPracticeId());
            return toProblemItem(fhirCondition);
        } catch (ResourceNotFoundException e) {
            throw new RuntimeException("Medical problem not found for patientId=" + patientId + " problemId=" + problemId);
        }
    }

    // ✅ Update single problem item
    public MedicalProblemDto.MedicalProblemItem updateItem(Long patientId, Long problemId,
                                                           MedicalProblemDto.MedicalProblemItem patch) {
        String fhirId = String.valueOf(problemId);
        log.info("Updating FHIR Condition (problem) with ID: {}", fhirId);

        Condition fhirCondition = toFhirCondition(patch, patientId);
        fhirCondition.setId(fhirId);

        fhirClientService.update(fhirCondition, getPracticeId());

        patch.setFhirId(fhirId);
        patch.setExternalId(fhirId);
        return patch;
    }

    // ✅ Delete single problem item
    public void deleteItem(Long patientId, Long problemId) {
        String fhirId = String.valueOf(problemId);
        log.info("Deleting FHIR Condition (problem) with ID: {}", fhirId);
        fhirClientService.delete(Condition.class, fhirId, getPracticeId());
    }

    // ✅ Search all medical problems
    public ApiResponse<List<MedicalProblemDto>> searchAll() {
        log.debug("Searching all FHIR Conditions (problems)");

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Condition.class)
                .where(new TokenClientParam("category").exactly()
                        .systemAndCode("http://terminology.hl7.org/CodeSystem/condition-category", CONDITION_CATEGORY_PROBLEM))
                
                .returnBundle(Bundle.class)
                .execute();

        List<MedicalProblemDto.MedicalProblemItem> allItems = extractProblemItems(bundle);

        // Group by patient
        Map<Long, List<MedicalProblemDto.MedicalProblemItem>> byPatient = allItems.stream()
                .filter(item -> item.getPatientId() != null)
                .collect(Collectors.groupingBy(MedicalProblemDto.MedicalProblemItem::getPatientId));

        List<MedicalProblemDto> dtos = new ArrayList<>();
        for (var entry : byPatient.entrySet()) {
            MedicalProblemDto dto = new MedicalProblemDto();
            dto.setPatientId(entry.getKey());
            dto.setProblemsList(entry.getValue());
            if (!entry.getValue().isEmpty()) {
                dto.setFhirId(entry.getValue().get(0).getFhirId());
                dto.setExternalId(entry.getValue().get(0).getFhirId());
            }
            dtos.add(dto);
        }

        return ApiResponse.<List<MedicalProblemDto>>builder()
                .success(true)
                .message("Medical Problems retrieved successfully")
                .data(dtos)
                .build();
    }

    // ========== FHIR Mapping Methods ==========

    private Condition toFhirCondition(MedicalProblemDto.MedicalProblemItem item, Long patientId) {
        Condition condition = new Condition();

        // Category: problem-list-item
        condition.addCategory()
                .addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-category")
                .setCode(CONDITION_CATEGORY_PROBLEM)
                .setDisplay("Problem List Item");

        // Subject (Patient)
        condition.setSubject(new Reference("Patient/" + patientId));

        // Code (title/problem name)
        if (item.getTitle() != null) {
            condition.setCode(new CodeableConcept().setText(item.getTitle()));
        }

        // Clinical status (outcome)
        if (item.getOutcome() != null) {
            condition.setClinicalStatus(new CodeableConcept()
                    .addCoding(new Coding()
                            .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                            .setCode(mapToFhirClinicalStatus(item.getOutcome()))
                            .setDisplay(item.getOutcome())));
        }

        // Verification status
        if (item.getVerificationStatus() != null) {
            condition.setVerificationStatus(new CodeableConcept()
                    .addCoding(new Coding()
                            .setSystem("http://terminology.hl7.org/CodeSystem/condition-ver-status")
                            .setCode(mapToFhirVerificationStatus(item.getVerificationStatus()))
                            .setDisplay(item.getVerificationStatus())));
        }

        // Onset (occurrence)
        if (item.getOccurrence() != null) {
            condition.setOnset(new StringType(item.getOccurrence()));
        }

        // Note
        if (item.getNote() != null) {
            condition.addNote().setText(item.getNote());
        }

        return condition;
    }

    private MedicalProblemDto.MedicalProblemItem toProblemItem(Condition condition) {
        MedicalProblemDto.MedicalProblemItem item = new MedicalProblemDto.MedicalProblemItem();

        // FHIR ID
        if (condition.hasId()) {
            item.setFhirId(condition.getIdElement().getIdPart());
            item.setExternalId(condition.getIdElement().getIdPart());
        }

        // Patient ID
        if (condition.hasSubject() && condition.getSubject().hasReference()) {
            String ref = condition.getSubject().getReference();
            if (ref.startsWith("Patient/")) {
                try {
                    item.setPatientId(Long.parseLong(ref.substring(8)));
                } catch (NumberFormatException e) {
                    // Non-numeric FHIR ID
                }
            }
        }

        // Title (code)
        if (condition.hasCode()) {
            item.setTitle(condition.getCode().getText());
        }

        // Outcome (clinical status)
        if (condition.hasClinicalStatus() && condition.getClinicalStatus().hasCoding()) {
            item.setOutcome(mapFromFhirClinicalStatus(condition.getClinicalStatus().getCodingFirstRep().getCode()));
        }

        // Verification status
        if (condition.hasVerificationStatus() && condition.getVerificationStatus().hasCoding()) {
            item.setVerificationStatus(mapFromFhirVerificationStatus(
                    condition.getVerificationStatus().getCodingFirstRep().getCode()));
        }

        // Occurrence (onset)
        if (condition.hasOnsetStringType()) {
            item.setOccurrence(condition.getOnsetStringType().getValue());
        }

        // Note
        if (condition.hasNote()) {
            item.setNote(condition.getNoteFirstRep().getText());
        }

        return item;
    }

    private List<MedicalProblemDto.MedicalProblemItem> extractProblemItems(Bundle bundle) {
        List<MedicalProblemDto.MedicalProblemItem> items = new ArrayList<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Condition) {
                    items.add(toProblemItem((Condition) entry.getResource()));
                }
            }
        }
        return items;
    }

    private String mapToFhirClinicalStatus(String outcome) {
        if (outcome == null) return "active";
        return switch (outcome.toLowerCase()) {
            case "active", "ongoing" -> "active";
            case "recurrence" -> "recurrence";
            case "relapse" -> "relapse";
            case "inactive" -> "inactive";
            case "remission" -> "remission";
            case "resolved" -> "resolved";
            default -> "active";
        };
    }

    private String mapFromFhirClinicalStatus(String code) {
        if (code == null) return "active";
        return code; // FHIR codes are already readable
    }

    private String mapToFhirVerificationStatus(String status) {
        if (status == null) return "confirmed";
        return switch (status.toLowerCase()) {
            case "unconfirmed", "provisional" -> "unconfirmed";
            case "confirmed" -> "confirmed";
            case "refuted" -> "refuted";
            case "entered-in-error" -> "entered-in-error";
            default -> "confirmed";
        };
    }

    private String mapFromFhirVerificationStatus(String code) {
        if (code == null) return "confirmed";
        return code; // FHIR codes are already readable
    }
}
