package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.GlobalCodeDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-only GlobalCode Service (global billing/diagnosis code definitions).
 * Uses FHIR CodeSystem resource directly via FhirClientService.
 * No local database storage - all data stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GlobalCodeService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final String EXT_CODE_TYPE = "http://ciyex.com/fhir/StructureDefinition/code-type";
    private static final String EXT_MODIFIER = "http://ciyex.com/fhir/StructureDefinition/modifier";
    private static final String EXT_CATEGORY = "http://ciyex.com/fhir/StructureDefinition/category";
    private static final String EXT_SHORT_DESC = "http://ciyex.com/fhir/StructureDefinition/short-description";
    private static final String EXT_DIAGNOSIS_REPORTING = "http://ciyex.com/fhir/StructureDefinition/diagnosis-reporting";
    private static final String EXT_SERVICE_REPORTING = "http://ciyex.com/fhir/StructureDefinition/service-reporting";
    private static final String EXT_RELATE_TO = "http://ciyex.com/fhir/StructureDefinition/relate-to";
    private static final String EXT_FEE_STANDARD = "http://ciyex.com/fhir/StructureDefinition/fee-standard";

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public GlobalCodeDto create(GlobalCodeDto dto) {
        validateMandatory(dto);
        log.debug("Creating FHIR CodeSystem (GlobalCode): {}", dto.getCode());

        CodeSystem cs = toFhirCodeSystem(dto);
        var outcome = fhirClientService.create(cs, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setId((long) Math.abs(fhirId.hashCode()));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);

        log.info("Created FHIR CodeSystem (GlobalCode) with id: {}", fhirId);
        return dto;
    }

    // UPDATE
    public GlobalCodeDto update(String fhirId, GlobalCodeDto dto) {
        validateMandatory(dto);
        log.debug("Updating GlobalCode: {}", fhirId);

        CodeSystem cs = toFhirCodeSystem(dto);
        cs.setId(fhirId);
        fhirClientService.update(cs, getPracticeId());

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        return dto;
    }

    // DELETE
    public void delete(String fhirId) {
        log.debug("Deleting GlobalCode: {}", fhirId);
        fhirClientService.delete(CodeSystem.class, fhirId, getPracticeId());
    }

    // GET ONE
    public GlobalCodeDto getOne(String fhirId) {
        log.debug("Getting GlobalCode: {}", fhirId);
        CodeSystem cs = fhirClientService.read(CodeSystem.class, fhirId, getPracticeId());
        return fromFhirCodeSystem(cs);
    }

    // GET ALL
    public List<GlobalCodeDto> getAll() {
        log.debug("Getting all GlobalCodes");
        Bundle bundle = fhirClientService.search(CodeSystem.class, getPracticeId());
        return fhirClientService.extractResources(bundle, CodeSystem.class).stream()
                .filter(this::isGlobalCodeSystem)
                .map(this::fromFhirCodeSystem)
                .collect(Collectors.toList());
    }

    // SEARCH
    public List<GlobalCodeDto> search(String codeType, Boolean active, String q) {
        log.debug("Searching GlobalCodes: codeType={} active={} q={}", codeType, active, q);
        Bundle bundle = fhirClientService.search(CodeSystem.class, getPracticeId());
        return fhirClientService.extractResources(bundle, CodeSystem.class).stream()
                .filter(this::isGlobalCodeSystem)
                .map(this::fromFhirCodeSystem)
                .filter(dto -> codeType == null || codeType.equals(dto.getCodeType()))
                .filter(dto -> active == null || active.equals(dto.getActive()))
                .filter(dto -> q == null || matchesQuery(dto, q))
                .collect(Collectors.toList());
    }

    private boolean matchesQuery(GlobalCodeDto dto, String q) {
        String lower = q.toLowerCase();
        return (dto.getCode() != null && dto.getCode().toLowerCase().contains(lower)) ||
               (dto.getDescription() != null && dto.getDescription().toLowerCase().contains(lower)) ||
               (dto.getShortDescription() != null && dto.getShortDescription().toLowerCase().contains(lower));
    }

    // -------- FHIR Mapping --------

    private CodeSystem toFhirCodeSystem(GlobalCodeDto dto) {
        CodeSystem cs = new CodeSystem();
        cs.setStatus(Enumerations.PublicationStatus.ACTIVE);

        // Mark as global code
        cs.addIdentifier().setSystem("http://ciyex.com/fhir/codesystem-type").setValue("global-code");

        // Code and description
        if (dto.getCode() != null) {
            cs.setName(dto.getCode());
            cs.setTitle(dto.getShortDescription() != null ? dto.getShortDescription() : dto.getCode());
            CodeSystem.ConceptDefinitionComponent concept = cs.addConcept();
            concept.setCode(dto.getCode());
            concept.setDisplay(dto.getShortDescription() != null ? dto.getShortDescription() : dto.getCode());
            if (dto.getDescription() != null) {
                concept.setDefinition(dto.getDescription());
            }
        }

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
            cs.addExtension(new Extension(EXT_FEE_STANDARD, new DecimalType(dto.getFeeStandard())));
        }

        // Active status
        if (dto.getActive() != null && !dto.getActive()) {
            cs.setStatus(Enumerations.PublicationStatus.RETIRED);
        }

        return cs;
    }

    private GlobalCodeDto fromFhirCodeSystem(CodeSystem cs) {
        GlobalCodeDto dto = new GlobalCodeDto();

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
            dto.setFeeStandard(((DecimalType) feeExt.getValue()).getValue());
        }

        // Active status
        dto.setActive(cs.getStatus() != Enumerations.PublicationStatus.RETIRED);

        // Audit
        GlobalCodeDto.Audit audit = new GlobalCodeDto.Audit();
        audit.setCreatedDate(LocalDate.now().format(DAY));
        audit.setLastModifiedDate(LocalDate.now().format(DAY));
        dto.setAudit(audit);

        return dto;
    }

    private boolean isGlobalCodeSystem(CodeSystem cs) {
        return cs.getIdentifier().stream()
                .anyMatch(id -> "http://ciyex.com/fhir/codesystem-type".equals(id.getSystem()) && "global-code".equals(id.getValue()));
    }

    private void validateMandatory(GlobalCodeDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("GlobalCode payload is required");
        }
        if (!StringUtils.hasText(dto.getCodeType())) {
            throw new IllegalArgumentException("codeType is mandatory");
        }
        if (!StringUtils.hasText(dto.getCode())) {
            throw new IllegalArgumentException("code is mandatory");
        }
    }
}
