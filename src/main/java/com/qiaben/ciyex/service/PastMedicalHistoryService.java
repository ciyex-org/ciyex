//package com.qiaben.ciyex.service;
//
//import com.qiaben.ciyex.dto.PastMedicalHistoryDto;
//import com.qiaben.ciyex.entity.PastMedicalHistory;
//import com.qiaben.ciyex.repository.PastMedicalHistoryRepository;
//import com.qiaben.ciyex.storage.ExternalPastMedicalHistoryStorage;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.time.ZoneId;
//import java.time.format.DateTimeFormatter;
//import java.util.List;
//import java.util.Optional;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class PastMedicalHistoryService {
//
//    private final PastMedicalHistoryRepository repo;
//    private final Optional<ExternalPastMedicalHistoryStorage> external; // make external optional if not always used
//
//    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//
//    // CREATE
//    public PastMedicalHistoryDto create(Long orgId, Long patientId, Long encounterId, PastMedicalHistoryDto in) {
//        PastMedicalHistory toSave = PastMedicalHistory.builder()
//                .orgId(orgId)
//                .patientId(patientId)
//                .encounterId(encounterId)
//                .description(in.getDescription())
//                .build();
//
//        final PastMedicalHistory saved = repo.save(toSave);
//
//        external.ifPresent(ext -> {
//            final PastMedicalHistory ref = saved;
//            String externalId = ext.create(mapToDto(ref));
//            ref.setExternalId(externalId);
//            repo.save(ref);
//        });
//
//        return mapToDto(saved);
//    }
//
//    // UPDATE
//    public PastMedicalHistoryDto update(Long orgId, Long patientId, Long encounterId, Long id, PastMedicalHistoryDto in) {
//        PastMedicalHistory entity = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("PMH not found"));
//
//        entity.setDescription(in.getDescription());
//        final PastMedicalHistory updated = repo.save(entity);
//
//        external.ifPresent(ext -> {
//            final PastMedicalHistory e = updated;
//            if (e.getExternalId() != null) {
//                ext.update(e.getExternalId(), mapToDto(e));
//            }
//        });
//
//        return mapToDto(updated);
//    }
//
//    // DELETE
//    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
//        PastMedicalHistory entity = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("PMH not found"));
//
//        final PastMedicalHistory toDelete = entity;
//        external.ifPresent(ext -> {
//            if (toDelete.getExternalId() != null) {
//                ext.delete(toDelete.getExternalId());
//            }
//        });
//
//        repo.delete(toDelete);
//    }
//
//    // GET ONE
//    public PastMedicalHistoryDto getOne(Long orgId, Long patientId, Long encounterId, Long id) {
//        PastMedicalHistory entity = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("PMH not found"));
//        return mapToDto(entity);
//    }
//
//    // GET ALL by patient
//    public List<PastMedicalHistoryDto> getAllByPatient(Long orgId, Long patientId) {
//        return repo.findByOrgIdAndPatientId(orgId, patientId).stream().map(this::mapToDto).toList();
//    }
//
//    // GET ALL by patient + encounter
//    public List<PastMedicalHistoryDto> getAllByEncounter(Long orgId, Long patientId, Long encounterId) {
//        return repo.findByOrgIdAndPatientIdAndEncounterId(orgId, patientId, encounterId).stream().map(this::mapToDto).toList();
//    }
//
//    // Mapping
//    private PastMedicalHistoryDto mapToDto(PastMedicalHistory e) {
//        PastMedicalHistoryDto dto = new PastMedicalHistoryDto();
//        dto.setId(e.getId());
//        dto.setExternalId(e.getExternalId());
//        dto.setOrgId(e.getOrgId());
//        dto.setPatientId(e.getPatientId());
//        dto.setEncounterId(e.getEncounterId());
//        dto.setDescription(e.getDescription());
//
//        PastMedicalHistoryDto.Audit audit = new PastMedicalHistoryDto.Audit();
//        if (e.getCreatedAt() != null) {
//            audit.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
//        }
//        if (e.getUpdatedAt() != null) {
//            audit.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
//        }
//        dto.setAudit(audit);
//        return dto;
//    }
//}




