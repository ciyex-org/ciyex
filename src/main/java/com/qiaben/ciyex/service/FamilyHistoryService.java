package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import com.qiaben.ciyex.dto.EntryDto;
import com.qiaben.ciyex.dto.FamilyHistoryDto;
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
 * FamilyHistory Service - FHIR Only.
 * All family history data is stored in HAPI FHIR server as FamilyMemberHistory resources.
 */
@Service
@Slf4j
public class FamilyHistoryService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    // In-memory cache for e-sign/print metadata (keyed by FHIR ID)
    private final Map<String, SignMetadata> signMetadataCache = new ConcurrentHashMap<>();

    @Autowired
    public FamilyHistoryService(FhirClientService fhirClientService, PracticeContextService practiceContextService) {
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
        Long signedEntryId;
    }

    // ✅ Get all by patient
    public List<FamilyHistoryDto> getAllByPatient(Long patientId) {
        log.debug("Getting FHIR FamilyMemberHistory for patient: {}", patientId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(FamilyMemberHistory.class)
                .where(new ReferenceClientParam("patient").hasId("Patient/" + patientId))
                
                .returnBundle(Bundle.class)
                .execute();

        return extractFamilyHistoryDtos(bundle, patientId, null);
    }

    // ✅ Create family history
    public FamilyHistoryDto create(Long patientId, Long encounterId, FamilyHistoryDto dto) {
        log.info("Creating family history in FHIR for patient: {}, encounter: {}", patientId, encounterId);

        List<String> createdIds = new ArrayList<>();

        // Create one FamilyMemberHistory per entry
        if (dto.getEntries() != null) {
            for (EntryDto entry : dto.getEntries()) {
                FamilyMemberHistory fmh = toFhirFamilyMemberHistory(entry, patientId, encounterId);
                MethodOutcome outcome = fhirClientService.create(fmh, getPracticeId());
                String fhirId = outcome.getId().getIdPart();
                createdIds.add(fhirId);
                log.info("Created FHIR FamilyMemberHistory with ID: {}", fhirId);
            }
        }

        // Return DTO with first ID as container ID
        String containerId = createdIds.isEmpty() ? "FH-" + System.currentTimeMillis() : createdIds.get(0);
        dto.setId(Long.parseLong(containerId));
        dto.setFhirId(containerId);
        dto.setExternalId(containerId);
        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);
        
        FamilyHistoryDto.Audit audit = new FamilyHistoryDto.Audit();
        audit.setCreatedDate(java.time.LocalDate.now().toString());
        audit.setLastModifiedDate(java.time.LocalDate.now().toString());
        dto.setAudit(audit);

        return dto;
    }

    // ✅ Get one family history
    public FamilyHistoryDto getOne(Long patientId, Long encounterId, Long id) {
        String fhirId = String.valueOf(id);
        log.debug("Getting FHIR FamilyMemberHistory with ID: {}", fhirId);

        try {
            FamilyMemberHistory fmh = fhirClientService.read(FamilyMemberHistory.class, fhirId, getPracticeId());
            FamilyHistoryDto dto = toFamilyHistoryDto(fmh, patientId, encounterId);
            dto.setId(id);
            return dto;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Family History not found with ID: %d for Patient ID: %d and Encounter ID: %d",
                            id, patientId, encounterId));
        }
    }

    // ✅ List family histories for encounter
    public List<FamilyHistoryDto> list(Long patientId, Long encounterId) {
        log.debug("Listing FHIR FamilyMemberHistory for patient: {}, encounter: {}", patientId, encounterId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(FamilyMemberHistory.class)
                .where(new ReferenceClientParam("patient").hasId("Patient/" + patientId))
                
                .returnBundle(Bundle.class)
                .execute();

        return extractFamilyHistoryDtos(bundle, patientId, encounterId);
    }

    // ✅ Update family history
    public FamilyHistoryDto update(Long patientId, Long encounterId, Long id, FamilyHistoryDto dto) {
        String fhirId = String.valueOf(id);
        log.info("Updating FHIR FamilyMemberHistory with ID: {}", fhirId);

        // Check if signed
        SignMetadata meta = signMetadataCache.get(fhirId);
        if (meta != null && Boolean.TRUE.equals(meta.eSigned)) {
            throw new IllegalStateException("Signed family history is read-only.");
        }

        // Update first entry (or create if entries provided)
        if (dto.getEntries() != null && !dto.getEntries().isEmpty()) {
            EntryDto entry = dto.getEntries().get(0);
            FamilyMemberHistory fmh = toFhirFamilyMemberHistory(entry, patientId, encounterId);
            fmh.setId(fhirId);
            fhirClientService.update(fmh, getPracticeId());
        }

        return getOne(patientId, encounterId, id);
    }

    // ✅ Delete family history
    public void delete(Long patientId, Long encounterId, Long id) {
        String fhirId = String.valueOf(id);
        log.info("Deleting FHIR FamilyMemberHistory with ID: {}", fhirId);

        // Check if signed
        SignMetadata meta = signMetadataCache.get(fhirId);
        if (meta != null && Boolean.TRUE.equals(meta.eSigned)) {
            throw new IllegalStateException("Signed family history cannot be deleted.");
        }

        fhirClientService.delete(FamilyMemberHistory.class, fhirId, getPracticeId());
        signMetadataCache.remove(fhirId);
    }

    // ✅ eSign family history
    public FamilyHistoryDto eSign(Long patientId, Long encounterId, Long id, String signedBy, Long entryId) {
        String fhirId = String.valueOf(id);
        log.info("E-signing FHIR FamilyMemberHistory with ID: {}", fhirId);

        SignMetadata meta = signMetadataCache.computeIfAbsent(fhirId, k -> new SignMetadata());

        if (Boolean.TRUE.equals(meta.eSigned)) {
            // Idempotent
            return getOne(patientId, encounterId, id);
        }

        meta.eSigned = true;
        meta.signedBy = StringUtils.hasText(signedBy) ? signedBy : "system";
        meta.signedAt = OffsetDateTime.now(ZoneOffset.UTC);
        meta.signedEntryId = entryId;

        FamilyHistoryDto dto = getOne(patientId, encounterId, id);
        dto.setESigned(meta.eSigned);
        dto.setSignedAt(meta.signedAt);
        dto.setSignedBy(meta.signedBy);
        dto.setSignedEntryId(meta.signedEntryId);

        return dto;
    }

    // ✅ Render PDF
    public byte[] renderPdf(Long patientId, Long encounterId, Long id) {
        String fhirId = String.valueOf(id);
        log.info("Rendering PDF for FHIR FamilyMemberHistory with ID: {}", fhirId);

        FamilyHistoryDto dto = getOne(patientId, encounterId, id);

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
                cs.showText("Family History");
                cs.endText();

                // Meta
                y -= 26;
                draw(cs, x, y, "Patient ID:", String.valueOf(patientId)); y -= 16;
                draw(cs, x, y, "Encounter ID:", String.valueOf(encounterId)); y -= 16;
                draw(cs, x, y, "Record ID:", fhirId); y -= 22;

                // Entries
                List<EntryDto> entries = dto.getEntries() == null ? List.of() : dto.getEntries();
                if (entries.isEmpty()) {
                    draw(cs, x, y, "Entries:", "—"); y -= 16;
                } else {
                    for (EntryDto e : entries) {
                        draw(cs, x, y, "Relation:", nullTo(e.getRelation(), "-")); y -= 16;
                        String diag = nullTo(e.getDiagnosisText(), "—")
                                + (StringUtils.hasText(e.getDiagnosisCode()) ? " (" + e.getDiagnosisCode() + ")" : "");
                        draw(cs, x, y, "Diagnosis:", diag); y -= 16;
                        if (StringUtils.hasText(e.getNotes())) { draw(cs, x, y, "Notes:", e.getNotes()); y -= 16; }
                        y -= 8;
                    }
                }

                // eSign footer
                y -= 8;
                draw(cs, x, y, "eSigned:", Boolean.TRUE.equals(meta.eSigned) ? "Yes" : "No"); y -= 16;
                if (meta.signedAt != null) { draw(cs, x, y, "Signed At:", meta.signedAt.toString()); y -= 16; }
                if (StringUtils.hasText(meta.signedBy)) { draw(cs, x, y, "Signed By:", meta.signedBy); y -= 16; }
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Family History PDF", e);
        }
    }

    // ========== FHIR Mapping Methods ==========

    private FamilyMemberHistory toFhirFamilyMemberHistory(EntryDto entry, Long patientId, Long encounterId) {
        FamilyMemberHistory fmh = new FamilyMemberHistory();

        // Patient reference
        fmh.setPatient(new Reference("Patient/" + patientId));

        // Status
        fmh.setStatus(FamilyMemberHistory.FamilyHistoryStatus.COMPLETED);

        // Relationship
        if (entry.getRelation() != null) {
            fmh.setRelationship(new CodeableConcept()
                    .addCoding(new Coding()
                            .setSystem("http://terminology.hl7.org/CodeSystem/v3-RoleCode")
                            .setCode(mapRelationToFhirCode(entry.getRelation()))
                            .setDisplay(entry.getRelation())));
        }

        // Condition (diagnosis)
        if (entry.getDiagnosisCode() != null || entry.getDiagnosisText() != null) {
            FamilyMemberHistory.FamilyMemberHistoryConditionComponent condition = fmh.addCondition();
            condition.setCode(new CodeableConcept()
                    .addCoding(new Coding().setCode(entry.getDiagnosisCode()))
                    .setText(entry.getDiagnosisText()));
        }

        // Notes
        if (entry.getNotes() != null) {
            fmh.addNote().setText(entry.getNotes());
        }

        return fmh;
    }

    private FamilyHistoryDto toFamilyHistoryDto(FamilyMemberHistory fmh, Long patientId, Long encounterId) {
        FamilyHistoryDto dto = new FamilyHistoryDto();

        if (fmh.hasId()) {
            String fhirId = fmh.getIdElement().getIdPart();
            dto.setId(Long.parseLong(fhirId));
            dto.setFhirId(fhirId);
            dto.setExternalId(fhirId);
        }

        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);

        // Extract entry
        List<EntryDto> entries = new ArrayList<>();
        EntryDto entry = new EntryDto();

        if (fmh.hasRelationship()) {
            entry.setRelation(fmh.getRelationship().getText() != null ?
                    fmh.getRelationship().getText() :
                    (fmh.getRelationship().hasCoding() ? fmh.getRelationship().getCodingFirstRep().getDisplay() : null));
        }

        if (fmh.hasCondition()) {
            FamilyMemberHistory.FamilyMemberHistoryConditionComponent cond = fmh.getConditionFirstRep();
            if (cond.hasCode()) {
                entry.setDiagnosisCode(cond.getCode().hasCoding() ? cond.getCode().getCodingFirstRep().getCode() : null);
                entry.setDiagnosisText(cond.getCode().getText());
            }
        }

        if (fmh.hasNote()) {
            entry.setNotes(fmh.getNoteFirstRep().getText());
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
                dto.setSignedEntryId(meta.signedEntryId);
            }
        }
        
        if (fmh.hasMeta()) {
            populateAudit(dto, fmh.getMeta());
        }

        return dto;
    }

    private List<FamilyHistoryDto> extractFamilyHistoryDtos(Bundle bundle, Long patientId, Long encounterId) {
        List<FamilyHistoryDto> items = new ArrayList<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof FamilyMemberHistory) {
                    items.add(toFamilyHistoryDto((FamilyMemberHistory) entry.getResource(), patientId, encounterId));
                }
            }
        }
        return items;
    }

    private String mapRelationToFhirCode(String relation) {
        if (relation == null) return "FAMMEMB";
        return switch (relation.toUpperCase()) {
            case "FATHER" -> "FTH";
            case "MOTHER" -> "MTH";
            case "SIBLING" -> "SIB";
            case "SPOUSE" -> "SPS";
            case "OFFSPRING", "CHILD" -> "CHILD";
            default -> "FAMMEMB";
        };
    }

    // ========== PDF Helpers ==========

    private static void draw(PDPageContentStream cs, float x, float y, String label, String value) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 12); cs.newLineAtOffset(x, y); cs.showText(label); cs.endText();
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x + 160, y); cs.showText(value != null ? value : "-"); cs.endText();
    }

    private static String nullTo(String v, String fb) {
        return (v == null || v.isBlank()) ? fb : v;
    }
    
    private void populateAudit(FamilyHistoryDto dto, Meta meta) {
        FamilyHistoryDto.Audit audit = new FamilyHistoryDto.Audit();
        if (meta.hasLastUpdated()) {
            audit.setLastModifiedDate(meta.getLastUpdated().toInstant().atOffset(ZoneOffset.UTC).toLocalDate().toString());
            audit.setCreatedDate(meta.getLastUpdated().toInstant().atOffset(ZoneOffset.UTC).toLocalDate().toString());
        }
        dto.setAudit(audit);
    }
}
