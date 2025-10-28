//package com.qiaben.ciyex.service;
//
//import com.qiaben.ciyex.dto.HistoryOfPresentIllnessDto;
//import com.qiaben.ciyex.entity.HistoryOfPresentIllness;
//import com.qiaben.ciyex.repository.HistoryOfPresentIllnessRepository;
//import com.qiaben.ciyex.storage.ExternalHistoryOfPresentIllnessStorage;
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
//public class HistoryOfPresentIllnessService {
//
//    private final HistoryOfPresentIllnessRepository repo;
//    private final Optional<ExternalHistoryOfPresentIllnessStorage> external;
//
//    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//
//    // CREATE
//    public HistoryOfPresentIllnessDto create(Long patientId, Long encounterId, HistoryOfPresentIllnessDto in) {
//        HistoryOfPresentIllness toSave = HistoryOfPresentIllness.builder()

//                .patientId(patientId)
//                .encounterId(encounterId)
//                .description(in.getDescription())
//                .build();
//
//        final HistoryOfPresentIllness saved = repo.save(toSave);
//
//        external.ifPresent(ext -> {
//            final HistoryOfPresentIllness ref = saved;
//            String externalId = ext.create(mapToDto(ref));
//            ref.setExternalId(externalId);
//            repo.save(ref);
//        });
//
//        return mapToDto(saved);
//    }
//
//    // UPDATE
//    public HistoryOfPresentIllnessDto update(Long patientId, Long encounterId, Long id, HistoryOfPresentIllnessDto in) {
//        HistoryOfPresentIllness entity = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("HPI not found"));
//
//        entity.setDescription(in.getDescription());
//        final HistoryOfPresentIllness updated = repo.save(entity);
//
//        external.ifPresent(ext -> {
//            final HistoryOfPresentIllness e = updated;
//            if (e.getExternalId() != null) {
//                ext.update(e.getExternalId(), mapToDto(e));
//            }
//        });
//
//        return mapToDto(updated);
//    }
//
//    // DELETE
//    public void delete(Long patientId, Long encounterId, Long id) {
//        HistoryOfPresentIllness entity = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("HPI not found"));
//
//        final HistoryOfPresentIllness toDelete = entity;
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
//    public HistoryOfPresentIllnessDto getOne(Long patientId, Long encounterId, Long id) {
//        HistoryOfPresentIllness entity = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("HPI not found"));
//        return mapToDto(entity);
//    }
//
//    // GET ALL by patient
//    public List<HistoryOfPresentIllnessDto> getAllByPatient(Long patientId) {
//        return repo.findByPatientId(patientId).stream().map(this::mapToDto).toList();
//    }
//
//    // GET ALL by patient + encounter
//    public List<HistoryOfPresentIllnessDto> getAllByEncounter(Long patientId, Long encounterId) {
//        return repo.findByPatientIdAndEncounterId(patientId, encounterId).stream().map(this::mapToDto).toList();
//    }
//
//    // Mapping
//    private HistoryOfPresentIllnessDto mapToDto(HistoryOfPresentIllness e) {
//        HistoryOfPresentIllnessDto dto = new HistoryOfPresentIllnessDto();
//        dto.setId(e.getId());
//        dto.setExternalId(e.getExternalId());
//        dto.setOrgId(e.getOrgId());
//        dto.setPatientId(e.getPatientId());
//        dto.setEncounterId(e.getEncounterId());
//        dto.setDescription(e.getDescription());
//
//        HistoryOfPresentIllnessDto.Audit a = new HistoryOfPresentIllnessDto.Audit();
//        if (e.getCreatedAt() != null) {
//            a.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
//        }
//        if (e.getUpdatedAt() != null) {
//            a.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
//        }
//        dto.setAudit(a);
//        return dto;
//    }
//}




package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.HistoryOfPresentIllnessDto;
import com.qiaben.ciyex.entity.HistoryOfPresentIllness;
import com.qiaben.ciyex.repository.HistoryOfPresentIllnessRepository;
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
public class HistoryOfPresentIllnessService {

    private final HistoryOfPresentIllnessRepository repo;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Create
    public HistoryOfPresentIllnessDto create(Long patientId, Long encounterId, HistoryOfPresentIllnessDto dto) {
        HistoryOfPresentIllness e = new HistoryOfPresentIllness();
        e.setPatientId(patientId);
        e.setEncounterId(encounterId);
        applyDto(e, dto);
        e = repo.save(e);
        return toDto(e);
    }

