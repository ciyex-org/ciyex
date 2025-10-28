//package com.qiaben.ciyex.service;
//
//import com.qiaben.ciyex.dto.FamilyHistoryDto;
//import com.qiaben.ciyex.entity.FamilyHistory;
//import com.qiaben.ciyex.entity.FamilyHistoryEntry;
//import com.qiaben.ciyex.repository.FamilyHistoryRepository;
//import com.qiaben.ciyex.storage.ExternalFamilyHistoryStorage;
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
//public class FamilyHistoryService {
//
//    private final FamilyHistoryRepository repo;
//    private final Optional<ExternalFamilyHistoryStorage> external;
//
//    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//
//    // CREATE
//    public FamilyHistoryDto create(Long patientId, Long encounterId, FamilyHistoryDto in) {
//        FamilyHistory fh = new FamilyHistory();
//
//        fh.setPatientId(patientId);
//        fh.setEncounterId(encounterId);
//
//        // map entries
//        if (in.getEntries() != null) {
//            for (var e : in.getEntries()) {
//                FamilyHistoryEntry row = FamilyHistoryEntry.builder()
//                        .relation(normalizeRelation(e.getRelation()))
//                        .diagnosisCode(e.getDiagnosisCode())
//                        .diagnosisText(e.getDiagnosisText())
//                        .notes(e.getNotes())
//                        .familyHistory(fh)
//                        .build();
//                fh.getEntries().add(row);
//            }
//        }
//
//        final FamilyHistory saved = repo.save(fh);
//
//        external.ifPresent(ext -> {
//            final FamilyHistory ref = saved;
//            String externalId = ext.create(mapToDto(ref));
//            ref.setExternalId(externalId);
//            repo.save(ref);
//        });
//
//        return mapToDto(saved);
//    }
//
//    // UPDATE (replace entries)
//    public FamilyHistoryDto update(Long patientId, Long encounterId, Long id, FamilyHistoryDto in) {
//        FamilyHistory fh = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Family History not found"));
//
//        // full replace list
//        fh.getEntries().clear();
//        if (in.getEntries() != null) {
//            for (var e : in.getEntries()) {
//                FamilyHistoryEntry row = FamilyHistoryEntry.builder()
//                        .relation(normalizeRelation(e.getRelation()))
//                        .diagnosisCode(e.getDiagnosisCode())
//                        .diagnosisText(e.getDiagnosisText())
//                        .notes(e.getNotes())
//                        .familyHistory(fh)
//                        .build();
//                fh.getEntries().add(row);
//            }
//        }
//
//        final FamilyHistory updated = repo.save(fh);
//
//        external.ifPresent(ext -> {
//            final FamilyHistory ref = updated;
//            if (ref.getExternalId() != null) {
//                ext.update(ref.getExternalId(), mapToDto(ref));
//            }
//        });
//
//        return mapToDto(updated);
//    }
//
//    // DELETE
//    public void delete(Long patientId, Long encounterId, Long id) {
//        FamilyHistory fh = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Family History not found"));
//
//        final FamilyHistory toDelete = fh;
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
//    public FamilyHistoryDto getOne(Long patientId, Long encounterId, Long id) {
//        FamilyHistory fh = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Family History not found"));
//        return mapToDto(fh);
//    }
//
//    // GET ALL by patient
//    public List<FamilyHistoryDto> getAllByPatient(Long patientId) {
//        return repo.findByPatientId(patientId).stream().map(this::mapToDto).toList();
//    }
//
//    // GET ALL by patient + encounter
//    public List<FamilyHistoryDto> getAllByEncounter(Long patientId, Long encounterId) {
//        return repo.findByPatientIdAndEncounterId(patientId, encounterId).stream().map(this::mapToDto).toList();
//    }
//
//    // --- mapping helpers ---
//
//    private FamilyHistoryDto mapToDto(FamilyHistory fh) {
//        FamilyHistoryDto dto = new FamilyHistoryDto();
//        dto.setId(fh.getId());
//        dto.setExternalId(fh.getExternalId());
//        dto.setOrgId(fh.getOrgId());
//        dto.setPatientId(fh.getPatientId());
//        dto.setEncounterId(fh.getEncounterId());
//
//        if (fh.getEntries() != null) {
//            dto.setEntries(
//                    fh.getEntries().stream().map(e -> {
//                        FamilyHistoryDto.EntryDto ed = new FamilyHistoryDto.EntryDto();
//                        ed.setId(e.getId());
//                        ed.setRelation(e.getRelation());
//                        ed.setDiagnosisCode(e.getDiagnosisCode());
//                        ed.setDiagnosisText(e.getDiagnosisText());
//                        ed.setNotes(e.getNotes());
//                        return ed;
//                    }).toList()
//            );
//        }
//
//        FamilyHistoryDto.Audit a = new FamilyHistoryDto.Audit();
//        if (fh.getCreatedAt() != null) a.setCreatedDate(fh.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate().toString());
//        if (fh.getUpdatedAt() != null) a.setLastModifiedDate(fh.getUpdatedAt().atZone(ZoneId.systemDefault()).toLocalDate().toString());
//        dto.setAudit(a);
//
//        return dto;
//    }
//
//    private String normalizeRelation(String r) {
//        if (r == null) return "OTHER";
//        String v = r.trim().toUpperCase();
//        return switch (v) {
//            case "FATHER", "MOTHER", "SIBLING", "SPOUSE", "OFFSPRING" -> v;
//            default -> "OTHER";
//        };
//    }
//}




