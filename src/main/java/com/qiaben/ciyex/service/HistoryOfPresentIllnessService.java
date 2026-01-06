package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import com.qiaben.ciyex.dto.HistoryOfPresentIllnessDto;
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
 * History of Present Illness Service - FHIR Only.
 * All HPI data is stored in HAPI FHIR server as Condition resources.
 */
@Service
@Slf4j
public class HistoryOfPresentIllnessService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    // In-memory cache for e-sign/print metadata (keyed by FHIR ID)
    private final Map<String, SignMetadata> signMetadataCache = new ConcurrentHashMap<>();

    @Autowired
    public HistoryOfPresentIllnessService(FhirClientService fhirClientService, PracticeContextService practiceContextService) {
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
    public List<HistoryOfPresentIllnessDto> getAllByPatient(Long patientId) {
        log.debug("Getting FHIR Conditions (HPI) for patient: {}", patientId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Condition.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .where(new TokenClientParam("category").exactly()
                        .systemAndCode("http://hl7.org/fhir/us/core/CodeSystem/condition-category", "health-concern"))
                
                .returnBundle(Bundle.class)
                .execute();

        return extractHpiDtos(bundle, patientId, null);
    }

    // ✅ Create HPI
    public HistoryOfPresentIllnessDto create(Long patientId, Long encounterId, HistoryOfPresentIllnessDto dto) {
        log.info("Creating HPI in FHIR for patient: {}, encounter: {}", patientId, encounterId);

        Condition condition = toFhirCondition(dto, patientId, encounterId);
        MethodOutcome outcome = fhirClientService.create(condition, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setId(Long.parseLong(fhirId));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);
        
        Condition created = (Condition) outcome.getResource();
        if (created != null && created.hasMeta()) {
            populateAudit(dto, created.getMeta());
        }

        log.info("Created FHIR Condition (HPI) with ID: {}", fhirId);
        return dto;
    }

    // ✅ List HPI for encounter
    public List<HistoryOfPresentIllnessDto> list(Long patientId, Long encounterId) {
        log.debug("Listing FHIR Conditions (HPI) for patient: {}, encounter: {}", patientId, encounterId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Condition.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .where(new TokenClientParam("category").exactly()
                        .systemAndCode("http://hl7.org/fhir/us/core/CodeSystem/condition-category", "health-concern"))
                
                .returnBundle(Bundle.class)
                .execute();

        return extractHpiDtos(bundle, patientId, encounterId);
    }

    // ✅ Get one HPI
    public HistoryOfPresentIllnessDto getOne(Long patientId, Long encounterId, Long id) {
        String fhirId = String.valueOf(id);
        log.debug("Getting FHIR Condition (HPI) with ID: {}", fhirId);

        try {
            Condition condition = fhirClientService.read(Condition.class, fhirId, getPracticeId());
            HistoryOfPresentIllnessDto dto = toHpiDto(condition, patientId, encounterId);
            dto.setId(id);
            return dto;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("History of Present Illness not found with ID: %d for Patient ID: %d and Encounter ID: %d",
                            id, patientId, encounterId));
        }
    }

    // ✅ Update HPI
    public HistoryOfPresentIllnessDto update(Long patientId, Long encounterId, Long id, HistoryOfPresentIllnessDto dto) {
        String fhirId = String.valueOf(id);
        log.info("Updating FHIR Condition (HPI) with ID: {}", fhirId);

        // Check if signed
        SignMetadata meta = signMetadataCache.get(fhirId);
        if (meta != null && Boolean.TRUE.equals(meta.eSigned)) {
            throw new IllegalStateException("Signed HPI entries are read-only.");
        }

        Condition condition = toFhirCondition(dto, patientId, encounterId);
        condition.setId(fhirId);
        fhirClientService.update(condition, getPracticeId());

        return getOne(patientId, encounterId, id);
    }

    // ✅ Delete HPI
    public void delete(Long patientId, Long encounterId, Long id) {
        String fhirId = String.valueOf(id);
        log.info("Deleting FHIR Condition (HPI) with ID: {}", fhirId);

        // Check if signed
        SignMetadata meta = signMetadataCache.get(fhirId);
        if (meta != null && Boolean.TRUE.equals(meta.eSigned)) {
            throw new IllegalStateException("Signed HPI entries cannot be deleted.");
        }

        fhirClientService.delete(Condition.class, fhirId, getPracticeId());
        signMetadataCache.remove(fhirId);
    }

    // ✅ eSign HPI
    public HistoryOfPresentIllnessDto eSign(Long patientId, Long encounterId, Long id, String signedBy) {
        String fhirId = String.valueOf(id);
        log.info("E-signing FHIR Condition (HPI) with ID: {}", fhirId);

        SignMetadata meta = signMetadataCache.computeIfAbsent(fhirId, k -> new SignMetadata());

        if (Boolean.TRUE.equals(meta.eSigned)) {
            return getOne(patientId, encounterId, id);
        }

        meta.eSigned = true;
        meta.signedBy = StringUtils.hasText(signedBy) ? signedBy : "system";
        meta.signedAt = OffsetDateTime.now(ZoneOffset.UTC);

        HistoryOfPresentIllnessDto dto = getOne(patientId, encounterId, id);
        dto.setESigned(meta.eSigned);
        dto.setSignedAt(meta.signedAt);
        dto.setSignedBy(meta.signedBy);

        return dto;
    }

    // ✅ Render PDF
    public byte[] renderPdf(Long patientId, Long encounterId, Long id) {
        String fhirId = String.valueOf(id);
        log.info("Rendering PDF for FHIR Condition (HPI) with ID: {}", fhirId);

        HistoryOfPresentIllnessDto dto = getOne(patientId, encounterId, id);

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
                cs.showText("History of Present Illness");
                cs.endText();

                y -= 26;
                draw(cs, x, y, "Patient ID:", String.valueOf(patientId)); y -= 16;
                draw(cs, x, y, "Encounter ID:", String.valueOf(encounterId)); y -= 16;
                draw(cs, x, y, "HPI ID:", fhirId); y -= 22;

                y = drawMultiline(cs, x, y, "Description:", dto.getDescription(), 80);

                y -= 22;
                draw(cs, x, y, "eSigned:", Boolean.TRUE.equals(meta.eSigned) ? "Yes" : "No"); y -= 16;
                if (meta.signedAt != null) { draw(cs, x, y, "Signed At:", meta.signedAt.toString()); y -= 16; }
                if (StringUtils.hasText(meta.signedBy)) { draw(cs, x, y, "Signed By:", meta.signedBy); y -= 16; }
                if (meta.printedAt != null) { draw(cs, x, y, "Printed At:", meta.printedAt.toString()); y -= 16; }
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate HPI PDF", ex);
        }
    }

    // ========== FHIR Mapping Methods ==========

    private Condition toFhirCondition(HistoryOfPresentIllnessDto dto, Long patientId, Long encounterId) {
        Condition condition = new Condition();

        // Patient reference
        condition.setSubject(new Reference("Patient/" + patientId));

        // Encounter reference
        if (encounterId != null) {
            condition.setEncounter(new Reference("Encounter/" + encounterId));
        }

        // Category: health-concern (for HPI)
        condition.addCategory()
                .addCoding()
                .setSystem("http://hl7.org/fhir/us/core/CodeSystem/condition-category")
                .setCode("health-concern")
                .setDisplay("Health Concern");

        // Clinical status
        condition.setClinicalStatus(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                        .setCode("active")));

        // Verification status
        condition.setVerificationStatus(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/condition-ver-status")
                        .setCode("confirmed")));

        // Code (HPI as text)
        condition.setCode(new CodeableConcept().setText("History of Present Illness"));

        // Note (description)
        if (dto.getDescription() != null) {
            condition.addNote().setText(dto.getDescription());
        }

        return condition;
    }

    private HistoryOfPresentIllnessDto toHpiDto(Condition condition, Long patientId, Long encounterId) {
        HistoryOfPresentIllnessDto dto = new HistoryOfPresentIllnessDto();

        if (condition.hasId()) {
            String fhirId = condition.getIdElement().getIdPart();
            dto.setId(Long.parseLong(fhirId));
            dto.setFhirId(fhirId);
            dto.setExternalId(fhirId);
        }

        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);

        // Description from note
        if (condition.hasNote()) {
            dto.setDescription(condition.getNoteFirstRep().getText());
        }
        
        if (condition.hasMeta()) {
            populateAudit(dto, condition.getMeta());
        }

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

    private List<HistoryOfPresentIllnessDto> extractHpiDtos(Bundle bundle, Long patientId, Long encounterId) {
        List<HistoryOfPresentIllnessDto> items = new ArrayList<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Condition) {
                    items.add(toHpiDto((Condition) entry.getResource(), patientId, encounterId));
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

    private static float drawMultiline(PDPageContentStream cs, float x, float y, String label, String text, int wrap) throws IOException {
        draw(cs, x, y, label, ""); y -= 16;
        if (text == null || text.isBlank()) {
            draw(cs, x, y, "", "-");
            return y - 14;
        }
        String[] lines = text.split("\\r?\\n");
        for (String ln : lines) {
            int i = 0;
            while (i < ln.length()) {
                String chunk = ln.substring(i, Math.min(i + wrap, ln.length()));
                cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x, y); cs.showText(chunk); cs.endText();
                y -= 14; i += wrap;
            }
        }
        return y;
    }
    
    private void populateAudit(HistoryOfPresentIllnessDto dto, Meta meta) {
        HistoryOfPresentIllnessDto.Audit audit = new HistoryOfPresentIllnessDto.Audit();
        if (meta.hasLastUpdated()) {
            audit.setLastModifiedDate(meta.getLastUpdated().toInstant().atOffset(ZoneOffset.UTC).toLocalDate().toString());
            audit.setCreatedDate(meta.getLastUpdated().toInstant().atOffset(ZoneOffset.UTC).toLocalDate().toString());
        }
        dto.setAudit(audit);
    }
}
