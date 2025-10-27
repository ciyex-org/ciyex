//package com.qiaben.ciyex.service;
//
//import com.qiaben.ciyex.dto.SocialHistoryDto;
//import com.qiaben.ciyex.entity.SocialHistory;
//import com.qiaben.ciyex.entity.SocialHistoryEntry;
//import com.qiaben.ciyex.repository.SocialHistoryRepository;
//import com.qiaben.ciyex.storage.ExternalSocialHistoryStorage;
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
//public class SocialHistoryService {
//
//    private final SocialHistoryRepository repo;
//    private final Optional<ExternalSocialHistoryStorage> external;
//
//    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//
//    public SocialHistoryDto create(Long orgId, Long patientId, Long encounterId, SocialHistoryDto in) {
//        SocialHistory sh = new SocialHistory();
//        sh.setOrgId(orgId);
//        sh.setPatientId(patientId);
//        sh.setEncounterId(encounterId);
//
//        if (in.getEntries() != null) {
//            for (var e : in.getEntries()) {
//                SocialHistoryEntry row = SocialHistoryEntry.builder()
//                        .category(normalizeCategory(e.getCategory()))
//                        .value(e.getValue())
//                        .details(e.getDetails())
//                        .socialHistory(sh)
//                        .build();
//                sh.getEntries().add(row);
//            }
//        }
//
//        final SocialHistory saved = repo.save(sh);
//
//        external.ifPresent(ext -> {
//            final SocialHistory ref = saved;
//            String externalId = ext.create(mapToDto(ref));
//            ref.setExternalId(externalId);
//            repo.save(ref);
//        });
//
//        return mapToDto(saved);
//    }
//
//    public SocialHistoryDto update(Long orgId, Long patientId, Long encounterId, Long id, SocialHistoryDto in) {
//        SocialHistory sh = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Social History not found"));
//
//        sh.getEntries().clear();
//        if (in.getEntries() != null) {
//            for (var e : in.getEntries()) {
//                SocialHistoryEntry row = SocialHistoryEntry.builder()
//                        .category(normalizeCategory(e.getCategory()))
//                        .value(e.getValue())
//                        .details(e.getDetails())
//                        .socialHistory(sh)
//                        .build();
//                sh.getEntries().add(row);
//            }
//        }
//
//        final SocialHistory updated = repo.save(sh);
//
//        external.ifPresent(ext -> {
//            final SocialHistory ref = updated;
//            if (ref.getExternalId() != null) {
//                ext.update(ref.getExternalId(), mapToDto(ref));
//            }
//        });
//
//        return mapToDto(updated);
//    }
//
//    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
//        SocialHistory sh = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Social History not found"));
//
//        final SocialHistory toDelete = sh;
//        external.ifPresent(ext -> {
//            if (toDelete.getExternalId() != null) {
//                ext.delete(toDelete.getExternalId());
//            }
//        });
//
//        repo.delete(toDelete);
//    }
//
//    public SocialHistoryDto getOne(Long orgId, Long patientId, Long encounterId, Long id) {
//        SocialHistory sh = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Social History not found"));
//        return mapToDto(sh);
//    }
//
//    public List<SocialHistoryDto> getAllByPatient(Long orgId, Long patientId) {
//        return repo.findByPatientId(patientId).stream().map(this::mapToDto).toList();
//    }
//
//    public List<SocialHistoryDto> getAllByEncounter(Long orgId, Long patientId, Long encounterId) {
//        return repo.findByPatientIdAndEncounterId(patientId, encounterId).stream().map(this::mapToDto).toList();
//    }
//
//    private SocialHistoryDto mapToDto(SocialHistory sh) {
//        SocialHistoryDto dto = new SocialHistoryDto();
//        dto.setId(sh.getId());
//        dto.setExternalId(sh.getExternalId());
//        dto.setOrgId(sh.getOrgId());
//        dto.setPatientId(sh.getPatientId());
//        dto.setEncounterId(sh.getEncounterId());
//
//        dto.setEntries(sh.getEntries().stream().map(e -> {
//            SocialHistoryDto.EntryDto ed = new SocialHistoryDto.EntryDto();
//            ed.setCategory(e.getCategory());
//            ed.setValue(e.getValue());
//            ed.setDetails(e.getDetails());
//            return ed;
//        }).toList());
//
//        SocialHistoryDto.Audit a = new SocialHistoryDto.Audit();
//        if (sh.getCreatedAt() != null) a.setCreatedDate(sh.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate().toString());
//        if (sh.getUpdatedAt() != null) a.setLastModifiedDate(sh.getUpdatedAt().atZone(ZoneId.systemDefault()).toLocalDate().toString());
//        dto.setAudit(a);
//
//        return dto;
//    }
//
//    private String normalizeCategory(String c) {
//        if (c == null) return "OTHER";
//        String v = c.trim().toUpperCase().replace(' ', '_');
//        return switch (v) {
//            case "SMOKING", "ALCOHOL", "DRUGS", "OCCUPATION", "MARITAL_STATUS",
//                 "EXERCISE", "DIET", "HOUSING", "EDUCATION", "SEXUAL_HISTORY" -> v;
//            default -> "OTHER";
//        };
//    }
//}




