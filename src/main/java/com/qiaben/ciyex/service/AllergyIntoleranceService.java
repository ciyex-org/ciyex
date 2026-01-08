package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.AllergyIntoleranceDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AllergyIntolerance Service - FHIR Only.
 * All allergy data is stored in HAPI FHIR server as AllergyIntolerance resources.
 */
@Service
@Slf4j
public class AllergyIntoleranceService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    @Autowired
    public AllergyIntoleranceService(FhirClientService fhirClientService, PracticeContextService practiceContextService) {
        this.fhirClientService = fhirClientService;
        this.practiceContextService = practiceContextService;
    }

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // ✅ Create allergies for a patient
    public AllergyIntoleranceDto create(AllergyIntoleranceDto dto) {
        if (dto.getPatientId() == null) throw new IllegalArgumentException("patientId is required");
        log.info("Creating allergies in FHIR for patient: {}", dto.getPatientId());

        List<AllergyIntoleranceDto.AllergyItem> createdItems = new ArrayList<>();

        if (dto.getAllergiesList() != null) {
            for (var item : dto.getAllergiesList()) {
                validateMandatoryFields(item);
                validateDates(item.getStartDate(), item.getEndDate());

                AllergyIntolerance fhirAllergy = toFhirAllergyIntolerance(item, dto.getPatientId());
                MethodOutcome outcome = fhirClientService.create(fhirAllergy, getPracticeId());
                String fhirId = outcome.getId().getIdPart();

                item.setFhirId(fhirId);
                item.setExternalId(fhirId);
                item.setPatientId(dto.getPatientId());
                
                // Set ID from FHIR ID
                try {
                    item.setId(Long.valueOf(fhirId));
                } catch (NumberFormatException e) {
                    item.setId((long) fhirId.hashCode());
                }
                
                createdItems.add(item);

                log.info("Created FHIR AllergyIntolerance with ID: {}", fhirId);
            }
        }

        AllergyIntoleranceDto result = new AllergyIntoleranceDto();
        result.setAllergiesList(createdItems);
        
        // Add audit information
        AllergyIntoleranceDto.Audit audit = new AllergyIntoleranceDto.Audit();
        String currentTime = java.time.Instant.now().toString();
        audit.setCreatedDate(currentTime);
        audit.setLastModifiedDate(currentTime);
        result.setAudit(audit);
        
        return result;
    }

    // ✅ Get all allergies for a patient
    public AllergyIntoleranceDto getByPatientId(Long patientId) {
        log.debug("Getting FHIR AllergyIntolerances for patient: {}", patientId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(AllergyIntolerance.class)
                .where(new ReferenceClientParam("patient").hasId("Patient/" + patientId))
                
                .returnBundle(Bundle.class)
                .execute();

        List<AllergyIntoleranceDto.AllergyItem> items = extractAllergyItems(bundle);

        AllergyIntoleranceDto dto = new AllergyIntoleranceDto();
        dto.setAllergiesList(items);
        
        // Add audit information
        AllergyIntoleranceDto.Audit audit = new AllergyIntoleranceDto.Audit();
        String currentTime = java.time.Instant.now().toString();
        audit.setCreatedDate(currentTime);
        audit.setLastModifiedDate(currentTime);
        dto.setAudit(audit);
        
        return dto;
    }

    // ✅ Update allergies for a patient (replace all)
    public AllergyIntoleranceDto updateByPatientId(Long patientId, AllergyIntoleranceDto dto) {
        log.info("Updating allergies in FHIR for patient: {}", patientId);
        deleteByPatientId(patientId);
        dto.setPatientId(patientId);
        return create(dto);
    }

    // ✅ Delete all allergies for a patient
    public void deleteByPatientId(Long patientId) {
        log.info("Deleting all FHIR AllergyIntolerances for patient: {}", patientId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(AllergyIntolerance.class)
                .where(new ReferenceClientParam("patient").hasId("Patient/" + patientId))
                
                .returnBundle(Bundle.class)
                .execute();

        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof AllergyIntolerance) {
                    String fhirId = entry.getResource().getIdElement().getIdPart();
                    fhirClientService.delete(AllergyIntolerance.class, fhirId, getPracticeId());
                    log.debug("Deleted FHIR AllergyIntolerance: {}", fhirId);
                }
            }
        }
    }

    // ✅ Get single allergy item
    public AllergyIntoleranceDto.AllergyItem getItem(Long patientId, Long intoleranceId) {
        String fhirId = String.valueOf(intoleranceId);
        try {
            AllergyIntolerance fhirAllergy = fhirClientService.read(AllergyIntolerance.class, fhirId, getPracticeId());
            return toAllergyItem(fhirAllergy);
        } catch (ResourceNotFoundException e) {
            throw new RuntimeException("Allergy not found for patientId=" + patientId + " intoleranceId=" + intoleranceId);
        }
    }

    // ✅ Update single allergy item
    public AllergyIntoleranceDto.AllergyItem updateItem(Long patientId, Long intoleranceId,
                                                        AllergyIntoleranceDto.AllergyItem patch) {
        String fhirId = String.valueOf(intoleranceId);
        log.info("Updating FHIR AllergyIntolerance with ID: {}", fhirId);

        validateMandatoryFields(patch);
        validateDates(patch.getStartDate(), patch.getEndDate());

        AllergyIntolerance fhirAllergy = toFhirAllergyIntolerance(patch, patientId);
        patch.setPatientId(patientId);
        fhirAllergy.setId(fhirId);

        fhirClientService.update(fhirAllergy, getPracticeId());

        patch.setFhirId(fhirId);
        patch.setExternalId(fhirId);
        
        // Set ID from FHIR ID
        try {
            patch.setId(Long.valueOf(fhirId));
        } catch (NumberFormatException e) {
            patch.setId((long) fhirId.hashCode());
        }
        
        return patch;
    }

    // ✅ Delete single allergy item
    public void deleteItem(Long patientId, Long intoleranceId) {
        String fhirId = String.valueOf(intoleranceId);
        log.info("Deleting FHIR AllergyIntolerance with ID: {}", fhirId);
        fhirClientService.delete(AllergyIntolerance.class, fhirId, getPracticeId());
    }

    // ✅ Search all allergies
    public ApiResponse<List<AllergyIntoleranceDto>> searchAll() {
        log.debug("Searching all FHIR AllergyIntolerances");

        Bundle bundle = fhirClientService.search(AllergyIntolerance.class, getPracticeId());
        List<AllergyIntoleranceDto.AllergyItem> allItems = extractAllergyItems(bundle);

        // Group by patient
        Map<Long, List<AllergyIntoleranceDto.AllergyItem>> byPatient = allItems.stream()
                .filter(item -> item.getPatientId() != null)
                .collect(Collectors.groupingBy(AllergyIntoleranceDto.AllergyItem::getPatientId));

        List<AllergyIntoleranceDto> dtos = new ArrayList<>();
        for (var entry : byPatient.entrySet()) {
            AllergyIntoleranceDto dto = new AllergyIntoleranceDto();
            dto.setAllergiesList(entry.getValue());
            dtos.add(dto);
        }

        return ApiResponse.<List<AllergyIntoleranceDto>>builder()
                .success(true)
                .message("Allergy Intolerances retrieved successfully")
                .data(dtos)
                .build();
    }

    // ========== FHIR Mapping Methods ==========

    private AllergyIntolerance toFhirAllergyIntolerance(AllergyIntoleranceDto.AllergyItem item, Long patientId) {
        AllergyIntolerance allergy = new AllergyIntolerance();

        // Patient reference
        allergy.setPatient(new Reference("Patient/" + patientId));

        // Clinical status
        if (item.getStatus() != null) {
            allergy.setClinicalStatus(new CodeableConcept()
                    .addCoding(new Coding()
                            .setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical")
                            .setCode(mapToFhirClinicalStatus(item.getStatus()))
                            .setDisplay(item.getStatus())));
        }

        // Verification status
        allergy.setVerificationStatus(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-verification")
                        .setCode("confirmed")
                        .setDisplay("Confirmed")));

        // Allergy code (substance)
        if (item.getAllergyName() != null) {
            allergy.setCode(new CodeableConcept().setText(item.getAllergyName()));
        }

        // Criticality/Severity
        if (item.getSeverity() != null) {
            allergy.setCriticality(mapToFhirCriticality(item.getSeverity()));
        }

        // Reaction
        if (item.getReaction() != null) {
            AllergyIntolerance.AllergyIntoleranceReactionComponent reaction = allergy.addReaction();
            reaction.addManifestation(new CodeableConcept().setText(item.getReaction()));
            if (item.getSeverity() != null) {
                reaction.setSeverity(mapToFhirReactionSeverity(item.getSeverity()));
            }
        }

        // Onset (start date)
        if (item.getStartDate() != null) {
            try {
                allergy.setOnset(new DateTimeType(item.getStartDate()));
            } catch (Exception e) {
                // Ignore invalid date format
            }
        }

        // End date - store as extension since FHIR doesn't have native endDate
        if (item.getEndDate() != null) {
            try {
                Extension endDateExt = new Extension("http://ciyex.com/fhir/StructureDefinition/allergy-end-date");
                endDateExt.setValue(new DateType(item.getEndDate()));
                allergy.addExtension(endDateExt);
            } catch (Exception e) {
                // Ignore invalid date format
            }
        }

        // Notes/Comments
        if (item.getComments() != null) {
            allergy.addNote().setText(item.getComments());
        }

        return allergy;
    }

    private AllergyIntoleranceDto.AllergyItem toAllergyItem(AllergyIntolerance fhirAllergy) {
        AllergyIntoleranceDto.AllergyItem item = new AllergyIntoleranceDto.AllergyItem();

        // FHIR ID
        if (fhirAllergy.hasId()) {
            String fhirId = fhirAllergy.getIdElement().getIdPart();
            item.setFhirId(fhirId);
            item.setExternalId(fhirId);
            
            // Set ID from FHIR ID
            try {
                item.setId(Long.valueOf(fhirId));
            } catch (NumberFormatException e) {
                item.setId((long) fhirId.hashCode());
            }
        }

        // Patient ID
        if (fhirAllergy.hasPatient() && fhirAllergy.getPatient().hasReference()) {
            String ref = fhirAllergy.getPatient().getReference();
            if (ref.startsWith("Patient/")) {
                try {
                    item.setPatientId(Long.parseLong(ref.substring(8)));
                } catch (NumberFormatException e) {
                    // Non-numeric FHIR ID
                }
            }
        }

        // Allergy name
        if (fhirAllergy.hasCode()) {
            item.setAllergyName(fhirAllergy.getCode().getText());
        }

        // Clinical status
        if (fhirAllergy.hasClinicalStatus() && fhirAllergy.getClinicalStatus().hasCoding()) {
            item.setStatus(mapFromFhirClinicalStatus(fhirAllergy.getClinicalStatus().getCodingFirstRep().getCode()));
        }

        // Severity from criticality
        if (fhirAllergy.hasCriticality()) {
            item.setSeverity(mapFromFhirCriticality(fhirAllergy.getCriticality()));
        }

        // Reaction
        if (fhirAllergy.hasReaction()) {
            AllergyIntolerance.AllergyIntoleranceReactionComponent reaction = fhirAllergy.getReactionFirstRep();
            if (reaction.hasManifestation()) {
                item.setReaction(reaction.getManifestationFirstRep().getText());
            }
        }

        // Onset date
        if (fhirAllergy.hasOnsetDateTimeType()) {
            item.setStartDate(fhirAllergy.getOnsetDateTimeType().getValueAsString().substring(0, 10));
        }

        // End date - retrieve from extension
        Extension endDateExt = fhirAllergy.getExtensionByUrl("http://ciyex.com/fhir/StructureDefinition/allergy-end-date");
        if (endDateExt != null && endDateExt.getValue() instanceof DateType) {
            DateType endDate = (DateType) endDateExt.getValue();
            item.setEndDate(endDate.getValueAsString());
        }

        // Notes
        if (fhirAllergy.hasNote()) {
            item.setComments(fhirAllergy.getNoteFirstRep().getText());
        }

        return item;
    }

    private List<AllergyIntoleranceDto.AllergyItem> extractAllergyItems(Bundle bundle) {
        List<AllergyIntoleranceDto.AllergyItem> items = new ArrayList<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof AllergyIntolerance) {
                    items.add(toAllergyItem((AllergyIntolerance) entry.getResource()));
                }
            }
        }
        return items;
    }

    private String mapToFhirClinicalStatus(String status) {
        if (status == null) return "active";
        return switch (status.toLowerCase()) {
            case "active" -> "active";
            case "inactive" -> "inactive";
            case "resolved" -> "resolved";
            default -> "active";
        };
    }

    private String mapFromFhirClinicalStatus(String code) {
        if (code == null) return "active";
        return switch (code.toLowerCase()) {
            case "active" -> "active";
            case "inactive" -> "inactive";
            case "resolved" -> "resolved";
            default -> "active";
        };
    }

    private AllergyIntolerance.AllergyIntoleranceCriticality mapToFhirCriticality(String severity) {
        if (severity == null) return AllergyIntolerance.AllergyIntoleranceCriticality.UNABLETOASSESS;
        return switch (severity.toLowerCase()) {
            case "high", "severe" -> AllergyIntolerance.AllergyIntoleranceCriticality.HIGH;
            case "low", "mild" -> AllergyIntolerance.AllergyIntoleranceCriticality.LOW;
            default -> AllergyIntolerance.AllergyIntoleranceCriticality.UNABLETOASSESS;
        };
    }

    private String mapFromFhirCriticality(AllergyIntolerance.AllergyIntoleranceCriticality criticality) {
        if (criticality == null) return "moderate";
        return switch (criticality) {
            case HIGH -> "severe";
            case LOW -> "mild";
            default -> "moderate";
        };
    }

    private AllergyIntolerance.AllergyIntoleranceSeverity mapToFhirReactionSeverity(String severity) {
        if (severity == null) return AllergyIntolerance.AllergyIntoleranceSeverity.MODERATE;
        return switch (severity.toLowerCase()) {
            case "high", "severe" -> AllergyIntolerance.AllergyIntoleranceSeverity.SEVERE;
            case "low", "mild" -> AllergyIntolerance.AllergyIntoleranceSeverity.MILD;
            default -> AllergyIntolerance.AllergyIntoleranceSeverity.MODERATE;
        };
    }

    // ========== Validation Helpers ==========

    private void validateDates(String start, String end) {
        if (start == null || end == null) return;
        try {
            if (start.matches("\\d{4}-\\d{2}-\\d{2}") && end.matches("\\d{4}-\\d{2}-\\d{2}")) {
                if (LocalDate.parse(end).isBefore(LocalDate.parse(start))) {
                    throw new IllegalArgumentException("endDate cannot be before startDate");
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception ignore) {
            // tolerate non-ISO inputs
        }
    }

    private void validateMandatoryFields(AllergyIntoleranceDto.AllergyItem it) {
        if (it == null) throw new IllegalArgumentException("allergy item is required");
        if (isBlank(it.getAllergyName())) throw new IllegalArgumentException("allergyName is required");
        if (isBlank(it.getReaction())) throw new IllegalArgumentException("reaction is required");
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}

