package org.ciyex.ehr.service;

import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import org.ciyex.ehr.dto.RecallDto;
import org.ciyex.ehr.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-only Recall Service.
 * Uses FHIR Flag resource directly via FhirClientService.
 * No local database storage - all data stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecallService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    // Extension URLs
    private static final String EXT_PATIENT_NAME = "http://ciyex.com/fhir/StructureDefinition/patient-name";
    private static final String EXT_DOB = "http://ciyex.com/fhir/StructureDefinition/dob";
    private static final String EXT_PHONE = "http://ciyex.com/fhir/StructureDefinition/phone";
    private static final String EXT_EMAIL = "http://ciyex.com/fhir/StructureDefinition/email";
    private static final String EXT_ADDRESS = "http://ciyex.com/fhir/StructureDefinition/address";
    private static final String EXT_CITY = "http://ciyex.com/fhir/StructureDefinition/city";
    private static final String EXT_STATE = "http://ciyex.com/fhir/StructureDefinition/state";
    private static final String EXT_ZIP = "http://ciyex.com/fhir/StructureDefinition/zip";
    private static final String EXT_LAST_VISIT = "http://ciyex.com/fhir/StructureDefinition/last-visit";
    private static final String EXT_RECALL_DATE = "http://ciyex.com/fhir/StructureDefinition/recall-date";
    private static final String EXT_RECALL_REASON = "http://ciyex.com/fhir/StructureDefinition/recall-reason";
    private static final String EXT_SMS_CONSENT = "http://ciyex.com/fhir/StructureDefinition/sms-consent";
    private static final String EXT_EMAIL_CONSENT = "http://ciyex.com/fhir/StructureDefinition/email-consent";
    private static final String EXT_PROVIDER_ID = "http://ciyex.com/fhir/StructureDefinition/provider-id";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public RecallDto create(RecallDto dto) {
        validateMandatoryFields(dto);
        
        log.debug("Creating FHIR Flag (recall) for patient: {}", dto.getPatientId());

        try {
            Flag flag = toFhirFlag(dto);
            var outcome = fhirClientService.create(flag, getPracticeId());
            String fhirId = outcome.getId().getIdPart();

            dto.setFhirId(fhirId);
            dto.setId(Long.parseLong(fhirId));

            // Set audit information
            RecallDto.Audit audit = new RecallDto.Audit();
            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            audit.setCreatedDate(currentTime);
            audit.setLastModifiedDate(currentTime);
            dto.setAudit(audit);

            log.info("Created FHIR Flag (recall) with id: {}", fhirId);

            return dto;
        } catch (Exception e) {
            log.error("Failed to create recall: {}", e.getMessage());
            throw new RuntimeException("Failed to create recall: " + e.getMessage(), e);
        }
    }

    // GET BY ID
    public RecallDto getById(String fhirId) {
        if (fhirId == null || fhirId.trim().isEmpty()) {
            throw new IllegalArgumentException("Recall ID cannot be null or empty");
        }
        
        log.debug("Getting FHIR Flag (recall): {}", fhirId);
        
        try {
            Flag flag = fhirClientService.read(Flag.class, fhirId, getPracticeId());
            if (flag == null) {
                throw new IllegalArgumentException("Recall not found for recallId=" + fhirId);
            }
            return fromFhirFlag(flag);
        } catch (Exception e) {
            log.error("Failed to retrieve recall with ID {}: {}", fhirId, e.getMessage());
            throw new RuntimeException("Failed to retrieve recall: Recall not found for recallId=" + fhirId);
        }
    }

    // GET ALL
    public List<RecallDto> getAll() {
        log.debug("Getting all FHIR Flags (recall)");

        Bundle bundle = fhirClientService.search(Flag.class, getPracticeId());
        List<Flag> flags = fhirClientService.extractResources(bundle, Flag.class);

        return flags.stream()
                .filter(f -> isRecallFlag(f))
                .map(this::fromFhirFlag)
                .collect(Collectors.toList());
    }

    // GET ALL (Paginated)
    public Page<RecallDto> getAll(Pageable pageable) {
        List<RecallDto> all = getAll();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<RecallDto> pageContent = all.subList(start, end);
        return new PageImpl<>(pageContent, pageable, all.size());
    }

    // UPDATE
    public RecallDto update(String fhirId, RecallDto dto) {
        if (fhirId == null || fhirId.trim().isEmpty()) {
            throw new IllegalArgumentException("Recall ID cannot be null or empty");
        }
        
        validateMandatoryFields(dto);
        
        log.debug("Updating FHIR Flag (recall): {}", fhirId);

        try {
            // Verify recall exists
            fhirClientService.read(Flag.class, fhirId, getPracticeId());
            
            Flag flag = toFhirFlag(dto);
            flag.setId(fhirId);
            fhirClientService.update(flag, getPracticeId());

            dto.setFhirId(fhirId);
            dto.setId(Long.parseLong(fhirId));

            // Set audit information
            RecallDto.Audit audit = new RecallDto.Audit();
            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            if (dto.getAudit() != null && dto.getAudit().getCreatedDate() != null) {
                audit.setCreatedDate(dto.getAudit().getCreatedDate());
            } else {
                audit.setCreatedDate(currentTime);
            }
            audit.setLastModifiedDate(currentTime);
            dto.setAudit(audit);

            return dto;
        } catch (Exception e) {
            log.error("Failed to update recall with ID {}: {}", fhirId, e.getMessage());
            throw new RuntimeException("Failed to update recall: Recall not found for recallId=" + fhirId);
        }
    }

    // DELETE
    public void delete(String fhirId) {
        if (fhirId == null || fhirId.trim().isEmpty()) {
            throw new IllegalArgumentException("Recall ID cannot be null or empty");
        }
        
        log.debug("Deleting FHIR Flag (recall): {}", fhirId);
        
        try {
            // Verify recall exists before deleting
            fhirClientService.read(Flag.class, fhirId, getPracticeId());
            fhirClientService.delete(Flag.class, fhirId, getPracticeId());
            log.info("Successfully deleted recall with ID: {}", fhirId);
        } catch (Exception e) {
            log.error("Failed to delete recall with ID {}: {}", fhirId, e.getMessage());
            throw new RuntimeException("Failed to delete recall: Recall not found for recallId=" + fhirId);
        }
    }

    // -------- FHIR Mapping --------

    private Flag toFhirFlag(RecallDto dto) {
        Flag f = new Flag();
        f.setStatus(Flag.FlagStatus.ACTIVE);

        // Category = recall
        f.addCategory().addCoding()
                .setSystem("http://ciyex.com/fhir/CodeSystem/flag-category")
                .setCode("recall")
                .setDisplay("Patient Recall");

        // Subject (Patient)
        if (dto.getPatientId() != null) {
            f.setSubject(new Reference("Patient/" + dto.getPatientId()));
        }

        // Code (recall reason)
        if (dto.getRecallReason() != null) {
            f.getCode().setText(dto.getRecallReason());
        }

        // Extensions
        addStringExtension(f, EXT_PATIENT_NAME, dto.getPatientName());
        addStringExtension(f, EXT_DOB, dto.getDob());
        addStringExtension(f, EXT_PHONE, dto.getPhone());
        addStringExtension(f, EXT_EMAIL, dto.getEmail());
        addStringExtension(f, EXT_ADDRESS, dto.getAddress());
        addStringExtension(f, EXT_CITY, dto.getCity());
        addStringExtension(f, EXT_STATE, dto.getState());
        addStringExtension(f, EXT_ZIP, dto.getZip());
        addStringExtension(f, EXT_LAST_VISIT, dto.getLastVisit());
        addStringExtension(f, EXT_RECALL_DATE, dto.getRecallDate());
        addStringExtension(f, EXT_RECALL_REASON, dto.getRecallReason());

        if (dto.getProviderId() != null) {
            f.addExtension(new Extension(EXT_PROVIDER_ID, new StringType(dto.getProviderId().toString())));
        }

        f.addExtension(new Extension(EXT_SMS_CONSENT, new BooleanType(dto.isSmsConsent())));
        f.addExtension(new Extension(EXT_EMAIL_CONSENT, new BooleanType(dto.isEmailConsent())));

        return f;
    }

    private RecallDto fromFhirFlag(Flag f) {
        RecallDto dto = new RecallDto();
        String fhirId = f.getIdElement().getIdPart();
        dto.setFhirId(fhirId);
        dto.setId(Long.parseLong(fhirId));

        // Set audit information
        RecallDto.Audit audit = new RecallDto.Audit();
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        audit.setCreatedDate(currentTime);
        audit.setLastModifiedDate(currentTime);
        dto.setAudit(audit);

        // Subject -> patientId
        if (f.hasSubject() && f.getSubject().hasReference()) {
            String ref = f.getSubject().getReference();
            if (ref.startsWith("Patient/")) {
                try {
                    dto.setPatientId(Long.parseLong(ref.substring("Patient/".length())));
                } catch (NumberFormatException ignored) {}
            }
        }

        // Code -> recallReason
        if (f.hasCode() && f.getCode().hasText()) {
            dto.setRecallReason(f.getCode().getText());
        }

        // Extensions
        dto.setPatientName(getExtensionString(f, EXT_PATIENT_NAME));
        dto.setDob(getExtensionString(f, EXT_DOB));
        dto.setPhone(getExtensionString(f, EXT_PHONE));
        dto.setEmail(getExtensionString(f, EXT_EMAIL));
        dto.setAddress(getExtensionString(f, EXT_ADDRESS));
        dto.setCity(getExtensionString(f, EXT_CITY));
        dto.setState(getExtensionString(f, EXT_STATE));
        dto.setZip(getExtensionString(f, EXT_ZIP));
        dto.setLastVisit(getExtensionString(f, EXT_LAST_VISIT));
        dto.setRecallDate(getExtensionString(f, EXT_RECALL_DATE));

        String providerIdStr = getExtensionString(f, EXT_PROVIDER_ID);
        if (providerIdStr != null) {
            try {
                dto.setProviderId(Long.parseLong(providerIdStr));
            } catch (NumberFormatException ignored) {}
        }

        dto.setSmsConsent(getExtensionBoolean(f, EXT_SMS_CONSENT));
        dto.setEmailConsent(getExtensionBoolean(f, EXT_EMAIL_CONSENT));

        return dto;
    }

    // -------- Helpers --------

    private boolean isRecallFlag(Flag f) {
        if (!f.hasCategory()) return false;
        return f.getCategory().stream()
                .flatMap(cc -> cc.getCoding().stream())
                .anyMatch(c -> "recall".equals(c.getCode()));
    }

    private void addStringExtension(Flag f, String url, String value) {
        if (value != null) {
            f.addExtension(new Extension(url, new StringType(value)));
        }
    }

    private String getExtensionString(Flag f, String url) {
        Extension ext = f.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) {
            return ((StringType) ext.getValue()).getValue();
        }
        return null;
    }

    private boolean getExtensionBoolean(Flag f, String url) {
        Extension ext = f.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof BooleanType) {
            return ((BooleanType) ext.getValue()).booleanValue();
        }
        return false;
    }
    
    private void validateMandatoryFields(RecallDto dto) {
        StringBuilder errors = new StringBuilder();
        
        if (dto == null) {
            throw new IllegalArgumentException("Recall data cannot be null");
        }
        
        if (dto.getPatientName() == null || dto.getPatientName().trim().isEmpty()) {
            errors.append("patientName, ");
        }
        
        if (dto.getLastVisit() == null || dto.getLastVisit().trim().isEmpty()) {
            errors.append("lastVisit, ");
        }
        
        if (dto.getRecallDate() == null || dto.getRecallDate().trim().isEmpty()) {
            errors.append("recallDate, ");
        }
        
        if (errors.length() > 0) {
            errors.setLength(errors.length() - 2);
            throw new IllegalArgumentException("Missing mandatory fields: " + errors);
        }
    }
}
