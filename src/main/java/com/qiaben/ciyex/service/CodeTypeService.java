package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.CodeTypeDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-only CodeType Service (code type definitions per encounter).
 * Uses FHIR ValueSet resource directly via FhirClientService.
 * No local database storage - all data stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CodeTypeService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final String EXT_PATIENT = "http://ciyex.com/fhir/StructureDefinition/patient-reference";
    private static final String EXT_ENCOUNTER = "http://ciyex.com/fhir/StructureDefinition/encounter-reference";
    private static final String EXT_CODE_TYPE_KEY = "http://ciyex.com/fhir/StructureDefinition/code-type-key";
    private static final String EXT_CODE_TYPE_ID = "http://ciyex.com/fhir/StructureDefinition/code-type-id";
    private static final String EXT_SEQUENCE_NUMBER = "http://ciyex.com/fhir/StructureDefinition/sequence-number";
    private static final String EXT_MODIFIER = "http://ciyex.com/fhir/StructureDefinition/modifier";
    private static final String EXT_JUSTIFICATION = "http://ciyex.com/fhir/StructureDefinition/justification";
    private static final String EXT_MASK = "http://ciyex.com/fhir/StructureDefinition/mask";
    private static final String EXT_FEE_APPLICABLE = "http://ciyex.com/fhir/StructureDefinition/fee-applicable";
    private static final String EXT_RELATED_INDICATOR = "http://ciyex.com/fhir/StructureDefinition/related-indicator";
    private static final String EXT_NUM_SERVICES = "http://ciyex.com/fhir/StructureDefinition/number-of-services";
    private static final String EXT_DIAGNOSIS_FLAG = "http://ciyex.com/fhir/StructureDefinition/diagnosis-flag";
    private static final String EXT_LABEL = "http://ciyex.com/fhir/StructureDefinition/label";
    private static final String EXT_EXTERNAL_FLAG = "http://ciyex.com/fhir/StructureDefinition/external-flag";
    private static final String EXT_CLAIM_FLAG = "http://ciyex.com/fhir/StructureDefinition/claim-flag";
    private static final String EXT_PROCEDURE_FLAG = "http://ciyex.com/fhir/StructureDefinition/procedure-flag";
    private static final String EXT_TERMINOLOGY_FLAG = "http://ciyex.com/fhir/StructureDefinition/terminology-flag";
    private static final String EXT_PROBLEM_FLAG = "http://ciyex.com/fhir/StructureDefinition/problem-flag";
    private static final String EXT_DRUG_FLAG = "http://ciyex.com/fhir/StructureDefinition/drug-flag";

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public CodeTypeDto create(Long patientId, Long encounterId, CodeTypeDto dto) {
        validateMandatory(dto);
        log.debug("Creating FHIR ValueSet (CodeType) for patient {} encounter {}", patientId, encounterId);

        ValueSet vs = toFhirValueSet(dto, patientId, encounterId);
        var outcome = fhirClientService.create(vs, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setId((long) Math.abs(fhirId.hashCode()));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);

        log.info("Created FHIR ValueSet (CodeType) with id: {}", fhirId);
        return dto;
    }

    // UPDATE
    public CodeTypeDto update(Long patientId, Long encounterId, String fhirId, CodeTypeDto dto) {
        log.debug("Updating CodeType {} for patient {} encounter {}", fhirId, patientId, encounterId);

        ValueSet vs = toFhirValueSet(dto, patientId, encounterId);
        vs.setId(fhirId);
        fhirClientService.update(vs, getPracticeId());

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);
        return dto;
    }

    // DELETE
    public void delete(Long patientId, Long encounterId, String fhirId) {
        log.debug("Deleting CodeType {} for patient {} encounter {}", fhirId, patientId, encounterId);
        fhirClientService.delete(ValueSet.class, fhirId, getPracticeId());
    }

    // GET ONE
    public CodeTypeDto getOne(Long patientId, Long encounterId, String fhirId) {
        log.debug("Getting CodeType {} for patient {} encounter {}", fhirId, patientId, encounterId);
        ValueSet vs = fhirClientService.read(ValueSet.class, fhirId, getPracticeId());
        return fromFhirValueSet(vs);
    }

    // GET ALL by encounter
    public List<CodeTypeDto> getAllByEncounter(Long patientId, Long encounterId) {
        log.debug("Getting all CodeTypes for patient {} encounter {}", patientId, encounterId);
        Bundle bundle = fhirClientService.search(ValueSet.class, getPracticeId());
        List<CodeTypeDto> result = fhirClientService.extractResources(bundle, ValueSet.class).stream()
                .filter(vs -> isCodeTypeValueSet(vs))
                .filter(vs -> patientId.equals(getPatientId(vs)) && encounterId.equals(getEncounterId(vs)))
                .map(this::fromFhirValueSet)
                .collect(Collectors.toList());
        if (result.isEmpty()) {
            throw new IllegalArgumentException("No CodeTypes found for patientId=" + patientId + ", encounterId=" + encounterId);
        }
        return result;
    }

    // SEARCH
    public List<CodeTypeDto> searchInEncounter(Long patientId, Long encounterId, String codeTypeKey, Boolean active, String q) {
        log.debug("Searching CodeTypes for patient {} encounter {} key={} active={} q={}", patientId, encounterId, codeTypeKey, active, q);
        Bundle bundle = fhirClientService.search(ValueSet.class, getPracticeId());
        return fhirClientService.extractResources(bundle, ValueSet.class).stream()
                .filter(vs -> isCodeTypeValueSet(vs))
                .filter(vs -> patientId.equals(getPatientId(vs)) && encounterId.equals(getEncounterId(vs)))
                .map(this::fromFhirValueSet)
                .filter(dto -> codeTypeKey == null || codeTypeKey.equals(dto.getCodeTypeKey()))
                .filter(dto -> active == null || active.equals(dto.getActive()))
                .filter(dto -> q == null || matchesQuery(dto, q))
                .collect(Collectors.toList());
    }

    private boolean matchesQuery(CodeTypeDto dto, String q) {
        String lower = q.toLowerCase();
        return (dto.getCodeTypeKey() != null && dto.getCodeTypeKey().toLowerCase().contains(lower)) ||
               (dto.getLabel() != null && dto.getLabel().toLowerCase().contains(lower)) ||
               (dto.getJustification() != null && dto.getJustification().toLowerCase().contains(lower));
    }

    // -------- FHIR Mapping --------

    private ValueSet toFhirValueSet(CodeTypeDto dto, Long patientId, Long encounterId) {
        ValueSet vs = new ValueSet();
        vs.setStatus(Enumerations.PublicationStatus.ACTIVE);

        // Name/Title
        if (dto.getCodeTypeKey() != null) {
            vs.setName(dto.getCodeTypeKey());
            vs.setTitle(dto.getLabel() != null ? dto.getLabel() : dto.getCodeTypeKey());
        }

        // Mark as CodeType ValueSet
        vs.addIdentifier().setSystem("http://ciyex.com/fhir/valueset-type").setValue("code-type");

        // Patient reference
        vs.addExtension(new Extension(EXT_PATIENT, new StringType(patientId.toString())));
        vs.addExtension(new Extension(EXT_ENCOUNTER, new StringType(encounterId.toString())));

        // All fields as extensions
        if (dto.getCodeTypeKey() != null) vs.addExtension(new Extension(EXT_CODE_TYPE_KEY, new StringType(dto.getCodeTypeKey())));
        if (dto.getCodeTypeId() != null) vs.addExtension(new Extension(EXT_CODE_TYPE_ID, new IntegerType(dto.getCodeTypeId())));
        if (dto.getSequenceNumber() != null) vs.addExtension(new Extension(EXT_SEQUENCE_NUMBER, new IntegerType(dto.getSequenceNumber())));
        if (dto.getModifier() != null) vs.addExtension(new Extension(EXT_MODIFIER, new IntegerType(dto.getModifier())));
        if (dto.getJustification() != null) vs.addExtension(new Extension(EXT_JUSTIFICATION, new StringType(dto.getJustification())));
        if (dto.getMask() != null) vs.addExtension(new Extension(EXT_MASK, new StringType(dto.getMask())));
        if (dto.getFeeApplicable() != null) vs.addExtension(new Extension(EXT_FEE_APPLICABLE, new BooleanType(dto.getFeeApplicable())));
        if (dto.getRelatedIndicator() != null) vs.addExtension(new Extension(EXT_RELATED_INDICATOR, new BooleanType(dto.getRelatedIndicator())));
        if (dto.getNumberOfServices() != null) vs.addExtension(new Extension(EXT_NUM_SERVICES, new BooleanType(dto.getNumberOfServices())));
        if (dto.getDiagnosisFlag() != null) vs.addExtension(new Extension(EXT_DIAGNOSIS_FLAG, new BooleanType(dto.getDiagnosisFlag())));
        if (dto.getLabel() != null) vs.addExtension(new Extension(EXT_LABEL, new StringType(dto.getLabel())));
        if (dto.getExternalFlag() != null) vs.addExtension(new Extension(EXT_EXTERNAL_FLAG, new BooleanType(dto.getExternalFlag())));
        if (dto.getClaimFlag() != null) vs.addExtension(new Extension(EXT_CLAIM_FLAG, new BooleanType(dto.getClaimFlag())));
        if (dto.getProcedureFlag() != null) vs.addExtension(new Extension(EXT_PROCEDURE_FLAG, new BooleanType(dto.getProcedureFlag())));
        if (dto.getTerminologyFlag() != null) vs.addExtension(new Extension(EXT_TERMINOLOGY_FLAG, new BooleanType(dto.getTerminologyFlag())));
        if (dto.getProblemFlag() != null) vs.addExtension(new Extension(EXT_PROBLEM_FLAG, new BooleanType(dto.getProblemFlag())));
        if (dto.getDrugFlag() != null) vs.addExtension(new Extension(EXT_DRUG_FLAG, new BooleanType(dto.getDrugFlag())));

        if (dto.getActive() != null && !dto.getActive()) {
            vs.setStatus(Enumerations.PublicationStatus.RETIRED);
        }

        return vs;
    }

    private CodeTypeDto fromFhirValueSet(ValueSet vs) {
        CodeTypeDto dto = new CodeTypeDto();

        String fhirId = vs.getIdElement().getIdPart();
        dto.setId((long) Math.abs(fhirId.hashCode()));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setPatientId(getPatientId(vs));
        dto.setEncounterId(getEncounterId(vs));

        dto.setCodeTypeKey(getStringExt(vs, EXT_CODE_TYPE_KEY));
        dto.setCodeTypeId(getIntExt(vs, EXT_CODE_TYPE_ID));
        dto.setSequenceNumber(getIntExt(vs, EXT_SEQUENCE_NUMBER));
        dto.setModifier(getIntExt(vs, EXT_MODIFIER));
        dto.setJustification(getStringExt(vs, EXT_JUSTIFICATION));
        dto.setMask(getStringExt(vs, EXT_MASK));
        dto.setFeeApplicable(getBoolExt(vs, EXT_FEE_APPLICABLE));
        dto.setRelatedIndicator(getBoolExt(vs, EXT_RELATED_INDICATOR));
        dto.setNumberOfServices(getBoolExt(vs, EXT_NUM_SERVICES));
        dto.setDiagnosisFlag(getBoolExt(vs, EXT_DIAGNOSIS_FLAG));
        dto.setLabel(getStringExt(vs, EXT_LABEL));
        dto.setExternalFlag(getBoolExt(vs, EXT_EXTERNAL_FLAG));
        dto.setClaimFlag(getBoolExt(vs, EXT_CLAIM_FLAG));
        dto.setProcedureFlag(getBoolExt(vs, EXT_PROCEDURE_FLAG));
        dto.setTerminologyFlag(getBoolExt(vs, EXT_TERMINOLOGY_FLAG));
        dto.setProblemFlag(getBoolExt(vs, EXT_PROBLEM_FLAG));
        dto.setDrugFlag(getBoolExt(vs, EXT_DRUG_FLAG));

        dto.setActive(vs.getStatus() != Enumerations.PublicationStatus.RETIRED);

        CodeTypeDto.Audit audit = new CodeTypeDto.Audit();
        audit.setCreatedDate(LocalDate.now().format(DAY));
        audit.setLastModifiedDate(LocalDate.now().format(DAY));
        dto.setAudit(audit);

        return dto;
    }

    private boolean isCodeTypeValueSet(ValueSet vs) {
        return vs.getIdentifier().stream()
                .anyMatch(id -> "http://ciyex.com/fhir/valueset-type".equals(id.getSystem()) && "code-type".equals(id.getValue()));
    }

    private Long getPatientId(ValueSet vs) {
        Extension ext = vs.getExtensionByUrl(EXT_PATIENT);
        if (ext != null && ext.getValue() instanceof StringType) {
            try { return Long.parseLong(((StringType) ext.getValue()).getValue()); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private Long getEncounterId(ValueSet vs) {
        Extension ext = vs.getExtensionByUrl(EXT_ENCOUNTER);
        if (ext != null && ext.getValue() instanceof StringType) {
            try { return Long.parseLong(((StringType) ext.getValue()).getValue()); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private String getStringExt(ValueSet vs, String url) {
        Extension ext = vs.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) return ((StringType) ext.getValue()).getValue();
        return null;
    }

    private Integer getIntExt(ValueSet vs, String url) {
        Extension ext = vs.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof IntegerType) return ((IntegerType) ext.getValue()).getValue();
        return null;
    }

    private Boolean getBoolExt(ValueSet vs, String url) {
        Extension ext = vs.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof BooleanType) return ((BooleanType) ext.getValue()).booleanValue();
        return null;
    }

    private void validateMandatory(CodeTypeDto dto) {
        if (dto.getCodeTypeKey() == null || dto.getCodeTypeKey().trim().isEmpty())
            throw new IllegalArgumentException("codeTypeKey is required");
        if (dto.getCodeTypeId() == null)
            throw new IllegalArgumentException("codeTypeId is required");
        if (dto.getModifier() == null)
            throw new IllegalArgumentException("modifier is required");
        if (dto.getSequenceNumber() == null)
            throw new IllegalArgumentException("sequenceNumber is required");
        if (dto.getJustification() == null || dto.getJustification().trim().isEmpty())
            throw new IllegalArgumentException("justification is required");
        if (dto.getMask() == null || dto.getMask().trim().isEmpty())
            throw new IllegalArgumentException("mask is required");
    }
}
