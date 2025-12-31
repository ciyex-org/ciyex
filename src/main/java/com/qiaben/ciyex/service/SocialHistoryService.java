package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import com.qiaben.ciyex.dto.SocialHistoryDto;
import com.qiaben.ciyex.dto.SocialHistoryEntryDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SocialHistory Service - FHIR Only.
 * All social history data is stored in HAPI FHIR server as Observation resources.
 */
@Service
@Slf4j
public class SocialHistoryService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    // In-memory cache for e-sign/print metadata (keyed by FHIR ID)
    private final Map<String, SignMetadata> signMetadataCache = new ConcurrentHashMap<>();

    @Autowired
    public SocialHistoryService(FhirClientService fhirClientService, PracticeContextService practiceContextService) {
        this.fhirClientService = fhirClientService;
        this.practiceContextService = practiceContextService;
    }

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // Helper class for e-sign metadata
    private static class SignMetadata {
        Boolean eSigned = false;
        OffsetDateTime signedAt;
        String signedBy;
        OffsetDateTime printedAt;
    }

    // ✅ Get all by patient
    public List<SocialHistoryDto> getAllByPatient(Long patientId) {
        log.debug("Getting FHIR Observations (social history) for patient: {}", patientId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Observation.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .where(new TokenClientParam("category").exactly()
                        .systemAndCode("http://terminology.hl7.org/CodeSystem/observation-category", "social-history"))
                
                .returnBundle(Bundle.class)
                .execute();

        return extractSocialHistoryDtos(bundle, patientId, null);
    }

    // ✅ Create social history
    public SocialHistoryDto create(Long patientId, Long encounterId, SocialHistoryDto dto) {
        log.info("Creating social history in FHIR for patient: {}, encounter: {}", patientId, encounterId);

        List<String> createdIds = new ArrayList<>();

        // Create one Observation per entry
        if (dto.getEntries() != null) {
            for (SocialHistoryEntryDto entry : dto.getEntries()) {
                Observation obs = toFhirObservation(entry, patientId, encounterId);
                MethodOutcome outcome = fhirClientService.create(obs, getPracticeId());
                String fhirId = outcome.getId().getIdPart();
                createdIds.add(fhirId);
                log.info("Created FHIR Observation (social history) with ID: {}", fhirId);
            }
        }

        // Return DTO with first ID as container ID
        String containerId = createdIds.isEmpty() ? "SH-" + System.currentTimeMillis() : createdIds.get(0);
        dto.setFhirId(containerId);
        dto.setExternalId(containerId);
        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);

        return dto;
    }

    // ✅ Get one social history
    public SocialHistoryDto getOne(Long patientId, Long encounterId) {
        log.debug("Getting FHIR Observations (social history) for patient: {}, encounter: {}", patientId, encounterId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Observation.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .where(new TokenClientParam("category").exactly()
                        .systemAndCode("http://terminology.hl7.org/CodeSystem/observation-category", "social-history"))
                
                .returnBundle(Bundle.class)
                .execute();

        List<SocialHistoryDto> results = extractSocialHistoryDtos(bundle, patientId, encounterId);
        if (results.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("Social History not found for Patient ID: %d, Encounter ID: %d", patientId, encounterId));
        }
        return results.get(0);
    }

    // ✅ Get by id
    public SocialHistoryDto getById(Long patientId, Long encounterId, Long id) {
        String fhirId = String.valueOf(id);
        log.debug("Getting FHIR Observation (social history) with ID: {}", fhirId);

        try {
            Observation obs = fhirClientService.read(Observation.class, fhirId, getPracticeId());
            return toSocialHistoryDto(obs, patientId, encounterId);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Social History not found for Patient ID: %d, Encounter ID: %d, ID: %d",
                            patientId, encounterId, id));
        }
    }

    // ✅ Update social history
    public SocialHistoryDto update(Long patientId, Long encounterId, Long id, SocialHistoryDto dto) {
        String fhirId = String.valueOf(id);
        log.info("Updating FHIR Observation (social history) with ID: {}", fhirId);

        // Check if signed
        SignMetadata meta = signMetadataCache.get(fhirId);
        if (meta != null && Boolean.TRUE.equals(meta.eSigned)) {
            throw new IllegalStateException("Signed Social History is read-only.");
        }

        // Update first entry
        if (dto.getEntries() != null && !dto.getEntries().isEmpty()) {
            SocialHistoryEntryDto entry = dto.getEntries().get(0);
            Observation obs = toFhirObservation(entry, patientId, encounterId);
            obs.setId(fhirId);
            fhirClientService.update(obs, getPracticeId());
        }

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        return dto;
    }

    // ✅ Delete social history
    public void delete(Long patientId, Long encounterId, Long id) {
        String fhirId = String.valueOf(id);
        log.info("Deleting FHIR Observation (social history) with ID: {}", fhirId);

        // Check if signed
        SignMetadata meta = signMetadataCache.get(fhirId);
        if (meta != null && Boolean.TRUE.equals(meta.eSigned)) {
            throw new IllegalStateException("Signed Social History cannot be deleted.");
        }

        fhirClientService.delete(Observation.class, fhirId, getPracticeId());
        signMetadataCache.remove(fhirId);
    }

    // ✅ eSign social history
    public SocialHistoryDto eSign(Long patientId, Long encounterId, Long id, String signedBy) {
        String fhirId = String.valueOf(id);
        log.info("E-signing FHIR Observation (social history) with ID: {}", fhirId);

        SignMetadata meta = signMetadataCache.computeIfAbsent(fhirId, k -> new SignMetadata());

        if (Boolean.TRUE.equals(meta.eSigned)) {
            return getById(patientId, encounterId, id);
        }

        meta.eSigned = true;
        meta.signedBy = StringUtils.hasText(signedBy) ? signedBy : "system";
        meta.signedAt = OffsetDateTime.now(ZoneOffset.UTC);

        SocialHistoryDto dto = getById(patientId, encounterId, id);
        dto.setESigned(meta.eSigned);
        dto.setSignedAt(meta.signedAt);
        dto.setSignedBy(meta.signedBy);

        return dto;
    }

    // ✅ Render PDF
    public byte[] renderPdf(Long patientId, Long encounterId, Long id) {
        String fhirId = String.valueOf(id);
        log.info("Rendering PDF for FHIR Observation (social history) with ID: {}", fhirId);

        SocialHistoryDto dto = getById(patientId, encounterId, id);

        // Update print timestamp
        SignMetadata meta = signMetadataCache.computeIfAbsent(fhirId, k -> new SignMetadata());
        meta.printedAt = OffsetDateTime.now(ZoneOffset.UTC);

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float x = 64, y = 740;

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.newLineAtOffset(x, y);
                cs.showText("Social History");
                cs.endText();

                y -= 26;
                draw(cs, x, y, "Patient ID:", String.valueOf(patientId)); y -= 16;
                draw(cs, x, y, "Encounter ID:", String.valueOf(encounterId)); y -= 16;
                draw(cs, x, y, "Record ID:", fhirId); y -= 22;

                List<SocialHistoryEntryDto> entries = dto.getEntries() == null ? List.of() : dto.getEntries();
                if (entries.isEmpty()) {
                    draw(cs, x, y, "Entries:", "(none)"); y -= 16;
                } else {
                    for (int i = 0; i < entries.size(); i++) {
                        SocialHistoryEntryDto entry = entries.get(i);
                        cs.beginText();
                        cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                        cs.newLineAtOffset(x, y);
                        cs.showText((i + 1) + ". " + (entry.getCategory() != null ? entry.getCategory() : "—"));
                        cs.endText();
                        y -= 14;

                        if (StringUtils.hasText(entry.getValue())) { draw(cs, x + 16, y, "Value:", entry.getValue()); y -= 14; }
                        if (StringUtils.hasText(entry.getDetails())) {
                            draw(cs, x + 16, y, "Details:", entry.getDetails()); y -= 14;
                        }
                        y -= 8;
                    }
                }

                y -= 8;
                draw(cs, x, y, "eSigned:", Boolean.TRUE.equals(meta.eSigned) ? "Yes" : "No"); y -= 16;
                if (meta.signedAt != null) { draw(cs, x, y, "Signed At:", meta.signedAt.toString()); y -= 16; }
                if (StringUtils.hasText(meta.signedBy)) { draw(cs, x, y, "Signed By:", meta.signedBy); y -= 16; }
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate Social History PDF", ex);
        }
    }

    // ========== FHIR Mapping Methods ==========

    private Observation toFhirObservation(SocialHistoryEntryDto entry, Long patientId, Long encounterId) {
        Observation obs = new Observation();

        // Patient reference
        obs.setSubject(new Reference("Patient/" + patientId));

        // Encounter reference
        if (encounterId != null) {
            obs.setEncounter(new Reference("Encounter/" + encounterId));
        }

        // Category: social-history
        obs.addCategory()
                .addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
                .setCode("social-history")
                .setDisplay("Social History");

        // Status
        obs.setStatus(Observation.ObservationStatus.FINAL);

        // Code (category as code)
        if (entry.getCategory() != null) {
            obs.setCode(new CodeableConcept().setText(entry.getCategory()));
        }

        // Value
        if (entry.getValue() != null) {
            obs.setValue(new StringType(entry.getValue()));
        }

        // Note (details)
        if (entry.getDetails() != null) {
            obs.addNote().setText(entry.getDetails());
        }

        return obs;
    }

    private SocialHistoryDto toSocialHistoryDto(Observation obs, Long patientId, Long encounterId) {
        SocialHistoryDto dto = new SocialHistoryDto();

        if (obs.hasId()) {
            dto.setFhirId(obs.getIdElement().getIdPart());
            dto.setExternalId(obs.getIdElement().getIdPart());
        }

        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);

        // Extract entry
        List<SocialHistoryEntryDto> entries = new ArrayList<>();
        SocialHistoryEntryDto entry = new SocialHistoryEntryDto();

        if (obs.hasCode() && obs.getCode().hasText()) {
            entry.setCategory(obs.getCode().getText());
        }

        if (obs.hasValueStringType()) {
            entry.setValue(obs.getValueStringType().getValue());
        }

        if (obs.hasNote()) {
            entry.setDetails(obs.getNoteFirstRep().getText());
        }

        entries.add(entry);
        dto.setEntries(entries);

        // Check sign metadata
        String fhirId = dto.getFhirId();
        if (fhirId != null) {
            SignMetadata meta = signMetadataCache.get(fhirId);
            if (meta != null) {
                dto.setESigned(meta.eSigned);
                dto.setSignedAt(meta.signedAt);
                dto.setSignedBy(meta.signedBy);
                dto.setPrintedAt(meta.printedAt);
            }
        }

        return dto;
    }

    private List<SocialHistoryDto> extractSocialHistoryDtos(Bundle bundle, Long patientId, Long encounterId) {
        List<SocialHistoryDto> items = new ArrayList<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Observation) {
                    items.add(toSocialHistoryDto((Observation) entry.getResource(), patientId, encounterId));
                }
            }
        }
        return items;
    }

    // ========== PDF Helpers ==========

    private static void draw(PDPageContentStream cs, float x, float y, String label, String value) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 12); cs.newLineAtOffset(x, y); cs.showText(label); cs.endText();
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x + 140, y); cs.showText(value != null ? value : "-"); cs.endText();
    }
}
