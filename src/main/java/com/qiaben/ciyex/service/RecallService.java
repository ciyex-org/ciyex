package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import com.qiaben.ciyex.dto.RecallDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
        log.debug("Creating FHIR Flag (recall) for patient: {}", dto.getPatientId());

        Flag flag = toFhirFlag(dto);
        var outcome = fhirClientService.create(flag, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setFhirId(fhirId);
        log.info("Created FHIR Flag (recall) with id: {}", fhirId);

        return dto;
    }

    // GET BY ID
    public RecallDto getById(String fhirId) {
        log.debug("Getting FHIR Flag (recall): {}", fhirId);
        Flag flag = fhirClientService.read(Flag.class, fhirId, getPracticeId());
        return fromFhirFlag(flag);
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
        log.debug("Updating FHIR Flag (recall): {}", fhirId);

        Flag flag = toFhirFlag(dto);
        flag.setId(fhirId);
        fhirClientService.update(flag, getPracticeId());

        dto.setFhirId(fhirId);
        return dto;
    }

    // DELETE
    public void delete(String fhirId) {
        log.debug("Deleting FHIR Flag (recall): {}", fhirId);
        fhirClientService.delete(Flag.class, fhirId, getPracticeId());
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
        dto.setFhirId(f.getIdElement().getIdPart());

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
}
