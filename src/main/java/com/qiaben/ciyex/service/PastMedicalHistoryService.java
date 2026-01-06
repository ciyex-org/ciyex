package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import com.qiaben.ciyex.dto.PastMedicalHistoryDto;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PastMedicalHistory Service - FHIR Only.
 * All past medical history data is stored in HAPI FHIR server as Condition resources.
 */
@Service
@Slf4j
public class PastMedicalHistoryService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    // In-memory cache for e-sign/print metadata (keyed by FHIR ID)
    private final Map<String, SignMetadata> signMetadataCache = new ConcurrentHashMap<>();

    @Autowired
    public PastMedicalHistoryService(FhirClientService fhirClientService, PracticeContextService practiceContextService) {
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
    public List<PastMedicalHistoryDto> getAllByPatient(Long patientId) {
        log.debug("Getting FHIR Conditions (PMH) for patient: {}", patientId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Condition.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .where(new TokenClientParam("category").exactly()
                        .systemAndCode("http://terminology.hl7.org/CodeSystem/condition-category", "problem-list-item"))
                
                .returnBundle(Bundle.class)
                .execute();

        return extractPmhDtos(bundle, patientId, null);
    }

    // ✅ Create PMH
    public PastMedicalHistoryDto create(Long patientId, Long encounterId, PastMedicalHistoryDto dto) {
        log.info("Creating PMH in FHIR for patient: {}, encounter: {}", patientId, encounterId);

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

        log.info("Created FHIR Condition (PMH) with ID: {}", fhirId);
        return dto;
    }

    // ✅ Get one PMH
    public PastMedicalHistoryDto getOne(Long patientId, Long encounterId, Long id) {
        String fhirId = String.valueOf(id);
        log.debug("Getting FHIR Condition (PMH) with ID: {}", fhirId);

        try {
            Condition condition = fhirClientService.read(Condition.class, fhirId, getPracticeId());
            PastMedicalHistoryDto dto = toPmhDto(condition, patientId, encounterId);
            dto.setId(id);
            return dto;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Past Medical History not found with ID: %d for Patient ID: %d and Encounter ID: %d",
                            id, patientId, encounterId));
        }
    }

    // ✅ List PMH for encounter
    public List<PastMedicalHistoryDto> list(Long patientId, Long encounterId) {
        log.debug("Listing FHIR Conditions (PMH) for patient: {}, encounter: {}", patientId, encounterId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Condition.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .where(new TokenClientParam("category").exactly()
                        .systemAndCode("http://terminology.hl7.org/CodeSystem/condition-category", "problem-list-item"))
                
                .returnBundle(Bundle.class)
                .execute();

        return extractPmhDtos(bundle, patientId, encounterId);
    }

    // ✅ Update PMH
    public PastMedicalHistoryDto update(Long patientId, Long encounterId, Long id, PastMedicalHistoryDto dto) {
        String fhirId = String.valueOf(id);
        log.info("Updating FHIR Condition (PMH) with ID: {}", fhirId);

        // Check if signed
        SignMetadata meta = signMetadataCache.get(fhirId);
        if (meta != null && Boolean.TRUE.equals(meta.eSigned)) {
            throw new IllegalStateException("Signed PMH entries are read-only.");
        }

        Condition condition = toFhirCondition(dto, patientId, encounterId);
        condition.setId(fhirId);
        fhirClientService.update(condition, getPracticeId());

        return getOne(patientId, encounterId, id);
    }

    // ✅ Delete PMH
    public void delete(Long patientId, Long encounterId, Long id) {
        String fhirId = String.valueOf(id);
        log.info("Deleting FHIR Condition (PMH) with ID: {}", fhirId);

        // Check if signed
        SignMetadata meta = signMetadataCache.get(fhirId);
        if (meta != null && Boolean.TRUE.equals(meta.eSigned)) {
            throw new IllegalStateException("Signed PMH entries cannot be deleted.");
        }

        fhirClientService.delete(Condition.class, fhirId, getPracticeId());
        signMetadataCache.remove(fhirId);
    }

    // ✅ eSign PMH
    public PastMedicalHistoryDto eSign(Long patientId, Long encounterId, Long id, String signedBy) {
        String fhirId = String.valueOf(id);
        log.info("E-signing FHIR Condition (PMH) with ID: {}", fhirId);

        SignMetadata meta = signMetadataCache.computeIfAbsent(fhirId, k -> new SignMetadata());

        if (Boolean.TRUE.equals(meta.eSigned)) {
            return getOne(patientId, encounterId, id);
        }

        meta.eSigned = true;
        meta.signedBy = (signedBy == null || signedBy.isBlank()) ? "system" : signedBy;
        meta.signedAt = OffsetDateTime.now(ZoneOffset.UTC);

        PastMedicalHistoryDto dto = getOne(patientId, encounterId, id);
        dto.setESigned(meta.eSigned);
        dto.setSignedAt(meta.signedAt);
        dto.setSignedBy(meta.signedBy);

        return dto;
    }

    // ✅ Render PDF
    public byte[] renderPdf(Long patientId, Long encounterId, Long id) {
        String fhirId = String.valueOf(id);
        log.info("Rendering PDF for FHIR Condition (PMH) with ID: {}", fhirId);

        PastMedicalHistoryDto dto = getOne(patientId, encounterId, id);

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
                cs.showText("Past Medical History");
                cs.endText();

                y -= 26;
                draw(cs, x, y, "Patient ID:", String.valueOf(patientId)); y -= 16;
                draw(cs, x, y, "Encounter ID:", String.valueOf(encounterId)); y -= 16;
                draw(cs, x, y, "PMH ID:", fhirId); y -= 22;

                y = drawMultiline(cs, x, y, "Description:", dto.getDescription(), 80);

                y -= 22;
                draw(cs, x, y, "eSigned:", Boolean.TRUE.equals(meta.eSigned) ? "Yes" : "No"); y -= 16;
                if (meta.signedAt != null) { draw(cs, x, y, "Signed At:", meta.signedAt.toString()); y -= 16; }
                if (meta.signedBy != null) { draw(cs, x, y, "Signed By:", meta.signedBy); y -= 16; }
                if (meta.printedAt != null) { draw(cs, x, y, "Printed At:", meta.printedAt.toString()); y -= 16; }
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate PMH PDF", ex);
        }
    }

    // ========== FHIR Mapping Methods ==========

    private Condition toFhirCondition(PastMedicalHistoryDto dto, Long patientId, Long encounterId) {
        Condition condition = new Condition();

        // Patient reference
        condition.setSubject(new Reference("Patient/" + patientId));

        // Encounter reference
        if (encounterId != null) {
            condition.setEncounter(new Reference("Encounter/" + encounterId));
        }

        // Category: problem-list-item (for PMH)
        condition.addCategory()
                .addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-category")
                .setCode("problem-list-item")
                .setDisplay("Problem List Item");

        // Clinical status
        condition.setClinicalStatus(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                        .setCode("resolved")));

        // Verification status
        condition.setVerificationStatus(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/condition-ver-status")
                        .setCode("confirmed")));

        // Code (description as text)
        if (dto.getDescription() != null) {
            condition.setCode(new CodeableConcept().setText(dto.getDescription()));
        }

        // Note
        if (dto.getDescription() != null) {
            condition.addNote().setText(dto.getDescription());
        }

        return condition;
    }

    private PastMedicalHistoryDto toPmhDto(Condition condition, Long patientId, Long encounterId) {
        PastMedicalHistoryDto dto = new PastMedicalHistoryDto();

        if (condition.hasId()) {
            String fhirId = condition.getIdElement().getIdPart();
            dto.setId(Long.parseLong(fhirId));
            dto.setFhirId(fhirId);
            dto.setExternalId(fhirId);
        }

        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);

        // Description from code text or note
        if (condition.hasCode() && condition.getCode().hasText()) {
            dto.setDescription(condition.getCode().getText());
        } else if (condition.hasNote()) {
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

    private List<PastMedicalHistoryDto> extractPmhDtos(Bundle bundle, Long patientId, Long encounterId) {
        List<PastMedicalHistoryDto> items = new ArrayList<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Condition) {
                    items.add(toPmhDto((Condition) entry.getResource(), patientId, encounterId));
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
    
    private void populateAudit(PastMedicalHistoryDto dto, Meta meta) {
        PastMedicalHistoryDto.Audit audit = new PastMedicalHistoryDto.Audit();
        if (meta.hasLastUpdated()) {
            audit.setLastModifiedDate(meta.getLastUpdated().toInstant().atOffset(ZoneOffset.UTC).toLocalDate().toString());
            audit.setCreatedDate(meta.getLastUpdated().toInstant().atOffset(ZoneOffset.UTC).toLocalDate().toString());
        }
        dto.setAudit(audit);
    }
}
