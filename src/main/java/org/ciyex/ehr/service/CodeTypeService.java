package org.ciyex.ehr.service;

import org.ciyex.ehr.dto.CodeTypeDto;
import org.ciyex.ehr.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-only CodeType Service (code type definitions per encounter).
 * Uses FHIR Basic resource directly via FhirClientService.
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

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public CodeTypeDto create(Long patientId, Long encounterId, CodeTypeDto dto) {
        validateMandatory(dto);
        log.debug("Creating FHIR Basic (CodeType) for patient {} encounter {}", patientId, encounterId);

        Basic basic = toFhirBasic(dto, patientId, encounterId);
        var outcome = fhirClientService.create(basic, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        try {
            dto.setId(Long.parseLong(fhirId));
        } catch (NumberFormatException e) {
            dto.setId((long) Math.abs(fhirId.hashCode()));
        }
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);
        
        Basic created = (Basic) outcome.getResource();
        if (created != null && created.hasMeta()) {
            populateAudit(dto, created.getMeta());
        }

        log.info("Created FHIR Basic (CodeType) with id: {}", fhirId);
        return dto;
    }

    // UPDATE
    public CodeTypeDto update(Long patientId, Long encounterId, String fhirId, CodeTypeDto dto) {
        log.debug("Updating CodeType {} for patient {} encounter {}", fhirId, patientId, encounterId);

        Basic basic = toFhirBasic(dto, patientId, encounterId);
        basic.setId(fhirId);
        fhirClientService.update(basic, getPracticeId());

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);
        return dto;
    }

    // DELETE
    public void delete(Long patientId, Long encounterId, String fhirId) {
        log.debug("Deleting CodeType {} for patient {} encounter {}", fhirId, patientId, encounterId);
        fhirClientService.delete(Basic.class, fhirId, getPracticeId());
    }

    // GET ONE
    public CodeTypeDto getOne(Long patientId, Long encounterId, String fhirId) {
        log.debug("Getting CodeType {} for patient {} encounter {}", fhirId, patientId, encounterId);
        Basic basic = fhirClientService.read(Basic.class, fhirId, getPracticeId());
        return fromFhirBasic(basic);
    }

    // GET ALL by encounter
    public List<CodeTypeDto> getAllByEncounter(Long patientId, Long encounterId) {
        log.debug("Getting all CodeTypes for patient {} encounter {}", patientId, encounterId);
        Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());
        List<CodeTypeDto> result = fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(this::isCodeTypeBasic)
                .filter(basic -> patientId.equals(getPatientId(basic)) && encounterId.equals(getEncounterId(basic)))
                .map(this::fromFhirBasic)
                .collect(Collectors.toList());
        if (result.isEmpty()) {
            throw new IllegalArgumentException("No CodeTypes found for patientId=" + patientId + ", encounterId=" + encounterId);
        }
        return result;
    }

    // SEARCH
    public List<CodeTypeDto> searchInEncounter(Long patientId, Long encounterId, String codeTypeKey, Boolean active, String q) {
        log.debug("Searching CodeTypes for patient {} encounter {} key={} active={} q={}", patientId, encounterId, codeTypeKey, active, q);
        Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());
        return fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(this::isCodeTypeBasic)
                .filter(basic -> patientId.equals(getPatientId(basic)) && encounterId.equals(getEncounterId(basic)))
                .map(this::fromFhirBasic)
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

    private Basic toFhirBasic(CodeTypeDto dto, Long patientId, Long encounterId) {
        Basic basic = new Basic();
        
        // Code type identifier
        basic.getCode().addCoding()
                .setCode("code-type")
                .setDisplay("Code Type Definition");

        // Patient and Encounter references
        basic.addExtension(new Extension(EXT_PATIENT, new StringType(patientId.toString())));
        basic.addExtension(new Extension(EXT_ENCOUNTER, new StringType(encounterId.toString())));

        // All fields as extensions
        if (dto.getCodeTypeKey() != null) basic.addExtension(new Extension(EXT_CODE_TYPE_KEY, new StringType(dto.getCodeTypeKey())));
        if (dto.getCodeTypeId() != null) basic.addExtension(new Extension(EXT_CODE_TYPE_ID, new IntegerType(dto.getCodeTypeId())));
        if (dto.getSequenceNumber() != null) basic.addExtension(new Extension(EXT_SEQUENCE_NUMBER, new IntegerType(dto.getSequenceNumber())));
        if (dto.getModifier() != null) basic.addExtension(new Extension(EXT_MODIFIER, new IntegerType(dto.getModifier())));
        if (dto.getJustification() != null) basic.addExtension(new Extension(EXT_JUSTIFICATION, new StringType(dto.getJustification())));
        if (dto.getMask() != null) basic.addExtension(new Extension(EXT_MASK, new StringType(dto.getMask())));
        if (dto.getFeeApplicable() != null) basic.addExtension(new Extension(EXT_FEE_APPLICABLE, new BooleanType(dto.getFeeApplicable())));
        if (dto.getRelatedIndicator() != null) basic.addExtension(new Extension(EXT_RELATED_INDICATOR, new BooleanType(dto.getRelatedIndicator())));
        if (dto.getNumberOfServices() != null) basic.addExtension(new Extension(EXT_NUM_SERVICES, new BooleanType(dto.getNumberOfServices())));
        if (dto.getDiagnosisFlag() != null) basic.addExtension(new Extension(EXT_DIAGNOSIS_FLAG, new BooleanType(dto.getDiagnosisFlag())));
        if (dto.getLabel() != null) basic.addExtension(new Extension(EXT_LABEL, new StringType(dto.getLabel())));
        if (dto.getExternalFlag() != null) basic.addExtension(new Extension(EXT_EXTERNAL_FLAG, new BooleanType(dto.getExternalFlag())));
        if (dto.getClaimFlag() != null) basic.addExtension(new Extension(EXT_CLAIM_FLAG, new BooleanType(dto.getClaimFlag())));
        if (dto.getProcedureFlag() != null) basic.addExtension(new Extension(EXT_PROCEDURE_FLAG, new BooleanType(dto.getProcedureFlag())));
        if (dto.getTerminologyFlag() != null) basic.addExtension(new Extension(EXT_TERMINOLOGY_FLAG, new BooleanType(dto.getTerminologyFlag())));
        if (dto.getProblemFlag() != null) basic.addExtension(new Extension(EXT_PROBLEM_FLAG, new BooleanType(dto.getProblemFlag())));
        if (dto.getDrugFlag() != null) basic.addExtension(new Extension(EXT_DRUG_FLAG, new BooleanType(dto.getDrugFlag())));

        return basic;
    }

    private CodeTypeDto fromFhirBasic(Basic basic) {
        CodeTypeDto dto = new CodeTypeDto();

        String fhirId = basic.getIdElement().getIdPart();
        try {
            dto.setId(Long.parseLong(fhirId));
        } catch (NumberFormatException e) {
            dto.setId((long) Math.abs(fhirId.hashCode()));
        }
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setPatientId(getPatientId(basic));
        dto.setEncounterId(getEncounterId(basic));

        dto.setCodeTypeKey(getStringExt(basic, EXT_CODE_TYPE_KEY));
        dto.setCodeTypeId(getIntExt(basic, EXT_CODE_TYPE_ID));
        dto.setSequenceNumber(getIntExt(basic, EXT_SEQUENCE_NUMBER));
        dto.setModifier(getIntExt(basic, EXT_MODIFIER));
        dto.setJustification(getStringExt(basic, EXT_JUSTIFICATION));
        dto.setMask(getStringExt(basic, EXT_MASK));
        dto.setFeeApplicable(getBoolExt(basic, EXT_FEE_APPLICABLE));
        dto.setRelatedIndicator(getBoolExt(basic, EXT_RELATED_INDICATOR));
        dto.setNumberOfServices(getBoolExt(basic, EXT_NUM_SERVICES));
        dto.setDiagnosisFlag(getBoolExt(basic, EXT_DIAGNOSIS_FLAG));
        dto.setLabel(getStringExt(basic, EXT_LABEL));
        dto.setExternalFlag(getBoolExt(basic, EXT_EXTERNAL_FLAG));
        dto.setClaimFlag(getBoolExt(basic, EXT_CLAIM_FLAG));
        dto.setProcedureFlag(getBoolExt(basic, EXT_PROCEDURE_FLAG));
        dto.setTerminologyFlag(getBoolExt(basic, EXT_TERMINOLOGY_FLAG));
        dto.setProblemFlag(getBoolExt(basic, EXT_PROBLEM_FLAG));
        dto.setDrugFlag(getBoolExt(basic, EXT_DRUG_FLAG));

        dto.setActive(true);
        
        if (basic.hasMeta()) {
            populateAudit(dto, basic.getMeta());
        }

        return dto;
    }

    private boolean isCodeTypeBasic(Basic basic) {
        return basic.getCode().hasCoding() && 
               basic.getCode().getCoding().stream()
                       .anyMatch(coding -> "code-type".equals(coding.getCode()));
    }

    private Long getPatientId(Basic basic) {
        Extension ext = basic.getExtensionByUrl(EXT_PATIENT);
        if (ext != null && ext.getValue() instanceof StringType) {
            try { return Long.parseLong(((StringType) ext.getValue()).getValue()); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private Long getEncounterId(Basic basic) {
        Extension ext = basic.getExtensionByUrl(EXT_ENCOUNTER);
        if (ext != null && ext.getValue() instanceof StringType) {
            try { return Long.parseLong(((StringType) ext.getValue()).getValue()); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private String getStringExt(Basic basic, String url) {
        Extension ext = basic.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) return ((StringType) ext.getValue()).getValue();
        return null;
    }

    private Integer getIntExt(Basic basic, String url) {
        Extension ext = basic.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof IntegerType) return ((IntegerType) ext.getValue()).getValue();
        return null;
    }

    private Boolean getBoolExt(Basic basic, String url) {
        Extension ext = basic.getExtensionByUrl(url);
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
    
    private void populateAudit(CodeTypeDto dto, Meta meta) {
        CodeTypeDto.Audit audit = new CodeTypeDto.Audit();
        if (meta.hasLastUpdated()) {
            audit.setLastModifiedDate(meta.getLastUpdated().toInstant().atOffset(ZoneOffset.UTC).toLocalDate().toString());
            audit.setCreatedDate(meta.getLastUpdated().toInstant().atOffset(ZoneOffset.UTC).toLocalDate().toString());
        }
        dto.setAudit(audit);
    }
}
