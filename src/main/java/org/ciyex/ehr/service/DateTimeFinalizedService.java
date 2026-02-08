package org.ciyex.ehr.service;

import org.ciyex.ehr.dto.DateTimeFinalizedDto;
import org.ciyex.ehr.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-only DateTimeFinalized Service.
 * Uses FHIR Provenance resource for storing finalization records.
 * No local database storage - all data stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DateTimeFinalizedService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final String EXT_PATIENT = "http://ciyex.com/fhir/StructureDefinition/patient-id";
    private static final String EXT_ENCOUNTER = "http://ciyex.com/fhir/StructureDefinition/encounter-id";
    private static final String EXT_TARGET_TYPE = "http://ciyex.com/fhir/StructureDefinition/target-type";
    private static final String EXT_TARGET_ID = "http://ciyex.com/fhir/StructureDefinition/target-id";
    private static final String EXT_TARGET_VERSION = "http://ciyex.com/fhir/StructureDefinition/target-version";
    private static final String EXT_FINALIZED_AT = "http://ciyex.com/fhir/StructureDefinition/finalized-at";
    private static final String EXT_FINALIZED_BY = "http://ciyex.com/fhir/StructureDefinition/finalized-by";
    private static final String EXT_FINALIZER_ROLE = "http://ciyex.com/fhir/StructureDefinition/finalizer-role";
    private static final String EXT_METHOD = "http://ciyex.com/fhir/StructureDefinition/method";
    private static final String EXT_STATUS = "http://ciyex.com/fhir/StructureDefinition/status";
    private static final String EXT_REASON = "http://ciyex.com/fhir/StructureDefinition/reason";
    private static final String EXT_COMMENTS = "http://ciyex.com/fhir/StructureDefinition/comments";
    private static final String EXT_CONTENT_HASH = "http://ciyex.com/fhir/StructureDefinition/content-hash";
    private static final String EXT_PROVIDER_SIG_ID = "http://ciyex.com/fhir/StructureDefinition/provider-signature-id";
    private static final String EXT_SIGNOFF_ID = "http://ciyex.com/fhir/StructureDefinition/signoff-id";
    private static final String EXT_ESIGNED = "http://ciyex.com/fhir/StructureDefinition/e-signed";
    private static final String EXT_SIGNED_AT = "http://ciyex.com/fhir/StructureDefinition/signed-at";
    private static final String EXT_SIGNED_BY = "http://ciyex.com/fhir/StructureDefinition/signed-by";
    private static final String EXT_PRINTED_AT = "http://ciyex.com/fhir/StructureDefinition/printed-at";

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public DateTimeFinalizedDto create(Long patientId, Long encounterId, DateTimeFinalizedDto dto) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        log.debug("Creating FHIR Provenance (DateTimeFinalized) for patient {} encounter {}", patientId, encounterId);

        Provenance prov = toFhirProvenance(dto, patientId, encounterId);
        var outcome = fhirClientService.create(prov, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setId((long) Math.abs(fhirId.hashCode()));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);
        
        Provenance created = (Provenance) outcome.getResource();
        if (created != null && created.hasMeta()) {
            populateAudit(dto, created.getMeta());
        }

        log.info("Created FHIR Provenance (DateTimeFinalized) with id: {}", fhirId);
        return dto;
    }

    // GET ONE
    public DateTimeFinalizedDto getOne(Long patientId, Long encounterId, String fhirId) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validateFhirId(fhirId, "DateTimeFinalized ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        log.debug("Getting DateTimeFinalized {} for patient {} encounter {}", fhirId, patientId, encounterId);
        try {
            Provenance prov = fhirClientService.read(Provenance.class, fhirId, getPracticeId());
            DateTimeFinalizedDto dto = fromFhirProvenance(prov);
            dto.setId((long) Math.abs(fhirId.hashCode()));
            return dto;
        } catch (Exception e) {
            throw new IllegalArgumentException("DateTimeFinalized ID is invalid. DateTimeFinalized not found: " + fhirId);
        }
    }

    // LIST by encounter
    public List<DateTimeFinalizedDto> list(Long patientId, Long encounterId) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        log.debug("Listing DateTimeFinalized for patient {} encounter {}", patientId, encounterId);
        Bundle bundle = fhirClientService.search(Provenance.class, getPracticeId());

        return fhirClientService.extractResources(bundle, Provenance.class).stream()
                .filter(p -> patientId.equals(getPatientId(p)) && encounterId.equals(getEncounterId(p)))
                .map(this::fromFhirProvenance)
                .collect(Collectors.toList());
    }

    // GET ALL by patient
    public List<DateTimeFinalizedDto> getAllByPatient(Long patientId) {
        validatePathVariable(patientId, "Patient ID");
        log.debug("Getting all DateTimeFinalized for patient {}", patientId);
        Bundle bundle = fhirClientService.search(Provenance.class, getPracticeId());

        return fhirClientService.extractResources(bundle, Provenance.class).stream()
                .filter(p -> patientId.equals(getPatientId(p)))
                .map(this::fromFhirProvenance)
                .collect(Collectors.toList());
    }

    // UPDATE
    public DateTimeFinalizedDto update(Long patientId, Long encounterId, String fhirId, DateTimeFinalizedDto dto) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validateFhirId(fhirId, "DateTimeFinalized ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        log.debug("Updating DateTimeFinalized {} for patient {} encounter {}", fhirId, patientId, encounterId);

        Provenance existing;
        try {
            existing = fhirClientService.read(Provenance.class, fhirId, getPracticeId());
        } catch (Exception e) {
            throw new IllegalArgumentException("DateTimeFinalized ID is invalid. DateTimeFinalized not found: " + fhirId);
        }

        if (Boolean.TRUE.equals(getBoolExt(existing, EXT_ESIGNED))) {
            throw new IllegalStateException("Signed finalizations are read-only.");
        }

        Provenance prov = toFhirProvenance(dto, patientId, encounterId);
        prov.setId(fhirId);
        fhirClientService.update(prov, getPracticeId());

        return getOne(patientId, encounterId, fhirId);
    }

    // DELETE
    public void delete(Long patientId, Long encounterId, String fhirId) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validateFhirId(fhirId, "DateTimeFinalized ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        log.debug("Deleting DateTimeFinalized {} for patient {} encounter {}", fhirId, patientId, encounterId);

        Provenance existing;
        try {
            existing = fhirClientService.read(Provenance.class, fhirId, getPracticeId());
        } catch (Exception e) {
            throw new IllegalArgumentException("DateTimeFinalized ID is invalid. DateTimeFinalized not found: " + fhirId);
        }

        if (Boolean.TRUE.equals(getBoolExt(existing, EXT_ESIGNED))) {
            throw new IllegalStateException("Signed finalizations cannot be deleted.");
        }

        fhirClientService.delete(Provenance.class, fhirId, getPracticeId());
    }

    // ESIGN
    public DateTimeFinalizedDto eSign(Long patientId, Long encounterId, String fhirId, String signedBy) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validateFhirId(fhirId, "DateTimeFinalized ID");
        log.debug("E-signing DateTimeFinalized {} for patient {} encounter {}", fhirId, patientId, encounterId);

        Provenance prov = fhirClientService.read(Provenance.class, fhirId, getPracticeId());

        // Already signed - return as-is
        if (Boolean.TRUE.equals(getBoolExt(prov, EXT_ESIGNED))) {
            return fromFhirProvenance(prov);
        }

        // Set eSigned fields
        prov.getExtension().removeIf(e -> EXT_ESIGNED.equals(e.getUrl()) || EXT_SIGNED_AT.equals(e.getUrl()) || EXT_SIGNED_BY.equals(e.getUrl()));
        prov.addExtension(new Extension(EXT_ESIGNED, new BooleanType(true)));
        prov.addExtension(new Extension(EXT_SIGNED_AT, new StringType(OffsetDateTime.now(ZoneOffset.UTC).toString())));
        prov.addExtension(new Extension(EXT_SIGNED_BY, new StringType(StringUtils.hasText(signedBy) ? signedBy : "system")));

        // Set status to finalized if not set
        if (getStringExt(prov, EXT_STATUS) == null) {
            prov.addExtension(new Extension(EXT_STATUS, new StringType("finalized")));
        }

        fhirClientService.update(prov, getPracticeId());
        return fromFhirProvenance(prov);
    }

    // PRINT PDF - returns empty for now
    public byte[] renderPdf(Long patientId, Long encounterId, String fhirId) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validateFhirId(fhirId, "DateTimeFinalized ID");
        log.debug("Rendering PDF for DateTimeFinalized {} patient {} encounter {}", fhirId, patientId, encounterId);

        // Mark as printed
        Provenance prov = fhirClientService.read(Provenance.class, fhirId, getPracticeId());
        prov.getExtension().removeIf(e -> EXT_PRINTED_AT.equals(e.getUrl()));
        prov.addExtension(new Extension(EXT_PRINTED_AT, new StringType(OffsetDateTime.now(ZoneOffset.UTC).toString())));
        fhirClientService.update(prov, getPracticeId());

        return new byte[0];
    }

    // -------- FHIR Mapping --------

    private Provenance toFhirProvenance(DateTimeFinalizedDto dto, Long patientId, Long encounterId) {
        Provenance prov = new Provenance();

        prov.addTarget(new Reference("Encounter/" + encounterId));
        prov.setRecorded(new java.util.Date());

        prov.addExtension(new Extension(EXT_PATIENT, new StringType(patientId.toString())));
        prov.addExtension(new Extension(EXT_ENCOUNTER, new StringType(encounterId.toString())));

        if (StringUtils.hasText(dto.getTargetType())) {
            prov.addExtension(new Extension(EXT_TARGET_TYPE, new StringType(dto.getTargetType())));
        }
        if (dto.getTargetId() != null) {
            prov.addExtension(new Extension(EXT_TARGET_ID, new StringType(dto.getTargetId().toString())));
        }
        if (StringUtils.hasText(dto.getTargetVersion())) {
            prov.addExtension(new Extension(EXT_TARGET_VERSION, new StringType(dto.getTargetVersion())));
        }
        if (StringUtils.hasText(dto.getFinalizedAt())) {
            prov.addExtension(new Extension(EXT_FINALIZED_AT, new StringType(dto.getFinalizedAt())));
        }
        if (StringUtils.hasText(dto.getFinalizedBy())) {
            prov.addExtension(new Extension(EXT_FINALIZED_BY, new StringType(dto.getFinalizedBy())));
        }
        if (StringUtils.hasText(dto.getFinalizerRole())) {
            prov.addExtension(new Extension(EXT_FINALIZER_ROLE, new StringType(dto.getFinalizerRole())));
        }
        if (StringUtils.hasText(dto.getMethod())) {
            prov.addExtension(new Extension(EXT_METHOD, new StringType(dto.getMethod())));
        }
        if (StringUtils.hasText(dto.getStatus())) {
            prov.addExtension(new Extension(EXT_STATUS, new StringType(dto.getStatus())));
        }
        if (StringUtils.hasText(dto.getReason())) {
            prov.addExtension(new Extension(EXT_REASON, new StringType(dto.getReason())));
        }
        if (StringUtils.hasText(dto.getComments())) {
            prov.addExtension(new Extension(EXT_COMMENTS, new StringType(dto.getComments())));
        }
        if (StringUtils.hasText(dto.getContentHash())) {
            prov.addExtension(new Extension(EXT_CONTENT_HASH, new StringType(dto.getContentHash())));
        }
        if (dto.getProviderSignatureId() != null) {
            prov.addExtension(new Extension(EXT_PROVIDER_SIG_ID, new StringType(dto.getProviderSignatureId().toString())));
        }
        if (dto.getSignoffId() != null) {
            prov.addExtension(new Extension(EXT_SIGNOFF_ID, new StringType(dto.getSignoffId().toString())));
        }

        return prov;
    }

    private DateTimeFinalizedDto fromFhirProvenance(Provenance prov) {
        DateTimeFinalizedDto dto = new DateTimeFinalizedDto();

        String fhirId = prov.getIdElement().getIdPart();
        dto.setId((long) Math.abs(fhirId.hashCode()));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);

        dto.setPatientId(getPatientId(prov));
        dto.setEncounterId(getEncounterId(prov));

        dto.setTargetType(getStringExt(prov, EXT_TARGET_TYPE));
        dto.setTargetId(getLongExt(prov, EXT_TARGET_ID));
        dto.setTargetVersion(getStringExt(prov, EXT_TARGET_VERSION));
        dto.setFinalizedAt(getStringExt(prov, EXT_FINALIZED_AT));
        dto.setFinalizedBy(getStringExt(prov, EXT_FINALIZED_BY));
        dto.setFinalizerRole(getStringExt(prov, EXT_FINALIZER_ROLE));
        dto.setMethod(getStringExt(prov, EXT_METHOD));
        dto.setStatus(getStringExt(prov, EXT_STATUS));
        dto.setReason(getStringExt(prov, EXT_REASON));
        dto.setComments(getStringExt(prov, EXT_COMMENTS));
        dto.setContentHash(getStringExt(prov, EXT_CONTENT_HASH));
        dto.setProviderSignatureId(getLongExt(prov, EXT_PROVIDER_SIG_ID));
        dto.setSignoffId(getLongExt(prov, EXT_SIGNOFF_ID));

        dto.setESigned(getBoolExt(prov, EXT_ESIGNED));
        String signedAtStr = getStringExt(prov, EXT_SIGNED_AT);
        if (signedAtStr != null) {
            try {
                dto.setSignedAt(OffsetDateTime.parse(signedAtStr));
            } catch (Exception ignored) {}
        }
        dto.setSignedBy(getStringExt(prov, EXT_SIGNED_BY));
        String printedAtStr = getStringExt(prov, EXT_PRINTED_AT);
        if (printedAtStr != null) {
            try {
                dto.setPrintedAt(OffsetDateTime.parse(printedAtStr));
            } catch (Exception ignored) {}
        }

        DateTimeFinalizedDto.Audit audit = new DateTimeFinalizedDto.Audit();
        audit.setCreatedDate(LocalDate.now().format(DAY));
        audit.setLastModifiedDate(LocalDate.now().format(DAY));
        dto.setAudit(audit);
        
        if (prov.hasMeta()) {
            populateAudit(dto, prov.getMeta());
        }

        return dto;
    }

    private Long getPatientId(Provenance prov) {
        return getLongExt(prov, EXT_PATIENT);
    }

    private Long getEncounterId(Provenance prov) {
        return getLongExt(prov, EXT_ENCOUNTER);
    }

    private String getStringExt(Provenance prov, String url) {
        Extension ext = prov.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) {
            return ((StringType) ext.getValue()).getValue();
        }
        return null;
    }

    private Long getLongExt(Provenance prov, String url) {
        Extension ext = prov.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) {
            try {
                return Long.parseLong(((StringType) ext.getValue()).getValue());
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private Boolean getBoolExt(Provenance prov, String url) {
        Extension ext = prov.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof BooleanType) {
            return ((BooleanType) ext.getValue()).booleanValue();
        }
        return null;
    }
    
    private void populateAudit(DateTimeFinalizedDto dto, Meta meta) {
        DateTimeFinalizedDto.Audit audit = dto.getAudit() != null ? dto.getAudit() : new DateTimeFinalizedDto.Audit();
        if (meta.hasLastUpdated()) {
            audit.setLastModifiedDate(meta.getLastUpdated().toInstant().atOffset(ZoneOffset.UTC).toLocalDate().toString());
            if (audit.getCreatedDate() == null) {
                audit.setCreatedDate(meta.getLastUpdated().toInstant().atOffset(ZoneOffset.UTC).toLocalDate().toString());
            }
        }
        dto.setAudit(audit);
    }
    
    // ✅ Validate path variables
    private void validatePathVariable(Long value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is invalid. " + fieldName + " cannot be null");
        }
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " is invalid. " + fieldName + " must be a positive number. Provided: " + value);
        }
    }
    
    private void validateFhirId(String fhirId, String fieldName) {
        if (fhirId == null || fhirId.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is invalid. " + fieldName + " cannot be null or empty");
        }
    }
    
    private void validatePatientExists(Long patientId) {
        try {
            fhirClientService.read(Patient.class, String.valueOf(patientId), getPracticeId());
        } catch (Exception e) {
            throw new IllegalArgumentException("Patient ID is invalid. Patient not found: " + patientId);
        }
    }
    
    private void validateEncounterExists(Long encounterId) {
        try {
            fhirClientService.read(Encounter.class, String.valueOf(encounterId), getPracticeId());
        } catch (Exception e) {
            throw new IllegalArgumentException("Encounter ID is invalid. Encounter not found: " + encounterId);
        }
    }
}