package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.SocialHistoryDto;
import com.qiaben.ciyex.dto.SocialHistoryEntryDto;
import com.qiaben.ciyex.entity.SocialHistory;
import com.qiaben.ciyex.entity.SocialHistoryEntry;
import com.qiaben.ciyex.repository.SocialHistoryRepository;
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
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialHistoryService {

    private final SocialHistoryRepository repo;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // CREATE (container + entries)
    public SocialHistoryDto create(Long orgId, Long patientId, Long encounterId, SocialHistoryDto dto) {
        SocialHistory e = new SocialHistory();
        e.setPatientId(patientId);
        e.setEncounterId(encounterId);
        applyEntries(e, dto.getEntries());
        e = repo.save(e);
        return toDto(e);
    }

    // READ one container (first if multiple)
    public SocialHistoryDto getOne(Long orgId, Long patientId, Long encounterId) {
        List<SocialHistory> list = repo.findByPatientIdAndEncounterId(patientId, encounterId);
        SocialHistory e = list.isEmpty() ? null : list.get(0);
        if (e == null) throw new IllegalArgumentException("Social History not found");
        return toDto(e);
    }

    // READ by id
    public SocialHistoryDto getById(Long orgId, Long patientId, Long encounterId, Long id) {
        SocialHistory e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Social History not found"));
        return toDto(e);
    }

    // UPDATE container (blocked if signed)
    public SocialHistoryDto update(Long orgId, Long patientId, Long encounterId, Long id, SocialHistoryDto dto) {
        SocialHistory e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Social History not found"));
        if (Boolean.TRUE.equals(e.getESigned())) {
            throw new IllegalStateException("Signed Social History is read-only.");
        }
        applyEntries(e, dto.getEntries());
        e = repo.save(e);
        return toDto(e);
    }

    // DELETE container (blocked if signed)
    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
        SocialHistory e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Social History not found"));
        if (Boolean.TRUE.equals(e.getESigned())) {
            throw new IllegalStateException("Signed Social History cannot be deleted.");
        }
        repo.delete(e);
    }

    // eSIGN (idempotent)
    public SocialHistoryDto eSign(Long orgId, Long patientId, Long encounterId, Long id, String signedBy) {
        SocialHistory e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Social History not found"));
        if (Boolean.TRUE.equals(e.getESigned())) return toDto(e);
        e.setESigned(true);
        e.setSignedAt(OffsetDateTime.now(ZoneOffset.UTC));
        e.setSignedBy(StringUtils.hasText(signedBy) ? signedBy : "system");
        e = repo.save(e);
        return toDto(e);
    }

    // PRINT (PDF) — also stamps printedAt
    public byte[] renderPdf(Long orgId, Long patientId, Long encounterId, Long id) {
        SocialHistory e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Social History not found"));
        e.setPrintedAt(OffsetDateTime.now(ZoneOffset.UTC));
        repo.save(e);

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
                draw(cs, x, y, "Record ID:", String.valueOf(id)); y -= 22;

                if (e.getEntries() != null && !e.getEntries().isEmpty()) {
                    for (int i = 0; i < e.getEntries().size(); i++) {
                        SocialHistoryEntry it = e.getEntries().get(i);
                        cs.beginText();
                        cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                        cs.newLineAtOffset(x, y);
                        cs.showText((i + 1) + ". " + (it.getCategory() != null ? it.getCategory() : "—"));
                        cs.endText();
                        y -= 14;

                        if (StringUtils.hasText(it.getValue())) { draw(cs, x + 16, y, "Value:", it.getValue()); y -= 14; }
                        if (StringUtils.hasText(it.getDetails())) {
                            draw(cs, x + 16, y, "Details:", ""); y -= 14;
                            for (String ln : it.getDetails().split("\\R")) {
                                cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x + 16, y);
                                cs.showText(ln); cs.endText();
                                y -= 14;
                            }
                        }
                        y -= 8;
                    }
                } else {
                    draw(cs, x, y, "Entries:", "(none)"); y -= 16;
                }

                y -= 8;
                draw(cs, x, y, "eSigned:", Boolean.TRUE.equals(e.getESigned()) ? "Yes" : "No"); y -= 16;
                if (e.getSignedAt() != null) { draw(cs, x, y, "Signed At:", e.getSignedAt().toString()); y -= 16; }
                if (StringUtils.hasText(e.getSignedBy())) { draw(cs, x, y, "Signed By:", e.getSignedBy()); y -= 16; }
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate Social History PDF", ex);
        }
    }

    // ---- helpers ----
    private static void draw(PDPageContentStream cs, float x, float y, String label, String value) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 12); cs.newLineAtOffset(x, y); cs.showText(label); cs.endText();
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x + 140, y); cs.showText(value != null ? value : "-"); cs.endText();
    }

    private SocialHistoryDto toDto(SocialHistory e) {
        SocialHistoryDto d = new SocialHistoryDto();
        d.setId(e.getId());
        d.setExternalId(e.getExternalId());
        d.setPatientId(e.getPatientId());
        d.setEncounterId(e.getEncounterId());

        List<SocialHistoryEntryDto> list = new ArrayList<>();
        for (SocialHistoryEntry it : e.getEntries()) {
            SocialHistoryEntryDto ed = new SocialHistoryEntryDto();
            ed.setId(it.getId());
            ed.setCategory(it.getCategory());
            ed.setDetails(it.getDetails());
            ed.setValue(it.getValue());
            list.add(ed);
        }
        d.setEntries(list);

        d.setESigned(e.getESigned());
        d.setSignedAt(e.getSignedAt());
        d.setSignedBy(e.getSignedBy());
        d.setPrintedAt(e.getPrintedAt());

        SocialHistoryDto.Audit a = new SocialHistoryDto.Audit();
        if (e.getCreatedAt() != null) a.setCreatedDate(DAY.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
        if (e.getUpdatedAt() != null) a.setLastModifiedDate(DAY.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
        d.setAudit(a);
        return d;
    }

    private void applyEntries(SocialHistory e, List<SocialHistoryEntryDto> dtos) {
        e.getEntries().clear();
        if (dtos == null) return;
        for (SocialHistoryEntryDto d : dtos) {
            SocialHistoryEntry it = new SocialHistoryEntry();
            it.setCategory(d.getCategory());
            it.setDetails(d.getDetails());
            it.setValue(d.getValue());
            it.setSocialHistory(e);
            e.getEntries().add(it);
        }
    }
}
