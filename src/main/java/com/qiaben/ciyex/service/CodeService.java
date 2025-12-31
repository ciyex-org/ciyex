package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.CodeDto;
import com.qiaben.ciyex.fhir.FhirClientService;
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
 * Uses FHIR CodeSystem resource directly via FhirClientService.
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
        log.debug("Creating FHIR CodeSystem for patient {} encounter {}", patientId, encounterId);

        CodeSystem cs = toFhirCodeSystem(dto, patientId, encounterId);
        var outcome = fhirClientService.create(cs, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setId((long) Math.abs(fhirId.hashCode()));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);

        log.info("Created FHIR CodeSystem with id: {}", fhirId);
        return dto;
    }

    // GET ALL by patient
    public List<CodeDto> getAllByPatient(Long patientId) {
        log.debug("Getting all codes for patient {}", patientId);
        Bundle bundle = fhirClientService.search(CodeSystem.class, getPracticeId());
        return fhirClientService.extractResources(bundle, CodeSystem.class).stream()
                .filter(cs -> patientId.equals(getPatientId(cs)))
                .map(this::fromFhirCodeSystem)
                .collect(Collectors.toList());
    }

    // LIST by encounter
    public List<CodeDto> list(Long patientId, Long encounterId) {
        log.debug("Listing codes for patient {} encounter {}", patientId, encounterId);
        Bundle bundle = fhirClientService.search(CodeSystem.class, getPracticeId());
        return fhirClientService.extractResources(bundle, CodeSystem.class).stream()
                .filter(cs -> patientId.equals(getPatientId(cs)) && encounterId.equals(getEncounterId(cs)))
                .map(this::fromFhirCodeSystem)
                .collect(Collectors.toList());
    }

    // GET ONE
    public CodeDto getOne(Long patientId, Long encounterId, String fhirId) {
        log.debug("Getting code {} for patient {} encounter {}", fhirId, patientId, encounterId);
        CodeSystem cs = fhirClientService.read(CodeSystem.class, fhirId, getPracticeId());
        return fromFhirCodeSystem(cs);
    }

    // UPDATE
    public CodeDto update(Long patientId, Long encounterId, String fhirId, CodeDto dto) {
        log.debug("Updating code {} for patient {} encounter {}", fhirId, patientId, encounterId);

        CodeSystem cs = toFhirCodeSystem(dto, patientId, encounterId);
        cs.setId(fhirId);
        fhirClientService.update(cs, getPracticeId());

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);
        return dto;
    }

    // DELETE
    public void delete(Long patientId, Long encounterId, String fhirId) {
        log.debug("Deleting code {} for patient {} encounter {}", fhirId, patientId, encounterId);
        fhirClientService.delete(CodeSystem.class, fhirId, getPracticeId());
    }

    // ESIGN
    public CodeDto eSign(Long patientId, Long encounterId, String fhirId, String signedBy) {
        log.debug("E-signing code {} for patient {} encounter {}", fhirId, patientId, encounterId);
        CodeSystem cs = fhirClientService.read(CodeSystem.class, fhirId, getPracticeId());

        cs.getExtension().removeIf(e -> EXT_ESIGNED.equals(e.getUrl()));
        cs.addExtension(new Extension(EXT_ESIGNED, new BooleanType(true)));
        cs.getExtension().removeIf(e -> EXT_SIGNED_BY.equals(e.getUrl()));
        cs.addExtension(new Extension(EXT_SIGNED_BY, new StringType(signedBy != null ? signedBy : "system")));
        cs.getExtension().removeIf(e -> EXT_SIGNED_AT.equals(e.getUrl()));
        cs.addExtension(new Extension(EXT_SIGNED_AT, new StringType(LocalDate.now().format(DAY))));

        fhirClientService.update(cs, getPracticeId());
        return fromFhirCodeSystem(cs);
    }

    // PRINT (PDF) - returns empty for now, can be implemented later
    public byte[] renderPdf(Long patientId, Long encounterId, String fhirId) {
        log.debug("Rendering PDF for code {} patient {} encounter {}", fhirId, patientId, encounterId);
        return new byte[0];
    }

    // -------- FHIR Mapping --------

    private CodeSystem toFhirCodeSystem(CodeDto dto, Long patientId, Long encounterId) {
        CodeSystem cs = new CodeSystem();
        cs.setStatus(Enumerations.PublicationStatus.ACTIVE);

        // Code and description
        if (dto.getCode() != null) {
            cs.setName(dto.getCode());
            cs.setTitle(dto.getCode());
            CodeSystem.ConceptDefinitionComponent concept = cs.addConcept();
            concept.setCode(dto.getCode());
            concept.setDisplay(dto.getShortDescription() != null ? dto.getShortDescription() : dto.getCode());
            if (dto.getDescription() != null) {
                concept.setDefinition(dto.getDescription());
            }
        }

        // Patient reference
        cs.addExtension(new Extension(EXT_PATIENT, new StringType(patientId.toString())));

        // Encounter reference
        cs.addExtension(new Extension(EXT_ENCOUNTER, new StringType(encounterId.toString())));

        // Code type (ICD9, ICD10, CPT4, HCPCS, CUSTOM)
        if (dto.getCodeType() != null) {
            cs.addExtension(new Extension(EXT_CODE_TYPE, new StringType(dto.getCodeType())));
        }

        // Modifier
        if (dto.getModifier() != null) {
            cs.addExtension(new Extension(EXT_MODIFIER, new StringType(dto.getModifier())));
        }

        // Category
        if (dto.getCategory() != null) {
            cs.addExtension(new Extension(EXT_CATEGORY, new StringType(dto.getCategory())));
        }

        // Short description
        if (dto.getShortDescription() != null) {
            cs.addExtension(new Extension(EXT_SHORT_DESC, new StringType(dto.getShortDescription())));
        }

        // Diagnosis reporting
        if (dto.getDiagnosisReporting() != null) {
            cs.addExtension(new Extension(EXT_DIAGNOSIS_REPORTING, new BooleanType(dto.getDiagnosisReporting())));
        }

        // Service reporting
        if (dto.getServiceReporting() != null) {
            cs.addExtension(new Extension(EXT_SERVICE_REPORTING, new BooleanType(dto.getServiceReporting())));
        }

        // Relate to
        if (dto.getRelateTo() != null) {
            cs.addExtension(new Extension(EXT_RELATE_TO, new StringType(dto.getRelateTo())));
        }

        // Fee standard
        if (dto.getFeeStandard() != null) {
            cs.addExtension(new Extension(EXT_FEE_STANDARD, new DecimalType(BigDecimal.valueOf(dto.getFeeStandard()))));
        }

        // Active status
        if (dto.getActive() != null && !dto.getActive()) {
            cs.setStatus(Enumerations.PublicationStatus.RETIRED);
        }

        return cs;
    }

    private CodeDto fromFhirCodeSystem(CodeSystem cs) {
        CodeDto dto = new CodeDto();

        String fhirId = cs.getIdElement().getIdPart();
        dto.setId((long) Math.abs(fhirId.hashCode()));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);

        // Code from concept
        if (cs.hasConcept() && !cs.getConcept().isEmpty()) {
            CodeSystem.ConceptDefinitionComponent concept = cs.getConcept().get(0);
            dto.setCode(concept.getCode());
            dto.setDescription(concept.getDefinition());
        } else if (cs.hasName()) {
            dto.setCode(cs.getName());
        }

        // Patient ID
        dto.setPatientId(getPatientId(cs));

        // Encounter ID
        dto.setEncounterId(getEncounterId(cs));

        // Code type
        Extension codeTypeExt = cs.getExtensionByUrl(EXT_CODE_TYPE);
        if (codeTypeExt != null && codeTypeExt.getValue() instanceof StringType) {
            dto.setCodeType(((StringType) codeTypeExt.getValue()).getValue());
        }

        // Modifier
        Extension modifierExt = cs.getExtensionByUrl(EXT_MODIFIER);
        if (modifierExt != null && modifierExt.getValue() instanceof StringType) {
            dto.setModifier(((StringType) modifierExt.getValue()).getValue());
        }

        // Category
        Extension categoryExt = cs.getExtensionByUrl(EXT_CATEGORY);
        if (categoryExt != null && categoryExt.getValue() instanceof StringType) {
            dto.setCategory(((StringType) categoryExt.getValue()).getValue());
        }

        // Short description
        Extension shortDescExt = cs.getExtensionByUrl(EXT_SHORT_DESC);
        if (shortDescExt != null && shortDescExt.getValue() instanceof StringType) {
            dto.setShortDescription(((StringType) shortDescExt.getValue()).getValue());
        }

        // Diagnosis reporting
        Extension diagExt = cs.getExtensionByUrl(EXT_DIAGNOSIS_REPORTING);
        if (diagExt != null && diagExt.getValue() instanceof BooleanType) {
            dto.setDiagnosisReporting(((BooleanType) diagExt.getValue()).booleanValue());
        }

        // Service reporting
        Extension svcExt = cs.getExtensionByUrl(EXT_SERVICE_REPORTING);
        if (svcExt != null && svcExt.getValue() instanceof BooleanType) {
            dto.setServiceReporting(((BooleanType) svcExt.getValue()).booleanValue());
        }

        // Relate to
        Extension relateExt = cs.getExtensionByUrl(EXT_RELATE_TO);
        if (relateExt != null && relateExt.getValue() instanceof StringType) {
            dto.setRelateTo(((StringType) relateExt.getValue()).getValue());
        }

        // Fee standard
        Extension feeExt = cs.getExtensionByUrl(EXT_FEE_STANDARD);
        if (feeExt != null && feeExt.getValue() instanceof DecimalType) {
            dto.setFeeStandard(((DecimalType) feeExt.getValue()).getValue().doubleValue());
        }

        // Active status
        dto.setActive(cs.getStatus() != Enumerations.PublicationStatus.RETIRED);

        // E-signed
        Extension esignedExt = cs.getExtensionByUrl(EXT_ESIGNED);
        if (esignedExt != null && esignedExt.getValue() instanceof BooleanType) {
            dto.setESigned(((BooleanType) esignedExt.getValue()).booleanValue());
        }

        // Signed by
        Extension signedByExt = cs.getExtensionByUrl(EXT_SIGNED_BY);
        if (signedByExt != null && signedByExt.getValue() instanceof StringType) {
            dto.setSignedBy(((StringType) signedByExt.getValue()).getValue());
        }

        // Signed at
        Extension signedAtExt = cs.getExtensionByUrl(EXT_SIGNED_AT);
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

    private Long getPatientId(CodeSystem cs) {
        Extension ext = cs.getExtensionByUrl(EXT_PATIENT);
        if (ext != null && ext.getValue() instanceof StringType) {
            try {
                return Long.parseLong(((StringType) ext.getValue()).getValue());
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private Long getEncounterId(CodeSystem cs) {
        Extension ext = cs.getExtensionByUrl(EXT_ENCOUNTER);
        if (ext != null && ext.getValue() instanceof StringType) {
            try {
                return Long.parseLong(((StringType) ext.getValue()).getValue());
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
