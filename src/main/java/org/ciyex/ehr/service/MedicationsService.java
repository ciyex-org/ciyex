package org.ciyex.ehr.service;

import org.ciyex.ehr.dto.MedicationRequestDto;
import org.ciyex.ehr.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-only Medications Service for portal users.
 * Uses FHIR MedicationRequest resource directly via FhirClientService.
 * No local database storage - all data stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MedicationsService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final String EXT_PATIENT = "http://ciyex.com/fhir/StructureDefinition/patient-id";
    private static final String EXT_ENCOUNTER = "http://ciyex.com/fhir/StructureDefinition/encounter-id";
    private static final String EXT_DOSAGE = "http://ciyex.com/fhir/StructureDefinition/dosage-text";
    private static final String EXT_INSTRUCTIONS = "http://ciyex.com/fhir/StructureDefinition/instructions";
    private static final String EXT_PRESCRIBER = "http://ciyex.com/fhir/StructureDefinition/prescribing-doctor";

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    /**
     * Get medications for the currently authenticated portal user.
     * Extracts patient ID from JWT claims and fetches from FHIR server.
     */
    public List<MedicationRequestDto> getMedicationsForPortalUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("No authenticated user found in security context");
            throw new RuntimeException("User not authenticated");
        }

        // Extract patient ID from JWT
        Long patientId = null;
        Object principal = authentication.getPrincipal();

        if (principal instanceof Jwt jwt) {
            // Try to get patient ID from JWT claims
            Object patientIdClaim = jwt.getClaim("patient_id");
            if (patientIdClaim != null) {
                patientId = Long.parseLong(patientIdClaim.toString());
            }
            String email = jwt.getClaim("email");
            log.info("Fetching medications for portal user email: {}, patientId: {}", email, patientId);
        }

        if (patientId == null) {
            log.error("Patient ID not found in JWT claims");
            throw new RuntimeException("Patient ID not found in token");
        }

        return getMedicationsByPatientId(patientId);
    }

    /**
     * Get all medications for a specific patient from FHIR server.
     */
    public List<MedicationRequestDto> getMedicationsByPatientId(Long patientId) {
        log.debug("Getting medications for patient {}", patientId);

        Bundle bundle = fhirClientService.search(
                org.hl7.fhir.r4.model.MedicationRequest.class,
                getPracticeId()
        );

        return fhirClientService.extractResources(bundle, org.hl7.fhir.r4.model.MedicationRequest.class).stream()
                .filter(mr -> patientId.equals(getPatientId(mr)))
                .map(this::fromFhirMedicationRequest)
                .collect(Collectors.toList());
    }

    /**
     * Get a single medication by FHIR ID.
     */
    public MedicationRequestDto getOne(String fhirId) {
        log.debug("Getting medication: {}", fhirId);
        org.hl7.fhir.r4.model.MedicationRequest mr = fhirClientService.read(
                org.hl7.fhir.r4.model.MedicationRequest.class, fhirId, getPracticeId());
        return fromFhirMedicationRequest(mr);
    }

    /**
     * Create a new medication request in FHIR.
     */
    public MedicationRequestDto create(Long patientId, Long encounterId, MedicationRequestDto dto) {
        log.debug("Creating medication for patient {} encounter {}", patientId, encounterId);

        org.hl7.fhir.r4.model.MedicationRequest mr = toFhirMedicationRequest(dto, patientId, encounterId);
        var outcome = fhirClientService.create(mr, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setId((long) Math.abs(fhirId.hashCode()));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);

        log.info("Created FHIR MedicationRequest with id: {}", fhirId);
        return dto;
    }

    /**
     * Update an existing medication request in FHIR.
     */
    public MedicationRequestDto update(String fhirId, MedicationRequestDto dto) {
        log.debug("Updating medication: {}", fhirId);

        org.hl7.fhir.r4.model.MedicationRequest mr = toFhirMedicationRequest(dto, dto.getPatientId(), dto.getEncounterId());
        mr.setId(fhirId);
        fhirClientService.update(mr, getPracticeId());

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        return dto;
    }

    /**
     * Delete a medication request from FHIR.
     */
    public void delete(String fhirId) {
        log.debug("Deleting medication: {}", fhirId);
        fhirClientService.delete(org.hl7.fhir.r4.model.MedicationRequest.class, fhirId, getPracticeId());
    }

    // -------- FHIR Mapping --------

    private org.hl7.fhir.r4.model.MedicationRequest toFhirMedicationRequest(MedicationRequestDto dto, Long patientId, Long encounterId) {
        org.hl7.fhir.r4.model.MedicationRequest mr = new org.hl7.fhir.r4.model.MedicationRequest();

        // Status
        if (dto.getStatus() != null) {
            mr.setStatus(org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestStatus.fromCode(dto.getStatus().toLowerCase()));
        } else {
            mr.setStatus(org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestStatus.ACTIVE);
        }

        mr.setIntent(org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestIntent.ORDER);

        // Medication name
        if (dto.getMedicationName() != null) {
            CodeableConcept med = new CodeableConcept();
            med.setText(dto.getMedicationName());
            mr.setMedication(med);
        }

        // Patient reference
        mr.setSubject(new Reference("Patient/" + patientId));
        mr.addExtension(new Extension(EXT_PATIENT, new StringType(patientId.toString())));

        // Encounter reference
        if (encounterId != null) {
            mr.setEncounter(new Reference("Encounter/" + encounterId));
            mr.addExtension(new Extension(EXT_ENCOUNTER, new StringType(encounterId.toString())));
        }

        // Dosage
        if (dto.getDosage() != null) {
            Dosage dosage = mr.addDosageInstruction();
            dosage.setText(dto.getDosage());
            mr.addExtension(new Extension(EXT_DOSAGE, new StringType(dto.getDosage())));
        }

        // Instructions
        if (dto.getInstructions() != null) {
            mr.addExtension(new Extension(EXT_INSTRUCTIONS, new StringType(dto.getInstructions())));
        }

        // Prescribing doctor
        if (dto.getPrescribingDoctor() != null) {
            mr.addExtension(new Extension(EXT_PRESCRIBER, new StringType(dto.getPrescribingDoctor())));
        }

        // Date issued
        if (dto.getDateIssued() != null) {
            try {
                mr.setAuthoredOn(java.sql.Date.valueOf(LocalDate.parse(dto.getDateIssued())));
            } catch (Exception e) {
                log.warn("Could not parse date issued: {}", dto.getDateIssued());
            }
        }

        return mr;
    }

    private MedicationRequestDto fromFhirMedicationRequest(org.hl7.fhir.r4.model.MedicationRequest mr) {
        MedicationRequestDto dto = new MedicationRequestDto();

        String fhirId = mr.getIdElement().getIdPart();
        dto.setId((long) Math.abs(fhirId.hashCode()));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);

        // Patient ID
        dto.setPatientId(getPatientId(mr));

        // Encounter ID
        dto.setEncounterId(getEncounterId(mr));

        // Medication name
        if (mr.hasMedicationCodeableConcept()) {
            dto.setMedicationName(mr.getMedicationCodeableConcept().getText());
        }

        // Status
        if (mr.hasStatus()) {
            dto.setStatus(mr.getStatus().toCode());
        }

        // Dosage
        Extension dosageExt = mr.getExtensionByUrl(EXT_DOSAGE);
        if (dosageExt != null && dosageExt.getValue() instanceof StringType) {
            dto.setDosage(((StringType) dosageExt.getValue()).getValue());
        } else if (mr.hasDosageInstruction() && !mr.getDosageInstruction().isEmpty()) {
            dto.setDosage(mr.getDosageInstruction().get(0).getText());
        }

        // Instructions
        Extension instrExt = mr.getExtensionByUrl(EXT_INSTRUCTIONS);
        if (instrExt != null && instrExt.getValue() instanceof StringType) {
            dto.setInstructions(((StringType) instrExt.getValue()).getValue());
        }

        // Prescribing doctor
        Extension prescriberExt = mr.getExtensionByUrl(EXT_PRESCRIBER);
        if (prescriberExt != null && prescriberExt.getValue() instanceof StringType) {
            dto.setPrescribingDoctor(((StringType) prescriberExt.getValue()).getValue());
        }

        // Date issued
        if (mr.hasAuthoredOn()) {
            dto.setDateIssued(mr.getAuthoredOn().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(DAY));
        }

        // Audit
        MedicationRequestDto.Audit audit = new MedicationRequestDto.Audit();
        audit.setCreatedDate(LocalDate.now().format(DAY));
        audit.setLastModifiedDate(LocalDate.now().format(DAY));
        dto.setAudit(audit);

        return dto;
    }

    private Long getPatientId(org.hl7.fhir.r4.model.MedicationRequest mr) {
        Extension ext = mr.getExtensionByUrl(EXT_PATIENT);
        if (ext != null && ext.getValue() instanceof StringType) {
            try {
                return Long.parseLong(((StringType) ext.getValue()).getValue());
            } catch (NumberFormatException ignored) {}
        }
        // Try from subject reference
        if (mr.hasSubject() && mr.getSubject().hasReference()) {
            String ref = mr.getSubject().getReference();
            if (ref.startsWith("Patient/")) {
                try {
                    return Long.parseLong(ref.substring(8));
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    private Long getEncounterId(org.hl7.fhir.r4.model.MedicationRequest mr) {
        Extension ext = mr.getExtensionByUrl(EXT_ENCOUNTER);
        if (ext != null && ext.getValue() instanceof StringType) {
            try {
                return Long.parseLong(((StringType) ext.getValue()).getValue());
            } catch (NumberFormatException ignored) {}
        }
        // Try from encounter reference
        if (mr.hasEncounter() && mr.getEncounter().hasReference()) {
            String ref = mr.getEncounter().getReference();
            if (ref.startsWith("Encounter/")) {
                try {
                    return Long.parseLong(ref.substring(10));
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }
}
