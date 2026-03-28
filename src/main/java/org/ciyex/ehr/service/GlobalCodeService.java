package org.ciyex.ehr.service;

import org.ciyex.ehr.dto.GlobalCodeDto;
import org.ciyex.ehr.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-only GlobalCode Service (global billing/diagnosis code definitions).
 * Uses FHIR Basic resource directly via FhirClientService.
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
        log.debug("Creating FHIR Basic (GlobalCode): {}", dto.getCode());
        String practiceId = getPracticeId();

        Basic basic = toFhirBasic(dto);
        var outcome = fhirClientService.create(basic, practiceId);
        String fhirId = outcome.getId().getIdPart();

        try {
            dto.setId(Long.parseLong(fhirId));
        } catch (NumberFormatException e) {
            dto.setId((long) Math.abs(fhirId.hashCode()));
        }
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        
        Basic created = (Basic) outcome.getResource();
        if (created != null && created.hasMeta()) {
            populateAudit(dto, created.getMeta());
        }

        log.info("Created FHIR Basic (GlobalCode) with id: {}", fhirId);
        return dto;
    }

    // UPDATE
    public GlobalCodeDto update(String fhirId, GlobalCodeDto dto) {
        validateMandatory(dto);
        log.debug("Updating GlobalCode: {}", fhirId);
        String practiceId = getPracticeId();

        Basic basic = toFhirBasic(dto);
        basic.setId(fhirId);
        fhirClientService.update(basic, practiceId);

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        return dto;
    }

    // DELETE
    public void delete(String fhirId) {
        log.debug("Deleting GlobalCode: {}", fhirId);
        String practiceId = getPracticeId();
        fhirClientService.delete(Basic.class, fhirId, practiceId);
    }

    // GET ONE
    public GlobalCodeDto getOne(String fhirId) {
        log.debug("Getting GlobalCode: {}", fhirId);
        String practiceId = getPracticeId();
        try {
            Basic basic = fhirClientService.read(Basic.class, fhirId, practiceId);
            return fromFhirBasic(basic);
        } catch (ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException e) {
            throw new org.ciyex.ehr.exception.ResourceNotFoundException("GlobalCode", "id", fhirId);
        }
    }

    // GET ALL
    public List<GlobalCodeDto> getAll() {
        log.debug("Getting all GlobalCodes");
        String practiceId = getPracticeId();
        Bundle bundle = fhirClientService.search(Basic.class, practiceId);
        return fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(this::isGlobalCodeBasic)
                .map(this::fromFhirBasic)
                .collect(Collectors.toList());
    }

    // SEARCH
    public List<GlobalCodeDto> search(String codeType, Boolean active, String q) {
        log.debug("Searching GlobalCodes: codeType={} active={} q={}", codeType, active, q);
        String practiceId = getPracticeId();
        Bundle bundle = fhirClientService.search(Basic.class, practiceId);
        return fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(this::isGlobalCodeBasic)
                .map(this::fromFhirBasic)
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

    private Basic toFhirBasic(GlobalCodeDto dto) {
        Basic basic = new Basic();
        
        // Code type identifier
        basic.getCode().addCoding()
                .setCode("global-code")
                .setDisplay("Global Code Definition");

        // Code and description in extensions
        if (dto.getCode() != null) {
            basic.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/code", new StringType(dto.getCode())));
        }
        if (dto.getDescription() != null) {
            basic.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/description", new StringType(dto.getDescription())));
        }

        // Code type (ICD9, ICD10, CPT4, HCPCS, CUSTOM)
        if (dto.getCodeType() != null) {
            basic.addExtension(new Extension(EXT_CODE_TYPE, new StringType(dto.getCodeType())));
        }

        // Modifier
        if (dto.getModifier() != null) {
            basic.addExtension(new Extension(EXT_MODIFIER, new StringType(dto.getModifier())));
        }

        // Category
        if (dto.getCategory() != null) {
            basic.addExtension(new Extension(EXT_CATEGORY, new StringType(dto.getCategory())));
        }

        // Short description
        if (dto.getShortDescription() != null) {
            basic.addExtension(new Extension(EXT_SHORT_DESC, new StringType(dto.getShortDescription())));
        }

        // Diagnosis reporting
        if (dto.getDiagnosisReporting() != null) {
            basic.addExtension(new Extension(EXT_DIAGNOSIS_REPORTING, new BooleanType(dto.getDiagnosisReporting())));
        }

        // Service reporting
        if (dto.getServiceReporting() != null) {
            basic.addExtension(new Extension(EXT_SERVICE_REPORTING, new BooleanType(dto.getServiceReporting())));
        }

        // Relate to
        if (dto.getRelateTo() != null) {
            basic.addExtension(new Extension(EXT_RELATE_TO, new StringType(dto.getRelateTo())));
        }

        // Fee standard
        if (dto.getFeeStandard() != null) {
            basic.addExtension(new Extension(EXT_FEE_STANDARD, new DecimalType(dto.getFeeStandard())));
        }

        return basic;
    }

    private GlobalCodeDto fromFhirBasic(Basic basic) {
        GlobalCodeDto dto = new GlobalCodeDto();

        String fhirId = basic.getIdElement().getIdPart();
        try {
            dto.setId(Long.parseLong(fhirId));
        } catch (NumberFormatException e) {
            dto.setId((long) Math.abs(fhirId.hashCode()));
        }
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);

        // Code from extension
        Extension codeExt = basic.getExtensionByUrl("http://ciyex.com/fhir/StructureDefinition/code");
        if (codeExt != null && codeExt.getValue() instanceof StringType) {
            dto.setCode(((StringType) codeExt.getValue()).getValue());
        }

        // Description from extension
        Extension descExt = basic.getExtensionByUrl("http://ciyex.com/fhir/StructureDefinition/description");
        if (descExt != null && descExt.getValue() instanceof StringType) {
            dto.setDescription(((StringType) descExt.getValue()).getValue());
        }

        // Code type
        Extension codeTypeExt = basic.getExtensionByUrl(EXT_CODE_TYPE);
        if (codeTypeExt != null && codeTypeExt.getValue() instanceof StringType) {
            dto.setCodeType(((StringType) codeTypeExt.getValue()).getValue());
        }

        // Modifier
        Extension modifierExt = basic.getExtensionByUrl(EXT_MODIFIER);
        if (modifierExt != null && modifierExt.getValue() instanceof StringType) {
            dto.setModifier(((StringType) modifierExt.getValue()).getValue());
        }

        // Category
        Extension categoryExt = basic.getExtensionByUrl(EXT_CATEGORY);
        if (categoryExt != null && categoryExt.getValue() instanceof StringType) {
            dto.setCategory(((StringType) categoryExt.getValue()).getValue());
        }

        // Short description
        Extension shortDescExt = basic.getExtensionByUrl(EXT_SHORT_DESC);
        if (shortDescExt != null && shortDescExt.getValue() instanceof StringType) {
            dto.setShortDescription(((StringType) shortDescExt.getValue()).getValue());
        }

        // Diagnosis reporting
        Extension diagExt = basic.getExtensionByUrl(EXT_DIAGNOSIS_REPORTING);
        if (diagExt != null && diagExt.getValue() instanceof BooleanType) {
            dto.setDiagnosisReporting(((BooleanType) diagExt.getValue()).booleanValue());
        }

        // Service reporting
        Extension svcExt = basic.getExtensionByUrl(EXT_SERVICE_REPORTING);
        if (svcExt != null && svcExt.getValue() instanceof BooleanType) {
            dto.setServiceReporting(((BooleanType) svcExt.getValue()).booleanValue());
        }

        // Relate to
        Extension relateExt = basic.getExtensionByUrl(EXT_RELATE_TO);
        if (relateExt != null && relateExt.getValue() instanceof StringType) {
            dto.setRelateTo(((StringType) relateExt.getValue()).getValue());
        }

        // Fee standard
        Extension feeExt = basic.getExtensionByUrl(EXT_FEE_STANDARD);
        if (feeExt != null && feeExt.getValue() instanceof DecimalType) {
            dto.setFeeStandard(((DecimalType) feeExt.getValue()).getValue());
        }

        // Active status (default true)
        dto.setActive(true);
        
        if (basic.hasMeta()) {
            populateAudit(dto, basic.getMeta());
        }

        return dto;
    }

    private boolean isGlobalCodeBasic(Basic basic) {
        return basic.getCode().hasCoding() && 
               basic.getCode().getCoding().stream()
                       .anyMatch(coding -> "global-code".equals(coding.getCode()));
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
    
    private void populateAudit(GlobalCodeDto dto, Meta meta) {
        GlobalCodeDto.Audit audit = new GlobalCodeDto.Audit();
        if (meta.hasLastUpdated()) {
            audit.setLastModifiedDate(meta.getLastUpdated().toInstant().atOffset(ZoneOffset.UTC).toLocalDate().toString());
            audit.setCreatedDate(meta.getLastUpdated().toInstant().atOffset(ZoneOffset.UTC).toLocalDate().toString());
        }
        dto.setAudit(audit);
    }
}
