package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import com.qiaben.ciyex.dto.MedicationRequestDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * MedicationRequest Service - FHIR Only.
 * All medication request data is stored in HAPI FHIR server as MedicationRequest resources.
 */
@Service
@Slf4j
public class MedicationRequestService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    @Autowired
    public MedicationRequestService(FhirClientService fhirClientService, PracticeContextService practiceContextService) {
        this.fhirClientService = fhirClientService;
        this.practiceContextService = practiceContextService;
    }

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // ✅ Create medication request
    public MedicationRequestDto create(MedicationRequestDto dto) {
        validateMandatoryFields(dto);
        log.info("Creating medication request in FHIR for patient: {}, encounter: {}", dto.getPatientId(), dto.getEncounterId());

        org.hl7.fhir.r4.model.MedicationRequest medRequest = toFhirMedicationRequest(dto);
        MethodOutcome outcome = fhirClientService.create(medRequest, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);

        log.info("Created FHIR MedicationRequest with ID: {}", fhirId);
        return dto;
    }

    private void validateMandatoryFields(MedicationRequestDto dto) {
        StringBuilder errors = new StringBuilder();
        if (dto.getPatientId() == null) errors.append("patientId, ");
        if (dto.getEncounterId() == null) errors.append("encounterId, ");
        if (dto.getMedicationName() == null || dto.getMedicationName().trim().isEmpty()) errors.append("medicationName, ");

        if (errors.length() > 0) {
            throw new IllegalArgumentException("Missing mandatory fields: " + errors.substring(0, errors.length() - 2));
        }
    }

    // ✅ Get by ID
    public MedicationRequestDto getById(Long id) {
        String fhirId = String.valueOf(id);
        log.debug("Getting FHIR MedicationRequest with ID: {}", fhirId);

        try {
            org.hl7.fhir.r4.model.MedicationRequest medRequest = fhirClientService.read(
                    org.hl7.fhir.r4.model.MedicationRequest.class, fhirId, getPracticeId());
            return toMedicationRequestDto(medRequest);
        } catch (Exception e) {
            throw new RuntimeException("MedicationRequest not found with id: " + id);
        }
    }

    // ✅ Update medication request
    public MedicationRequestDto update(Long id, MedicationRequestDto dto) {
        String fhirId = String.valueOf(id);
        log.info("Updating FHIR MedicationRequest with ID: {}", fhirId);

        org.hl7.fhir.r4.model.MedicationRequest medRequest = toFhirMedicationRequest(dto);
        medRequest.setId(fhirId);
        fhirClientService.update(medRequest, getPracticeId());

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        return dto;
    }

    // ✅ Delete medication request
    public void delete(Long id) {
        String fhirId = String.valueOf(id);
        log.info("Deleting FHIR MedicationRequest with ID: {}", fhirId);
        fhirClientService.delete(org.hl7.fhir.r4.model.MedicationRequest.class, fhirId, getPracticeId());
    }

    // ✅ Get all by patient or encounter
    public List<MedicationRequestDto> getAllByPatientIdOrEncounterId(Long patientId, Long encounterId) {
        log.debug("Getting FHIR MedicationRequests for patient: {}, encounter: {}", patientId, encounterId);

        Bundle bundle;
        if (patientId != null) {
            bundle = fhirClientService.getClient().search()
                    .forResource(org.hl7.fhir.r4.model.MedicationRequest.class)
                    .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                    .withAdditionalHeader("X-Request-Tenant-Id", getPracticeId())
                    .returnBundle(Bundle.class)
                    .execute();
        } else if (encounterId != null) {
            bundle = fhirClientService.getClient().search()
                    .forResource(org.hl7.fhir.r4.model.MedicationRequest.class)
                    .where(new ReferenceClientParam("encounter").hasId("Encounter/" + encounterId))
                    .withAdditionalHeader("X-Request-Tenant-Id", getPracticeId())
                    .returnBundle(Bundle.class)
                    .execute();
        } else {
            bundle = fhirClientService.getClient().search()
                    .forResource(org.hl7.fhir.r4.model.MedicationRequest.class)
                    .withAdditionalHeader("X-Request-Tenant-Id", getPracticeId())
                    .returnBundle(Bundle.class)
                    .execute();
        }

        return extractMedicationRequestDtos(bundle);
    }

    // ========== FHIR Mapping Methods ==========

    private org.hl7.fhir.r4.model.MedicationRequest toFhirMedicationRequest(MedicationRequestDto dto) {
        org.hl7.fhir.r4.model.MedicationRequest medRequest = new org.hl7.fhir.r4.model.MedicationRequest();

        // Patient reference
        if (dto.getPatientId() != null) {
            medRequest.setSubject(new Reference("Patient/" + dto.getPatientId()));
        }

        // Encounter reference
        if (dto.getEncounterId() != null) {
            medRequest.setEncounter(new Reference("Encounter/" + dto.getEncounterId()));
        }

        // Status
        String status = dto.getStatus() != null ? dto.getStatus().toLowerCase() : "active";
        medRequest.setStatus(mapToMedicationRequestStatus(status));

        // Intent
        medRequest.setIntent(org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestIntent.ORDER);

        // Medication (as CodeableConcept)
        if (dto.getMedicationName() != null) {
            medRequest.setMedication(new CodeableConcept().setText(dto.getMedicationName()));
        }

        // Dosage instruction
        if (dto.getDosage() != null || dto.getInstructions() != null) {
            Dosage dosage = medRequest.addDosageInstruction();
            if (dto.getDosage() != null) {
                dosage.setText(dto.getDosage());
            }
            if (dto.getInstructions() != null) {
                dosage.setPatientInstruction(dto.getInstructions());
            }
        }

        // Requester (prescribing doctor)
        if (dto.getPrescribingDoctor() != null) {
            medRequest.setRequester(new Reference().setDisplay(dto.getPrescribingDoctor()));
        }

        // Authored on (date issued)
        if (dto.getDateIssued() != null) {
            medRequest.setAuthoredOnElement(new DateTimeType(dto.getDateIssued()));
        }

        return medRequest;
    }

    private MedicationRequestDto toMedicationRequestDto(org.hl7.fhir.r4.model.MedicationRequest medRequest) {
        MedicationRequestDto dto = new MedicationRequestDto();

        if (medRequest.hasId()) {
            dto.setFhirId(medRequest.getIdElement().getIdPart());
            dto.setExternalId(medRequest.getIdElement().getIdPart());
        }

        // Patient ID from subject reference
        if (medRequest.hasSubject() && medRequest.getSubject().hasReference()) {
            String ref = medRequest.getSubject().getReference();
            if (ref.startsWith("Patient/")) {
                try {
                    dto.setPatientId(Long.parseLong(ref.substring(8)));
                } catch (NumberFormatException ignored) {}
            }
        }

        // Encounter ID from encounter reference
        if (medRequest.hasEncounter() && medRequest.getEncounter().hasReference()) {
            String ref = medRequest.getEncounter().getReference();
            if (ref.startsWith("Encounter/")) {
                try {
                    dto.setEncounterId(Long.parseLong(ref.substring(10)));
                } catch (NumberFormatException ignored) {}
            }
        }

        // Medication name
        if (medRequest.hasMedicationCodeableConcept() && medRequest.getMedicationCodeableConcept().hasText()) {
            dto.setMedicationName(medRequest.getMedicationCodeableConcept().getText());
        }

        // Status
        if (medRequest.hasStatus()) {
            dto.setStatus(medRequest.getStatus().toCode());
        }

        // Dosage and instructions
        if (medRequest.hasDosageInstruction()) {
            Dosage dosage = medRequest.getDosageInstructionFirstRep();
            if (dosage.hasText()) {
                dto.setDosage(dosage.getText());
            }
            if (dosage.hasPatientInstruction()) {
                dto.setInstructions(dosage.getPatientInstruction());
            }
        }

        // Prescribing doctor
        if (medRequest.hasRequester() && medRequest.getRequester().hasDisplay()) {
            dto.setPrescribingDoctor(medRequest.getRequester().getDisplay());
        }

        // Date issued
        if (medRequest.hasAuthoredOn()) {
            dto.setDateIssued(medRequest.getAuthoredOn().toString());
        }

        // Audit
        dto.setAudit(new MedicationRequestDto.Audit());

        return dto;
    }

    private List<MedicationRequestDto> extractMedicationRequestDtos(Bundle bundle) {
        List<MedicationRequestDto> items = new ArrayList<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof org.hl7.fhir.r4.model.MedicationRequest) {
                    items.add(toMedicationRequestDto((org.hl7.fhir.r4.model.MedicationRequest) entry.getResource()));
                }
            }
        }
        return items;
    }

    private org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestStatus mapToMedicationRequestStatus(String status) {
        return switch (status) {
            case "active" -> org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestStatus.ACTIVE;
            case "on-hold" -> org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestStatus.ONHOLD;
            case "cancelled" -> org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestStatus.CANCELLED;
            case "completed" -> org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestStatus.COMPLETED;
            case "entered-in-error" -> org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestStatus.ENTEREDINERROR;
            case "stopped" -> org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestStatus.STOPPED;
            case "draft" -> org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestStatus.DRAFT;
            default -> org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestStatus.ACTIVE;
        };
    }
}