package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.PastMedicalHistoryDto;
import com.qiaben.ciyex.entity.PastMedicalHistory;
import com.qiaben.ciyex.repository.PastMedicalHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PastMedicalHistoryService {

    private final PastMedicalHistoryRepository repo;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Create
    public PastMedicalHistoryDto create(Long orgId, Long patientId, Long encounterId, PastMedicalHistoryDto dto) {
        PastMedicalHistory e = new PastMedicalHistory();
        e.setOrgId(orgId);
        e.setPatientId(patientId);
        e.setEncounterId(encounterId);
        applyDto(e, dto);
        e = repo.save(e);
        return toDto(e);
    }

    // Read
    public PastMedicalHistoryDto getOne(Long orgId, Long patientId, Long encounterId, Long id) {
        PastMedicalHistory e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("PMH not found"));
        return toDto(e);
    }

    public List<PastMedicalHistoryDto> list(Long orgId, Long patientId, Long encounterId) {
        return repo.findByOrgIdAndPatientIdAndEncounterId(orgId, patientId, encounterId)
                .stream().map(this::toDto).toList();
    }

    // Update (blocked if signed)
    public PastMedicalHistoryDto update(Long orgId, Long patientId, Long encounterId, Long id, PastMedicalHistoryDto dto) {
        PastMedicalHistory e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("PMH not found"));
        if (Boolean.TRUE.equals(e.getESigned())) throw new IllegalStateException("Signed PMH entries are read-only.");

        applyDto(e, dto);
        e = repo.save(e);
        return toDto(e);
    }

    // Delete (blocked if signed)
    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
        PastMedicalHistory e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("PMH not found"));
        if (Boolean.TRUE.equals(e.getESigned())) throw new IllegalStateException("Signed PMH entries cannot be deleted.");

        repo.delete(e);
    }

    // eSign (idempotent)
    public PastMedicalHistoryDto eSign(Long orgId, Long patientId, Long encounterId, Long id, String signedBy) {
        PastMedicalHistory e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("PMH not found"));
        if (Boolean.TRUE.equals(e.getESigned())) return toDto(e);

        e.setESigned(Boolean.TRUE);
        e.setSignedBy((signedBy == null || signedBy.isBlank()) ? "system" : signedBy);
        e.setSignedAt(java.time.OffsetDateTime.now(ZoneOffset.UTC));
        e = repo.save(e);
        return toDto(e);
    }

    // Print (PDF) — stamps printedAt
    public byte[] renderPdf(Long orgId, Long patientId, Long encounterId, Long id) {
        PastMedicalHistory e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("PMH not found"));

        e.setPrintedAt(java.time.OffsetDateTime.now(ZoneOffset.UTC));
        repo.save(e);

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
                draw(cs, x, y, "PMH ID:", String.valueOf(id)); y -= 22;

                drawMultiline(cs, x, y, "Description:", e.getDescription(), 80);

                y -= 8;
                draw(cs, x, y, "eSigned:", Boolean.TRUE.equals(e.getESigned()) ? "Yes" : "No"); y -= 16;
                if (e.getSignedAt() != null) { draw(cs, x, y, "Signed At:", e.getSignedAt().toString()); y -= 16; }
                if (e.getSignedBy() != null) { draw(cs, x, y, "Signed By:", e.getSignedBy()); y -= 16; }
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate PMH PDF", ex);
        }
    }

    // ---- helpers ----
    private static void draw(PDPageContentStream cs, float x, float y, String label, String value) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 12); cs.newLineAtOffset(x, y); cs.showText(label); cs.endText();
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x + 140, y); cs.showText(value != null ? value : "-"); cs.endText();
    }

    private static void drawMultiline(PDPageContentStream cs, float x, float y, String label, String text, int wrap) throws IOException {
        draw(cs, x, y, label, ""); y -= 16;
        if (text == null || text.isBlank()) {
            draw(cs, x, y, "", "-");
            return;
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
    }

    private PastMedicalHistoryDto toDto(PastMedicalHistory e) {
        PastMedicalHistoryDto d = new PastMedicalHistoryDto();
        d.setId(e.getId());
        d.setExternalId(e.getExternalId());
        d.setOrgId(e.getOrgId());
        d.setPatientId(e.getPatientId());
        d.setEncounterId(e.getEncounterId());
        d.setDescription(e.getDescription());

        d.setESigned(e.getESigned());
        d.setSignedAt(e.getSignedAt());
        d.setSignedBy(e.getSignedBy());
        d.setPrintedAt(e.getPrintedAt());

        PastMedicalHistoryDto.Audit a = new PastMedicalHistoryDto.Audit();
        if (e.getCreatedAt() != null) a.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
        if (e.getUpdatedAt() != null) a.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
        d.setAudit(a);

        return d;
    }

    private void applyDto(PastMedicalHistory e, PastMedicalHistoryDto d) {
        e.setExternalId(d.getExternalId());
        e.setDescription(d.getDescription());
        // eSign fields are managed via eSign()
    }
}
