package org.ciyex.ehr.service;

import org.ciyex.ehr.dto.CodeDto;
import org.ciyex.ehr.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-only Code Service (billing/diagnosis codes per encounter).
 * Uses FHIR Observation resource directly via FhirClientService.
 * No local database storage - all data stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CodeService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final String EXT_PATIENT = "http://ciyex.com/fhir/StructureDefinition/patient-reference";
    private static final String EXT_ENCOUNTER = "http://ciyex.com/fhir/StructureDefinition/encounter-reference";
    private static final String EXT_CODE_TYPE = "http://ciyex.com/fhir/StructureDefinition/code-type";
    private static final String EXT_MODIFIER = "http://ciyex.com/fhir/StructureDefinition/modifier";
    private static final String EXT_CATEGORY = "http://ciyex.com/fhir/StructureDefinition/category";
    private static final String EXT_SHORT_DESC = "http://ciyex.com/fhir/StructureDefinition/short-description";
    private static final String EXT_DIAGNOSIS_REPORTING = "http://ciyex.com/fhir/StructureDefinition/diagnosis-reporting";
    private static final String EXT_SERVICE_REPORTING = "http://ciyex.com/fhir/StructureDefinition/service-reporting";
    private static final String EXT_RELATE_TO = "http://ciyex.com/fhir/StructureDefinition/relate-to";
    private static final String EXT_FEE_STANDARD = "http://ciyex.com/fhir/StructureDefinition/fee-standard";
    private static final String EXT_ESIGNED = "http://ciyex.com/fhir/StructureDefinition/esigned";
    private static final String EXT_SIGNED_BY = "http://ciyex.com/fhir/StructureDefinition/signed-by";
    private static final String EXT_SIGNED_AT = "http://ciyex.com/fhir/StructureDefinition/signed-at";

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public CodeDto create(Long patientId, Long encounterId, CodeDto dto) {
        log.debug("Creating FHIR Observation for patient {} encounter {}", patientId, encounterId);

        Observation obs = toFhirObservation(dto, patientId, encounterId);
        var outcome = fhirClientService.create(obs, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        try {
            dto.setId(Long.parseLong(fhirId));
        } catch (NumberFormatException e) {
            // Fallback to hash if FHIR ID is not numeric
            dto.setId((long) Math.abs(fhirId.hashCode()));
        }
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);

        // Set audit information
        CodeDto.Audit audit = new CodeDto.Audit();
        audit.setCreatedDate(LocalDate.now().format(DAY));
        audit.setLastModifiedDate(LocalDate.now().format(DAY));
        dto.setAudit(audit);

        log.info("Created FHIR Observation with id: {}", fhirId);
        return dto;
    }

    // GET ALL by patient
    public List<CodeDto> getAllByPatient(Long patientId) {
        log.debug("Getting all codes for patient {}", patientId);
        Bundle bundle = fhirClientService.search(Observation.class, getPracticeId());
        return fhirClientService.extractResources(bundle, Observation.class).stream()
                .filter(obs -> patientId.equals(getPatientId(obs)))
                .map(this::fromFhirObservation)
                .collect(Collectors.toList());
    }

    // LIST by encounter
    public List<CodeDto> list(Long patientId, Long encounterId) {
        log.debug("Listing codes for patient {} encounter {}", patientId, encounterId);
        Bundle bundle = fhirClientService.search(Observation.class, getPracticeId());
        List<Observation> allObs = fhirClientService.extractResources(bundle, Observation.class);
        log.debug("Found {} total observations", allObs.size());
        
        return allObs.stream()
                .filter(obs -> {
                    Long obsPatientId = getPatientId(obs);
                    Long obsEncounterId = getEncounterId(obs);
                    log.debug("Observation: patientId={}, encounterId={}, target: patientId={}, encounterId={}", 
                             obsPatientId, obsEncounterId, patientId, encounterId);
                    return patientId.equals(obsPatientId) && encounterId.equals(obsEncounterId);
                })
                .map(this::fromFhirObservation)
                .collect(Collectors.toList());
    }

    // GET ONE
    public CodeDto getOne(Long patientId, Long encounterId, String fhirId) {
        log.debug("Getting code {} for patient {} encounter {}", fhirId, patientId, encounterId);
        Observation obs = fhirClientService.read(Observation.class, fhirId, getPracticeId());
        return fromFhirObservation(obs);
    }

    // UPDATE
    public CodeDto update(Long patientId, Long encounterId, String fhirId, CodeDto dto) {
        log.debug("Updating code {} for patient {} encounter {}", fhirId, patientId, encounterId);

        Observation obs = toFhirObservation(dto, patientId, encounterId);
        obs.setId(fhirId);
        fhirClientService.update(obs, getPracticeId());

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);
        return dto;
    }

    // DELETE
    public void delete(Long patientId, Long encounterId, String fhirId) {
        log.debug("Deleting code {} for patient {} encounter {}", fhirId, patientId, encounterId);
        fhirClientService.delete(Observation.class, fhirId, getPracticeId());
    }

    // ESIGN
    public CodeDto eSign(Long patientId, Long encounterId, String fhirId, String signedBy) {
        log.debug("E-signing code {} for patient {} encounter {}", fhirId, patientId, encounterId);
        Observation obs = fhirClientService.read(Observation.class, fhirId, getPracticeId());

        obs.getExtension().removeIf(e -> EXT_ESIGNED.equals(e.getUrl()));
        obs.addExtension(new Extension(EXT_ESIGNED, new BooleanType(true)));
        obs.getExtension().removeIf(e -> EXT_SIGNED_BY.equals(e.getUrl()));
        obs.addExtension(new Extension(EXT_SIGNED_BY, new StringType(signedBy != null ? signedBy : "system")));
        obs.getExtension().removeIf(e -> EXT_SIGNED_AT.equals(e.getUrl()));
        obs.addExtension(new Extension(EXT_SIGNED_AT, new StringType(LocalDate.now().format(DAY))));

        fhirClientService.update(obs, getPracticeId());
        return fromFhirObservation(obs);
    }

    // PRINT (PDF) - returns empty for now, can be implemented later
    public byte[] renderPdf(Long patientId, Long encounterId, String fhirId) {
        log.debug("Rendering PDF for code {} patient {} encounter {}", fhirId, patientId, encounterId);
        return new byte[0];
    }

    // -------- FHIR Mapping --------

    private Observation toFhirObservation(CodeDto dto, Long patientId, Long encounterId) {
        Observation obs = new Observation();
        obs.setStatus(Observation.ObservationStatus.FINAL);

        // Patient reference
        obs.setSubject(new Reference("Patient/" + patientId));

        // Encounter reference
        obs.setEncounter(new Reference("Encounter/" + encounterId));

        // Code and description as observation code
        if (dto.getCode() != null) {
            obs.setCode(new CodeableConcept().setText(dto.getCode()));
        }

        // Store all code data as extensions
        if (dto.getCodeType() != null) {
            obs.addExtension(new Extension(EXT_CODE_TYPE, new StringType(dto.getCodeType())));
        }
        if (dto.getModifier() != null) {
            obs.addExtension(new Extension(EXT_MODIFIER, new StringType(dto.getModifier())));
        }
        if (dto.getCategory() != null) {
            obs.addExtension(new Extension(EXT_CATEGORY, new StringType(dto.getCategory())));
        }
        if (dto.getShortDescription() != null) {
            obs.addExtension(new Extension(EXT_SHORT_DESC, new StringType(dto.getShortDescription())));
        }
        if (dto.getDiagnosisReporting() != null) {
            obs.addExtension(new Extension(EXT_DIAGNOSIS_REPORTING, new BooleanType(dto.getDiagnosisReporting())));
        }
        if (dto.getServiceReporting() != null) {
            obs.addExtension(new Extension(EXT_SERVICE_REPORTING, new BooleanType(dto.getServiceReporting())));
        }
        if (dto.getRelateTo() != null) {
            obs.addExtension(new Extension(EXT_RELATE_TO, new StringType(dto.getRelateTo())));
        }
        if (dto.getFeeStandard() != null) {
            obs.addExtension(new Extension(EXT_FEE_STANDARD, new DecimalType(BigDecimal.valueOf(dto.getFeeStandard()))));
        }
        if (dto.getDescription() != null) {
            obs.addNote().setText(dto.getDescription());
        }

        return obs;
    }

    private CodeDto fromFhirObservation(Observation obs) {
        CodeDto dto = new CodeDto();

        String fhirId = obs.getIdElement().getIdPart();
        try {
            dto.setId(Long.parseLong(fhirId));
        } catch (NumberFormatException e) {
            // Fallback to hash if FHIR ID is not numeric
            dto.setId((long) Math.abs(fhirId.hashCode()));
        }
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);

        // Code from observation code
        if (obs.hasCode()) {
            dto.setCode(obs.getCode().getText());
        }

        // Patient ID
        dto.setPatientId(getPatientId(obs));

        // Encounter ID
        dto.setEncounterId(getEncounterId(obs));

        // Extract all data from extensions
        Extension codeTypeExt = obs.getExtensionByUrl(EXT_CODE_TYPE);
        if (codeTypeExt != null && codeTypeExt.getValue() instanceof StringType) {
            dto.setCodeType(((StringType) codeTypeExt.getValue()).getValue());
        }

        Extension modifierExt = obs.getExtensionByUrl(EXT_MODIFIER);
        if (modifierExt != null && modifierExt.getValue() instanceof StringType) {
            dto.setModifier(((StringType) modifierExt.getValue()).getValue());
        }

        Extension categoryExt = obs.getExtensionByUrl(EXT_CATEGORY);
        if (categoryExt != null && categoryExt.getValue() instanceof StringType) {
            dto.setCategory(((StringType) categoryExt.getValue()).getValue());
        }

        Extension shortDescExt = obs.getExtensionByUrl(EXT_SHORT_DESC);
        if (shortDescExt != null && shortDescExt.getValue() instanceof StringType) {
            dto.setShortDescription(((StringType) shortDescExt.getValue()).getValue());
        }

        Extension diagExt = obs.getExtensionByUrl(EXT_DIAGNOSIS_REPORTING);
        if (diagExt != null && diagExt.getValue() instanceof BooleanType) {
            dto.setDiagnosisReporting(((BooleanType) diagExt.getValue()).booleanValue());
        }

        Extension svcExt = obs.getExtensionByUrl(EXT_SERVICE_REPORTING);
        if (svcExt != null && svcExt.getValue() instanceof BooleanType) {
            dto.setServiceReporting(((BooleanType) svcExt.getValue()).booleanValue());
        }

        Extension relateExt = obs.getExtensionByUrl(EXT_RELATE_TO);
        if (relateExt != null && relateExt.getValue() instanceof StringType) {
            dto.setRelateTo(((StringType) relateExt.getValue()).getValue());
        }

        Extension feeExt = obs.getExtensionByUrl(EXT_FEE_STANDARD);
        if (feeExt != null && feeExt.getValue() instanceof DecimalType) {
            dto.setFeeStandard(((DecimalType) feeExt.getValue()).getValue().doubleValue());
        }

        // Description from notes
        if (obs.hasNote()) {
            dto.setDescription(obs.getNoteFirstRep().getText());
        }

        // Active status
        dto.setActive(obs.getStatus() != Observation.ObservationStatus.CANCELLED);

        // E-signed
        Extension esignedExt = obs.getExtensionByUrl(EXT_ESIGNED);
        if (esignedExt != null && esignedExt.getValue() instanceof BooleanType) {
            dto.setESigned(((BooleanType) esignedExt.getValue()).booleanValue());
        }

        Extension signedByExt = obs.getExtensionByUrl(EXT_SIGNED_BY);
        if (signedByExt != null && signedByExt.getValue() instanceof StringType) {
            dto.setSignedBy(((StringType) signedByExt.getValue()).getValue());
        }

        Extension signedAtExt = obs.getExtensionByUrl(EXT_SIGNED_AT);
        if (signedAtExt != null && signedAtExt.getValue() instanceof StringType) {
            dto.setSignedAt(((StringType) signedAtExt.getValue()).getValue());
        }

        // Audit
        CodeDto.Audit audit = new CodeDto.Audit();
        audit.setCreatedDate(LocalDate.now().format(DAY));
        audit.setLastModifiedDate(LocalDate.now().format(DAY));
        dto.setAudit(audit);

        return dto;
    }

    private Long getPatientId(Observation obs) {
        if (obs.hasSubject() && obs.getSubject().hasReference()) {
            String ref = obs.getSubject().getReference();
            if (ref.startsWith("Patient/")) {
                try {
                    return Long.parseLong(ref.substring(8));
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    private Long getEncounterId(Observation obs) {
        if (obs.hasEncounter() && obs.getEncounter().hasReference()) {
            String ref = obs.getEncounter().getReference();
            if (ref.startsWith("Encounter/")) {
                try {
                    return Long.parseLong(ref.substring(10));
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }
}
