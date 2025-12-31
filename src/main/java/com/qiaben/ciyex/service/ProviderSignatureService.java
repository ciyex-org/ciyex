package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ProviderSignatureDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-only Provider Signature Service.
 * Uses FHIR Provenance resource for storing provider signatures.
 * No local database storage - all data stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProviderSignatureService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final String EXT_PATIENT = "http://ciyex.com/fhir/StructureDefinition/patient-id";
    private static final String EXT_ENCOUNTER = "http://ciyex.com/fhir/StructureDefinition/encounter-id";
    private static final String EXT_SIGNED_AT = "http://ciyex.com/fhir/StructureDefinition/signed-at";
    private static final String EXT_SIGNED_BY = "http://ciyex.com/fhir/StructureDefinition/signed-by";
    private static final String EXT_SIGNER_ROLE = "http://ciyex.com/fhir/StructureDefinition/signer-role";
    private static final String EXT_SIG_TYPE = "http://ciyex.com/fhir/StructureDefinition/signature-type";
    private static final String EXT_SIG_FORMAT = "http://ciyex.com/fhir/StructureDefinition/signature-format";
    private static final String EXT_SIG_DATA = "http://ciyex.com/fhir/StructureDefinition/signature-data";
    private static final String EXT_SIG_HASH = "http://ciyex.com/fhir/StructureDefinition/signature-hash";
    private static final String EXT_STATUS = "http://ciyex.com/fhir/StructureDefinition/status";
    private static final String EXT_COMMENTS = "http://ciyex.com/fhir/StructureDefinition/comments";

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public ProviderSignatureDto create(Long patientId, Long encounterId, ProviderSignatureDto dto) {
        log.debug("Creating FHIR Provenance (ProviderSignature) for patient {} encounter {}", patientId, encounterId);

        // Compute hash if signature data present
        if (StringUtils.hasText(dto.getSignatureData())) {
            dto.setSignatureHash(sha256(dto.getSignatureData()));
        }

        Provenance prov = toFhirProvenance(dto, patientId, encounterId);
        var outcome = fhirClientService.create(prov, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setId((long) Math.abs(fhirId.hashCode()));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);

        log.info("Created FHIR Provenance (ProviderSignature) with id: {}", fhirId);
        return dto;
    }

    // ESIGN alias
    public ProviderSignatureDto eSign(Long patientId, Long encounterId, ProviderSignatureDto dto) {
        return create(patientId, encounterId, dto);
    }

    // GET ONE
    public ProviderSignatureDto getOne(Long patientId, Long encounterId, String fhirId) {
        log.debug("Getting provider signature {} for patient {} encounter {}", fhirId, patientId, encounterId);
        Provenance prov = fhirClientService.read(Provenance.class, fhirId, getPracticeId());
        return fromFhirProvenance(prov);
    }

    // LIST by encounter
    public List<ProviderSignatureDto> list(Long patientId, Long encounterId) {
        log.debug("Listing provider signatures for patient {} encounter {}", patientId, encounterId);
        Bundle bundle = fhirClientService.search(Provenance.class, getPracticeId());

        return fhirClientService.extractResources(bundle, Provenance.class).stream()
                .filter(p -> patientId.equals(getPatientId(p)) && encounterId.equals(getEncounterId(p)))
                .map(this::fromFhirProvenance)
                .collect(Collectors.toList());
    }

    // GET ALL by patient
    public List<ProviderSignatureDto> getAllByPatient(Long patientId) {
        log.debug("Getting all provider signatures for patient {}", patientId);
        Bundle bundle = fhirClientService.search(Provenance.class, getPracticeId());

        return fhirClientService.extractResources(bundle, Provenance.class).stream()
                .filter(p -> patientId.equals(getPatientId(p)))
                .map(this::fromFhirProvenance)
                .collect(Collectors.toList());
    }

    // UPDATE
    public ProviderSignatureDto update(Long patientId, Long encounterId, String fhirId, ProviderSignatureDto dto) {
        log.debug("Updating provider signature {} for patient {} encounter {}", fhirId, patientId, encounterId);

        // Compute hash if signature data present
        if (StringUtils.hasText(dto.getSignatureData())) {
            dto.setSignatureHash(sha256(dto.getSignatureData()));
        }

        Provenance prov = toFhirProvenance(dto, patientId, encounterId);
        prov.setId(fhirId);
        fhirClientService.update(prov, getPracticeId());

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);
        return dto;
    }

    // DELETE
    public void delete(Long patientId, Long encounterId, String fhirId) {
        log.debug("Deleting provider signature {} for patient {} encounter {}", fhirId, patientId, encounterId);
        fhirClientService.delete(Provenance.class, fhirId, getPracticeId());
    }

    // PRINT PDF - returns empty for now
    public byte[] renderPdf(Long patientId, Long encounterId, String fhirId) {
        log.debug("Rendering PDF for provider signature {} patient {} encounter {}", fhirId, patientId, encounterId);
        return new byte[0];
    }

    // -------- FHIR Mapping --------

    private Provenance toFhirProvenance(ProviderSignatureDto dto, Long patientId, Long encounterId) {
        Provenance prov = new Provenance();

        // Target (encounter reference)
        prov.addTarget(new Reference("Encounter/" + encounterId));

        // Recorded time
        prov.setRecorded(new java.util.Date());

        // Patient extension
        prov.addExtension(new Extension(EXT_PATIENT, new StringType(patientId.toString())));

        // Encounter extension
        prov.addExtension(new Extension(EXT_ENCOUNTER, new StringType(encounterId.toString())));

        // Signed at
        if (StringUtils.hasText(dto.getSignedAt())) {
            prov.addExtension(new Extension(EXT_SIGNED_AT, new StringType(dto.getSignedAt())));
        } else {
            prov.addExtension(new Extension(EXT_SIGNED_AT, new StringType(OffsetDateTime.now().toString())));
        }

        // Signed by
        if (StringUtils.hasText(dto.getSignedBy())) {
            prov.addExtension(new Extension(EXT_SIGNED_BY, new StringType(dto.getSignedBy())));
        }

        // Signer role
        if (StringUtils.hasText(dto.getSignerRole())) {
            prov.addExtension(new Extension(EXT_SIGNER_ROLE, new StringType(dto.getSignerRole())));
        }

        // Signature type
        if (StringUtils.hasText(dto.getSignatureType())) {
            prov.addExtension(new Extension(EXT_SIG_TYPE, new StringType(dto.getSignatureType())));
        }

        // Signature format
        if (StringUtils.hasText(dto.getSignatureFormat())) {
            prov.addExtension(new Extension(EXT_SIG_FORMAT, new StringType(dto.getSignatureFormat())));
        }

        // Signature data (base64)
        if (StringUtils.hasText(dto.getSignatureData())) {
            prov.addExtension(new Extension(EXT_SIG_DATA, new StringType(dto.getSignatureData())));
        }

        // Signature hash
        if (StringUtils.hasText(dto.getSignatureHash())) {
            prov.addExtension(new Extension(EXT_SIG_HASH, new StringType(dto.getSignatureHash())));
        }

        // Status
        if (StringUtils.hasText(dto.getStatus())) {
            prov.addExtension(new Extension(EXT_STATUS, new StringType(dto.getStatus())));
        }

        // Comments
        if (StringUtils.hasText(dto.getComments())) {
            prov.addExtension(new Extension(EXT_COMMENTS, new StringType(dto.getComments())));
        }

        return prov;
    }

    private ProviderSignatureDto fromFhirProvenance(Provenance prov) {
        ProviderSignatureDto dto = new ProviderSignatureDto();

        String fhirId = prov.getIdElement().getIdPart();
        dto.setId((long) Math.abs(fhirId.hashCode()));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);

        dto.setPatientId(getPatientId(prov));
        dto.setEncounterId(getEncounterId(prov));

        dto.setSignedAt(getStringExt(prov, EXT_SIGNED_AT));
        dto.setSignedBy(getStringExt(prov, EXT_SIGNED_BY));
        dto.setSignerRole(getStringExt(prov, EXT_SIGNER_ROLE));
        dto.setSignatureType(getStringExt(prov, EXT_SIG_TYPE));
        dto.setSignatureFormat(getStringExt(prov, EXT_SIG_FORMAT));
        dto.setSignatureData(getStringExt(prov, EXT_SIG_DATA));
        dto.setSignatureHash(getStringExt(prov, EXT_SIG_HASH));
        dto.setStatus(getStringExt(prov, EXT_STATUS));
        dto.setComments(getStringExt(prov, EXT_COMMENTS));

        // Audit
        ProviderSignatureDto.Audit audit = new ProviderSignatureDto.Audit();
        audit.setCreatedDate(LocalDate.now().format(DAY));
        audit.setLastModifiedDate(LocalDate.now().format(DAY));
        dto.setAudit(audit);

        return dto;
    }

    private Long getPatientId(Provenance prov) {
        Extension ext = prov.getExtensionByUrl(EXT_PATIENT);
        if (ext != null && ext.getValue() instanceof StringType) {
            try {
                return Long.parseLong(((StringType) ext.getValue()).getValue());
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private Long getEncounterId(Provenance prov) {
        Extension ext = prov.getExtensionByUrl(EXT_ENCOUNTER);
        if (ext != null && ext.getValue() instanceof StringType) {
            try {
                return Long.parseLong(((StringType) ext.getValue()).getValue());
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private String getStringExt(Provenance prov, String url) {
        Extension ext = prov.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) {
            return ((StringType) ext.getValue()).getValue();
        }
        return null;
    }

    private static String sha256(String base64) {
        byte[] bytes = base64.getBytes(StandardCharsets.UTF_8);
        return DigestUtils.md5DigestAsHex(bytes);
    }
}
