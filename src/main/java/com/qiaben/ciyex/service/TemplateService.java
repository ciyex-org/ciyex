package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.TemplateDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-only Template Service (email templates).
 * Uses FHIR Basic resource with extensions for template data.
 * No local database storage - all data stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final String TEMPLATE_TYPE_SYSTEM = "http://ciyex.com/fhir/template-type";
    private static final String TEMPLATE_TYPE_CODE = "email-template";
    private static final String EXT_TEMPLATE_NAME = "http://ciyex.com/fhir/StructureDefinition/template-name";
    private static final String EXT_SUBJECT = "http://ciyex.com/fhir/StructureDefinition/template-subject";
    private static final String EXT_BODY = "http://ciyex.com/fhir/StructureDefinition/template-body";
    private static final String EXT_CREATED_DATE = "http://ciyex.com/fhir/StructureDefinition/created-date";
    private static final String EXT_CREATED_BY = "http://ciyex.com/fhir/StructureDefinition/created-by";
    private static final String EXT_MODIFIED_DATE = "http://ciyex.com/fhir/StructureDefinition/last-modified-date";
    private static final String EXT_MODIFIED_BY = "http://ciyex.com/fhir/StructureDefinition/last-modified-by";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public TemplateDto create(TemplateDto dto) {
        log.debug("Creating FHIR Basic (Template): {}", dto.getTemplateName());

        Basic basic = toFhirBasic(dto);
        addAuditMetadata(basic, true);
        
        var outcome = fhirClientService.create(basic, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setId((long) Math.abs(fhirId.hashCode()));
        dto.setExternalId(fhirId);
        dto.setAudit(extractAudit(basic));

        log.info("Created FHIR Basic (Template) with id: {}", fhirId);
        return dto;
    }

    // GET BY ID
    public TemplateDto getById(String fhirId) {
        log.debug("Getting template: {}", fhirId);
        Basic basic = fhirClientService.read(Basic.class, fhirId, getPracticeId());
        return fromFhirBasic(basic);
    }

    // UPDATE
    public TemplateDto update(String fhirId, TemplateDto dto) {
        log.debug("Updating template: {}", fhirId);

        Basic existing = fhirClientService.read(Basic.class, fhirId, getPracticeId());
        Basic basic = toFhirBasic(dto);
        basic.setId(fhirId);
        
        // Preserve created audit, update modified audit
        preserveCreatedAudit(existing, basic);
        addAuditMetadata(basic, false);
        
        fhirClientService.update(basic, getPracticeId());

        dto.setExternalId(fhirId);
        dto.setAudit(extractAudit(basic));
        return dto;
    }

    // DELETE
    public void delete(String fhirId) {
        log.debug("Deleting template: {}", fhirId);
        fhirClientService.delete(Basic.class, fhirId, getPracticeId());
    }

    // GET ALL
    public ApiResponse<List<TemplateDto>> getAllTemplates() {
        log.debug("Getting all templates");
        Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());

        List<TemplateDto> dtos = fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(this::isEmailTemplate)
                .map(this::fromFhirBasic)
                .collect(Collectors.toList());

        return ApiResponse.<List<TemplateDto>>builder()
                .success(true)
                .message("Templates retrieved successfully")
                .data(dtos)
                .build();
    }

    // -------- FHIR Mapping --------

    private Basic toFhirBasic(TemplateDto dto) {
        Basic basic = new Basic();

        // Code to identify as email template
        CodeableConcept code = new CodeableConcept();
        code.addCoding().setSystem(TEMPLATE_TYPE_SYSTEM).setCode(TEMPLATE_TYPE_CODE).setDisplay("Email Template");
        basic.setCode(code);

        // Template name
        if (dto.getTemplateName() != null) {
            basic.addExtension(new Extension(EXT_TEMPLATE_NAME, new StringType(dto.getTemplateName())));
        }

        // Subject
        if (dto.getSubject() != null) {
            basic.addExtension(new Extension(EXT_SUBJECT, new StringType(dto.getSubject())));
        }

        // Body
        if (dto.getBody() != null) {
            basic.addExtension(new Extension(EXT_BODY, new StringType(dto.getBody())));
        }

        return basic;
    }

    private TemplateDto fromFhirBasic(Basic basic) {
        TemplateDto dto = new TemplateDto();

        String fhirId = basic.getIdElement().getIdPart();
        dto.setId((long) Math.abs(fhirId.hashCode()));
        dto.setExternalId(fhirId);

        // Template name
        Extension nameExt = basic.getExtensionByUrl(EXT_TEMPLATE_NAME);
        if (nameExt != null && nameExt.getValue() instanceof StringType) {
            dto.setTemplateName(((StringType) nameExt.getValue()).getValue());
        }

        // Subject
        Extension subjectExt = basic.getExtensionByUrl(EXT_SUBJECT);
        if (subjectExt != null && subjectExt.getValue() instanceof StringType) {
            dto.setSubject(((StringType) subjectExt.getValue()).getValue());
        }

        // Body
        Extension bodyExt = basic.getExtensionByUrl(EXT_BODY);
        if (bodyExt != null && bodyExt.getValue() instanceof StringType) {
            dto.setBody(((StringType) bodyExt.getValue()).getValue());
        }

        dto.setAudit(extractAudit(basic));
        return dto;
    }

    private boolean isEmailTemplate(Basic basic) {
        if (!basic.hasCode()) return false;
        return basic.getCode().getCoding().stream()
                .anyMatch(c -> TEMPLATE_TYPE_SYSTEM.equals(c.getSystem()) && TEMPLATE_TYPE_CODE.equals(c.getCode()));
    }

    // -------- Audit Methods --------

    private void addAuditMetadata(Basic basic, boolean isCreate) {
        String currentUser = getCurrentUser();
        Date now = new Date();

        if (isCreate) {
            basic.addExtension(new Extension(EXT_CREATED_DATE, new DateTimeType(now)));
            basic.addExtension(new Extension(EXT_CREATED_BY, new StringType(currentUser)));
        }

        basic.addExtension(new Extension(EXT_MODIFIED_DATE, new DateTimeType(now)));
        basic.addExtension(new Extension(EXT_MODIFIED_BY, new StringType(currentUser)));
    }

    private void preserveCreatedAudit(Basic existing, Basic updated) {
        Extension createdDate = existing.getExtensionByUrl(EXT_CREATED_DATE);
        Extension createdBy = existing.getExtensionByUrl(EXT_CREATED_BY);
        
        if (createdDate != null) {
            updated.addExtension(createdDate);
        }
        if (createdBy != null) {
            updated.addExtension(createdBy);
        }
    }

    private TemplateDto.Audit extractAudit(Basic basic) {
        TemplateDto.Audit audit = new TemplateDto.Audit();

        Extension createdDate = basic.getExtensionByUrl(EXT_CREATED_DATE);
        if (createdDate != null && createdDate.getValue() instanceof DateTimeType) {
            Date date = ((DateTimeType) createdDate.getValue()).getValue();
            audit.setCreatedAt(new java.sql.Timestamp(date.getTime()).toLocalDateTime());
        }

        Extension modifiedDate = basic.getExtensionByUrl(EXT_MODIFIED_DATE);
        if (modifiedDate != null && modifiedDate.getValue() instanceof DateTimeType) {
            Date date = ((DateTimeType) modifiedDate.getValue()).getValue();
            audit.setUpdatedAt(new java.sql.Timestamp(date.getTime()).toLocalDateTime());
        }

        return audit;
    }

    private String getCurrentUser() {
        try {
            RequestContext context = RequestContext.get();
            if (context != null && context.getAuthToken() != null) {
                return "user-from-token";
            }
        } catch (Exception e) {
            log.debug("Could not extract user from RequestContext: {}", e.getMessage());
        }
        return "system";
    }
}