    // Read one
    public HistoryOfPresentIllnessDto getOne(Long patientId, Long encounterId, Long id) {
        HistoryOfPresentIllness e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("HPI not found"));
        return toDto(e);
    }

    // List by encounter
    public List<HistoryOfPresentIllnessDto> list(Long patientId, Long encounterId) {
        return repo.findByPatientIdAndEncounterId(patientId, encounterId)
                .stream().map(this::toDto).toList();
    }

    // Update (blocked if signed)
    public HistoryOfPresentIllnessDto update(Long patientId, Long encounterId, Long id, HistoryOfPresentIllnessDto dto) {
        HistoryOfPresentIllness e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("HPI not found"));
        if (Boolean.TRUE.equals(e.getESigned())) throw new IllegalStateException("Signed HPI entries are read-only.");

        applyDto(e, dto);
        e = repo.save(e);
        return toDto(e);
    }

    // Delete (blocked if signed)
    public void delete(Long patientId, Long encounterId, Long id) {
        HistoryOfPresentIllness e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("HPI not found"));
        if (Boolean.TRUE.equals(e.getESigned())) throw new IllegalStateException("Signed HPI entries cannot be deleted.");
        repo.delete(e);
    }

    // eSign (idempotent)
    public HistoryOfPresentIllnessDto eSign(Long patientId, Long encounterId, Long id, String signedBy) {
        HistoryOfPresentIllness e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("HPI not found"));
        if (Boolean.TRUE.equals(e.getESigned())) return toDto(e);

        e.setESigned(Boolean.TRUE);
        e.setSignedBy(signedBy == null || signedBy.isBlank() ? "system" : signedBy);
        e.setSignedAt(java.time.OffsetDateTime.now(ZoneOffset.UTC));
        e = repo.save(e);
        return toDto(e);
    }

    // Print (PDF) — stamps printedAt
    public byte[] renderPdf(Long patientId, Long encounterId, Long id) {
        HistoryOfPresentIllness e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("HPI not found"));

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
                cs.showText("History of Present Illness");
                cs.endText();

                y -= 26;
                draw(cs, x, y, "Patient ID:", String.valueOf(patientId)); y -= 16;
                draw(cs, x, y, "Encounter ID:", String.valueOf(encounterId)); y -= 16;
                draw(cs, x, y, "HPI ID:", String.valueOf(id)); y -= 22;

                drawMultiline(cs, x, y, "Description:", e.getDescription(), 80);
                // (drawMultiline returns last y; we don't need it further here)

                y -= 8;
                draw(cs, x, y, "eSigned:", Boolean.TRUE.equals(e.getESigned()) ? "Yes" : "No"); y -= 16;
                if (e.getSignedAt() != null) { draw(cs, x, y, "Signed At:", e.getSignedAt().toString()); y -= 16; }
                if (e.getSignedBy() != null) { draw(cs, x, y, "Signed By:", e.getSignedBy()); y -= 16; }
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate HPI PDF", ex);
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

    private HistoryOfPresentIllnessDto toDto(HistoryOfPresentIllness e) {
        HistoryOfPresentIllnessDto d = new HistoryOfPresentIllnessDto();
        d.setId(e.getId());
        d.setExternalId(e.getExternalId());
        d.setPatientId(e.getPatientId());
        d.setEncounterId(e.getEncounterId());
        d.setDescription(e.getDescription());

        d.setESigned(e.getESigned());
        d.setSignedAt(e.getSignedAt());
        d.setSignedBy(e.getSignedBy());
        d.setPrintedAt(e.getPrintedAt());

        HistoryOfPresentIllnessDto.Audit a = new HistoryOfPresentIllnessDto.Audit();
        if (e.getCreatedAt() != null) a.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
        if (e.getUpdatedAt() != null) a.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
        d.setAudit(a);
        return d;
    }

    private void applyDto(HistoryOfPresentIllness e, HistoryOfPresentIllnessDto d) {
        e.setExternalId(d.getExternalId());
        e.setDescription(d.getDescription());
        // eSign fields managed by eSign()
    }
}
