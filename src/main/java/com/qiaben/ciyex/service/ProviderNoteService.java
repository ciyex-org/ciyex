package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import com.qiaben.ciyex.dto.ProviderNoteDto;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provider Note Service - FHIR Only.
 * All provider note data is stored in HAPI FHIR server as DocumentReference resources.
 */
@Service
@Slf4j
public class ProviderNoteService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    // In-memory cache for e-sign/print metadata (keyed by FHIR ID)
    private final Map<String, SignMetadata> signMetadataCache = new ConcurrentHashMap<>();

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Autowired
    public ProviderNoteService(FhirClientService fhirClientService, PracticeContextService practiceContextService) {
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
    public List<ProviderNoteDto> getAllByPatient(Long patientId) {
        log.debug("Getting FHIR DocumentReferences for patient: {}", patientId);

        Bundle bundle = fhirClientService.getClient().search()
                .forResource(DocumentReference.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .withAdditionalHeader("X-Request-Tenant-Id", getPracticeId())
                .returnBundle(Bundle.class)
                .execute();

        return extractNoteDtos(bundle, patientId, null);
    }

    // ✅ Create Provider Note
    public ProviderNoteDto create(Long patientId, Long encounterId, ProviderNoteDto dto) {
        log.info("Creating Provider Note in FHIR for patient: {}, encounter: {}", patientId, encounterId);

        DocumentReference docRef = toFhirDocumentReference(dto, patientId, encounterId);
        MethodOutcome outcome = fhirClientService.create(docRef, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);

        log.info("Created FHIR DocumentReference with ID: {}", fhirId);
        return dto;
    }

    // ✅ List Provider Notes for encounter
    public List<ProviderNoteDto> list(Long patientId, Long encounterId) {
        log.debug("Listing FHIR DocumentReferences for patient: {}, encounter: {}", patientId, encounterId);

        Bundle bundle = fhirClientService.getClient().search()
                .forResource(DocumentReference.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .withAdditionalHeader("X-Request-Tenant-Id", getPracticeId())
                .returnBundle(Bundle.class)
                .execute();

        return extractNoteDtos(bundle, patientId, encounterId);
    }

    // ✅ Get one Provider Note
    public ProviderNoteDto getOne(Long patientId, Long encounterId, Long id) {
        String fhirId = String.valueOf(id);
        log.debug("Getting FHIR DocumentReference with ID: {}", fhirId);

        try {
            DocumentReference docRef = fhirClientService.read(DocumentReference.class, fhirId, getPracticeId());
            return toNoteDto(docRef, patientId, encounterId);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Provider Note not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id));
        }
    }

    // ✅ Update Provider Note
    public ProviderNoteDto update(Long patientId, Long encounterId, Long id, ProviderNoteDto dto) {
        String fhirId = String.valueOf(id);
        log.info("Updating FHIR DocumentReference with ID: {}", fhirId);

        // Check if signed
        SignMetadata meta = signMetadataCache.get(fhirId);
        if (meta != null && Boolean.TRUE.equals(meta.eSigned)) {
            throw new IllegalStateException("Signed provider notes are read-only.");
        }

        DocumentReference docRef = toFhirDocumentReference(dto, patientId, encounterId);
        docRef.setId(fhirId);
        fhirClientService.update(docRef, getPracticeId());

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        return dto;
    }

    // ✅ Delete Provider Note
    public void delete(Long patientId, Long encounterId, Long id) {
        String fhirId = String.valueOf(id);
        log.info("Deleting FHIR DocumentReference with ID: {}", fhirId);

        // Check if signed
        SignMetadata meta = signMetadataCache.get(fhirId);
        if (meta != null && Boolean.TRUE.equals(meta.eSigned)) {
            throw new IllegalStateException("Signed provider notes cannot be deleted.");
        }

        fhirClientService.delete(DocumentReference.class, fhirId, getPracticeId());
        signMetadataCache.remove(fhirId);
    }

    // ✅ eSign Provider Note
    public ProviderNoteDto eSign(Long patientId, Long encounterId, Long id, String signedBy) {
        String fhirId = String.valueOf(id);
        log.info("E-signing FHIR DocumentReference with ID: {}", fhirId);

        SignMetadata meta = signMetadataCache.computeIfAbsent(fhirId, k -> new SignMetadata());

        if (Boolean.TRUE.equals(meta.eSigned)) {
            return getOne(patientId, encounterId, id);
        }

        meta.eSigned = true;
        meta.signedBy = StringUtils.hasText(signedBy) ? signedBy : "system";
        meta.signedAt = OffsetDateTime.now(ZoneOffset.UTC);

        ProviderNoteDto dto = getOne(patientId, encounterId, id);
        dto.setESigned(meta.eSigned);
        dto.setSignedAt(meta.signedAt != null ? meta.signedAt.format(ISO) : null);
        dto.setSignedBy(meta.signedBy);

        return dto;
    }

    // ✅ Render PDF
    public byte[] renderPdf(Long patientId, Long encounterId, Long id) {
        String fhirId = String.valueOf(id);
        log.info("Rendering PDF for FHIR DocumentReference with ID: {}", fhirId);

        ProviderNoteDto dto = getOne(patientId, encounterId, id);

        // Update print timestamp
        SignMetadata meta = signMetadataCache.computeIfAbsent(fhirId, k -> new SignMetadata());
        meta.printedAt = OffsetDateTime.now(ZoneOffset.UTC);

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float x = 64, y = 740;

                // Title
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.newLineAtOffset(x, y);
                cs.showText("Provider Note");
                cs.endText();

                // Meta
                y -= 30;
                draw(cs, x, y, "Patient ID:", String.valueOf(patientId)); y -= 18;
                draw(cs, x, y, "Encounter ID:", String.valueOf(encounterId)); y -= 18;
                draw(cs, x, y, "Note ID:", fhirId); y -= 18;

                if (StringUtils.hasText(dto.getNoteTitle())) { draw(cs, x, y, "Title:", dto.getNoteTitle()); y -= 18; }
                if (StringUtils.hasText(dto.getNoteTypeCode())) { draw(cs, x, y, "Type:", dto.getNoteTypeCode()); y -= 18; }
                if (StringUtils.hasText(dto.getNoteStatus())) { draw(cs, x, y, "Status:", dto.getNoteStatus()); y -= 18; }

                // SOAP sections
                y -= 24;
                if (StringUtils.hasText(dto.getSubjective())) { draw(cs, x, y, "S (Subjective):", dto.getSubjective()); y -= 18; }
                if (StringUtils.hasText(dto.getObjective())) { draw(cs, x, y, "O (Objective):", dto.getObjective()); y -= 18; }
                if (StringUtils.hasText(dto.getAssessment())) { draw(cs, x, y, "A (Assessment):", dto.getAssessment()); y -= 18; }
                if (StringUtils.hasText(dto.getPlan())) { draw(cs, x, y, "P (Plan):", dto.getPlan()); y -= 18; }
                if (StringUtils.hasText(dto.getNarrative())) { draw(cs, x, y, "Narrative:", dto.getNarrative()); y -= 18; }

                // Signature info
                y -= 20;
                draw(cs, x, y, "eSigned:", Boolean.TRUE.equals(meta.eSigned) ? "Yes" : "No"); y -= 18;
                if (meta.signedAt != null) { draw(cs, x, y, "Signed At:", meta.signedAt.format(ISO)); y -= 18; }
                if (StringUtils.hasText(meta.signedBy)) { draw(cs, x, y, "Signed By:", meta.signedBy); y -= 18; }
                if (meta.printedAt != null) { draw(cs, x, y, "Printed At:", meta.printedAt.format(ISO)); y -= 18; }
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate Provider Note PDF", ex);
        }
    }

    // ========== FHIR Mapping Methods ==========

    private DocumentReference toFhirDocumentReference(ProviderNoteDto dto, Long patientId, Long encounterId) {
        DocumentReference docRef = new DocumentReference();

        // Patient reference
        docRef.setSubject(new Reference("Patient/" + patientId));

        // Encounter reference
        if (encounterId != null) {
            DocumentReference.DocumentReferenceContextComponent context = new DocumentReference.DocumentReferenceContextComponent();
            context.addEncounter(new Reference("Encounter/" + encounterId));
            docRef.setContext(context);
        }

        // Status
        docRef.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);

        // Type
        if (StringUtils.hasText(dto.getNoteTypeCode())) {
            docRef.setType(new CodeableConcept().setText(dto.getNoteTypeCode()));
        }

        // Description (title)
        if (StringUtils.hasText(dto.getNoteTitle())) {
            docRef.setDescription(dto.getNoteTitle());
        }

        // Content - store SOAP note as attachment
        StringBuilder content = new StringBuilder();
        if (StringUtils.hasText(dto.getSubjective())) {
            content.append("S: ").append(dto.getSubjective()).append("\n");
        }
        if (StringUtils.hasText(dto.getObjective())) {
            content.append("O: ").append(dto.getObjective()).append("\n");
        }
        if (StringUtils.hasText(dto.getAssessment())) {
            content.append("A: ").append(dto.getAssessment()).append("\n");
        }
        if (StringUtils.hasText(dto.getPlan())) {
            content.append("P: ").append(dto.getPlan()).append("\n");
        }
        if (StringUtils.hasText(dto.getNarrative())) {
            content.append("Narrative: ").append(dto.getNarrative()).append("\n");
        }

        Attachment attachment = new Attachment();
        attachment.setContentType("text/plain");
        attachment.setData(content.toString().getBytes());
        docRef.addContent().setAttachment(attachment);

        return docRef;
    }

    private ProviderNoteDto toNoteDto(DocumentReference docRef, Long patientId, Long encounterId) {
        ProviderNoteDto dto = new ProviderNoteDto();

        if (docRef.hasId()) {
            dto.setFhirId(docRef.getIdElement().getIdPart());
            dto.setExternalId(docRef.getIdElement().getIdPart());
        }

        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);

        // Type
        if (docRef.hasType() && docRef.getType().hasText()) {
            dto.setNoteTypeCode(docRef.getType().getText());
        }

        // Description (title)
        if (docRef.hasDescription()) {
            dto.setNoteTitle(docRef.getDescription());
        }

        // Parse content back to SOAP fields
        if (docRef.hasContent()) {
            Attachment attachment = docRef.getContentFirstRep().getAttachment();
            if (attachment.hasData()) {
                String content = new String(attachment.getData());
                for (String line : content.split("\n")) {
                    if (line.startsWith("S: ")) {
                        dto.setSubjective(line.substring(3));
                    } else if (line.startsWith("O: ")) {
                        dto.setObjective(line.substring(3));
                    } else if (line.startsWith("A: ")) {
                        dto.setAssessment(line.substring(3));
                    } else if (line.startsWith("P: ")) {
                        dto.setPlan(line.substring(3));
                    } else if (line.startsWith("Narrative: ")) {
                        dto.setNarrative(line.substring(11));
                    }
                }
            }
        }

        // Check sign metadata
        String fhirId = dto.getFhirId();
        if (fhirId != null) {
            SignMetadata meta = signMetadataCache.get(fhirId);
            if (meta != null) {
                dto.setESigned(meta.eSigned);
                dto.setSignedAt(meta.signedAt != null ? meta.signedAt.format(ISO) : null);
                dto.setSignedBy(meta.signedBy);
                dto.setPrintedAt(meta.printedAt != null ? meta.printedAt.format(ISO) : null);
            }
        }

        return dto;
    }

    private List<ProviderNoteDto> extractNoteDtos(Bundle bundle, Long patientId, Long encounterId) {
        List<ProviderNoteDto> items = new ArrayList<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof DocumentReference) {
                    items.add(toNoteDto((DocumentReference) entry.getResource(), patientId, encounterId));
                }
            }
        }
        return items;
    }

    // ========== PDF Helpers ==========

    private static void draw(PDPageContentStream cs, float x, float y, String label, String value) throws IOException {
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
        cs.newLineAtOffset(x, y);
        cs.showText(label);
        cs.endText();

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 12);
        cs.newLineAtOffset(x + 140, y);
        cs.showText(value != null ? value : "-");
        cs.endText();
    }
}