package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.EntryDto;
import com.qiaben.ciyex.dto.FamilyHistoryDto;
import com.qiaben.ciyex.dto.EntryDto;
import com.qiaben.ciyex.entity.FamilyHistory;
import com.qiaben.ciyex.entity.FamilyHistoryEntry;
import com.qiaben.ciyex.repository.FamilyHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FamilyHistoryService {

    private final FamilyHistoryRepository repo;

    // Create container (and optional entries)
    public FamilyHistoryDto create(Long patientId, Long encounterId, FamilyHistoryDto dto) {
        FamilyHistory fh = new FamilyHistory();
        fh.setPatientId(patientId);
        fh.setEncounterId(encounterId);
        applyEntries(fh, dto.getEntries());
        fh = repo.save(fh);
        return toDto(fh);
    }

    // Get one container
    public FamilyHistoryDto getOne(Long patientId, Long encounterId, Long id) {
        FamilyHistory fh = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Family history not found"));
        return toDto(fh);
    }

    // List containers (usually single per patient+enc)
    public List<FamilyHistoryDto> list(Long patientId, Long encounterId) {
        return repo.findByPatientIdAndEncounterId(patientId, encounterId)
                .stream().map(this::toDto).toList();
    }

    // Replace entries (LOCKED if signed)
    public FamilyHistoryDto update(Long patientId, Long encounterId, Long id, FamilyHistoryDto dto) {
        FamilyHistory fh = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Family history not found"));

        if (Boolean.TRUE.equals(fh.getESigned())) {
            throw new IllegalStateException("Signed family history is read-only.");
        }

        // Replace entries atomically
        fh.getEntries().clear();
        applyEntries(fh, dto.getEntries());
        fh = repo.save(fh);
        return toDto(fh);
    }

    // Delete container (BLOCKED if signed)
    public void delete(Long patientId, Long encounterId, Long id) {
        FamilyHistory fh = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Family history not found"));

        if (Boolean.TRUE.equals(fh.getESigned())) {
            throw new IllegalStateException("Signed family history cannot be deleted.");
        }
        repo.delete(fh);
    }

    // eSign container (idempotent). Accepts optional entryId (which entry initiated sign)
    public FamilyHistoryDto eSign(Long patientId, Long encounterId, Long id, String signedBy, Long entryId) {
        FamilyHistory fh = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Family history not found"));

        if (Boolean.TRUE.equals(fh.getESigned())) {
            // idempotent
            return toDto(fh);
        }

        fh.setESigned(Boolean.TRUE);
        fh.setSignedBy(StringUtils.hasText(signedBy) ? signedBy : "system");
        fh.setSignedAt(java.time.OffsetDateTime.now(ZoneOffset.UTC));
        if (entryId != null) fh.setSignedEntryId(entryId);
        fh = repo.save(fh);

        return toDto(fh);
    }

    // Print PDF (also stamps printedAt)
    public byte[] renderPdf(Long patientId, Long encounterId, Long id) {
        FamilyHistory fh = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Family history not found"));

        fh.setPrintedAt(java.time.OffsetDateTime.now(ZoneOffset.UTC));
        repo.save(fh);

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
                draw(cs, x, y, "Container ID:", String.valueOf(id)); y -= 22;

                // Entries
                var entries = fh.getEntries() == null ? List.<FamilyHistoryEntry>of() : fh.getEntries();
                if (entries.isEmpty()) {
                    draw(cs, x, y, "Entries:", "—"); y -= 16;
                } else {
                    for (FamilyHistoryEntry e : entries) {
                        draw(cs, x, y, "Relation:", nullTo(e.getRelation(), "-")); y -= 16;
                        var diag = (nullTo(e.getDiagnosisText(), "—")
                                + (StringUtils.hasText(e.getDiagnosisCode()) ? " (" + e.getDiagnosisCode() + ")" : ""));
                        draw(cs, x, y, "Diagnosis:", diag); y -= 16;
                        if (StringUtils.hasText(e.getNotes())) { draw(cs, x, y, "Notes:", e.getNotes()); y -= 16; }
                        y -= 8;
                    }
                }

                // eSign footer
                y -= 8;
                draw(cs, x, y, "eSigned:", Boolean.TRUE.equals(fh.getESigned()) ? "Yes" : "No"); y -= 16;
                if (fh.getSignedAt() != null) { draw(cs, x, y, "Signed At:", fh.getSignedAt().toString()); y -= 16; }
                if (StringUtils.hasText(fh.getSignedBy())) { draw(cs, x, y, "Signed By:", fh.getSignedBy()); y -= 16; }
                if (fh.getSignedEntryId() != null) { draw(cs, x, y, "Signed Entry ID:", String.valueOf(fh.getSignedEntryId())); y -= 16; }
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Family History PDF", e);
        }
    }

    // --- helpers ---

    private static void draw(PDPageContentStream cs, float x, float y, String label, String value) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 12); cs.newLineAtOffset(x, y); cs.showText(label); cs.endText();
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x + 160, y); cs.showText(value != null ? value : "-"); cs.endText();
    }

    private static String nullTo(String v, String fb) { return (v == null || v.isBlank()) ? fb : v; }

    private FamilyHistoryDto toDto(FamilyHistory fh) {
        FamilyHistoryDto d = new FamilyHistoryDto();
        d.setId(fh.getId());
        d.setExternalId(fh.getExternalId());
        d.setPatientId(fh.getPatientId());
        d.setEncounterId(fh.getEncounterId());

        d.setESigned(fh.getESigned());
        d.setSignedAt(fh.getSignedAt());
        d.setSignedBy(fh.getSignedBy());
        d.setPrintedAt(fh.getPrintedAt());
        d.setSignedEntryId(fh.getSignedEntryId());

        // entries
        List<EntryDto> list = new ArrayList<>();
        if (fh.getEntries() != null) {
            for (FamilyHistoryEntry e : fh.getEntries()) {
                EntryDto ed = new EntryDto();
                ed.setId(e.getId());
                ed.setRelation(e.getRelation());
                ed.setDiagnosisCode(e.getDiagnosisCode());
                ed.setDiagnosisText(e.getDiagnosisText());
                ed.setNotes(e.getNotes());
                list.add(ed);
            }
        }
        d.setEntries(list);

        d.setCreatedAt(fh.getCreatedAt());
        d.setUpdatedAt(fh.getUpdatedAt());
        return d;
    }

    private void applyEntries(FamilyHistory fh, List<EntryDto> entries) {
        if (entries == null) return;
        for (EntryDto ed : entries) {
            FamilyHistoryEntry e = new FamilyHistoryEntry();
            e.setFamilyHistory(fh);
            e.setRelation(ed.getRelation());
            e.setDiagnosisCode(ed.getDiagnosisCode());
            e.setDiagnosisText(ed.getDiagnosisText());
            e.setNotes(ed.getNotes());
            fh.getEntries().add(e);
        }
    }
}
