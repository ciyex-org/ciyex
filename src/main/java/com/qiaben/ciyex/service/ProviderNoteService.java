//package com.qiaben.ciyex.service;
//
//import com.qiaben.ciyex.dto.ProviderNoteDto;
//import com.qiaben.ciyex.entity.ProviderNote;
//import com.qiaben.ciyex.repository.ProviderNoteRepository;
//import com.qiaben.ciyex.storage.ExternalProviderNoteStorage;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.util.StringUtils;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.time.format.DateTimeParseException;
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class ProviderNoteService {
//
//    private final ProviderNoteRepository repo;
//    private final ExternalProviderNoteStorage externalStorage; // FHIR bridge (no-op impl provided)
//
//    private static final DateTimeFormatter ISO_TS = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
//
//    @Transactional
//    public ProviderNoteDto create(Long orgId, Long patientId, Long encounterId, ProviderNoteDto d) {
//        ProviderNote e = new ProviderNote();
//        apply(e, orgId, patientId, encounterId, d);
//        ProviderNote saved = repo.save(e);
//        safeExternalCreate(saved);
//        return toDto(saved);
//    }
//
//    @Transactional
//    public ProviderNoteDto update(Long orgId, Long patientId, Long encounterId, Long id, ProviderNoteDto d) {
//        ProviderNote e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Provider note not found"));
//        apply(e, orgId, patientId, encounterId, d);
//        ProviderNote updated = repo.save(e);
//        safeExternalUpdate(updated);
//        return toDto(updated);
//    }
//
//    @Transactional(readOnly = true)
//    public ProviderNoteDto getOne(Long orgId, Long patientId, Long encounterId, Long id) {
//        ProviderNote e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Provider note not found"));
//        return toDto(e);
//    }
//
//    @Transactional(readOnly = true)
//    public List<ProviderNoteDto> getAllByEncounter(Long orgId, Long patientId, Long encounterId) {
//        return repo.findByOrgIdAndPatientIdAndEncounterId(orgId, patientId, encounterId)
//                .stream().map(this::toDto).toList();
//    }
//
//    @Transactional
//    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
//        ProviderNote e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Provider note not found"));
//        repo.delete(e);
//        safeExternalDelete(e);
//    }
//
//    /* ---------- mapping & parsing ---------- */
//
//    private void apply(ProviderNote e, Long orgId, Long patientId, Long encounterId, ProviderNoteDto d) {
//        e.setOrgId(orgId);
//        e.setPatientId(patientId);
//        e.setEncounterId(encounterId);
//
//        e.setNoteTitle(trim(d.getNoteTitle()));
//        e.setNoteTypeCode(trim(d.getNoteTypeCode()));
//        e.setNoteStatus(trim(d.getNoteStatus()));
//        e.setNoteDateTime(parseDateTime(d.getNoteDateTime()));
//        e.setAuthorPractitionerId(parseLong(d.getAuthorPractitionerId()));
//
//        e.setSubjective(d.getSubjective());
//        e.setObjective(d.getObjective());
//        e.setAssessment(d.getAssessment());
//        e.setPlan(d.getPlan());
//        e.setNarrative(d.getNarrative());
//        e.setExternalId(trim(d.getExternalId()));
//    }
//
//    private ProviderNoteDto toDto(ProviderNote e) {
//        return ProviderNoteDto.builder()
//                .id(s(e.getId()))
//                .orgId(s(e.getOrgId()))
//                .patientId(s(e.getPatientId()))
//                .encounterId(s(e.getEncounterId()))
//                .noteTitle(e.getNoteTitle())
//                .noteTypeCode(e.getNoteTypeCode())
//                .noteStatus(e.getNoteStatus())
//                .noteDateTime(e.getNoteDateTime() != null ? e.getNoteDateTime().toString() : null)
//                .authorPractitionerId(s(e.getAuthorPractitionerId()))
//                .subjective(e.getSubjective())
//                .objective(e.getObjective())
//                .assessment(e.getAssessment())
//                .plan(e.getPlan())
//                .narrative(e.getNarrative())
//                .externalId(e.getExternalId())
//                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
//                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
//                .build();
//    }
//
//    private static String trim(String s){ return StringUtils.hasText(s) ? s.trim() : null; }
//    private static String s(Object o){ return o == null ? null : String.valueOf(o); }
//
//    private static Long parseLong(String s){
//        return StringUtils.hasText(s) ? Long.valueOf(s.trim()) : null;
//    }
//
//    private static LocalDateTime parseDateTime(String s){
//        if (!StringUtils.hasText(s)) return null;
//        String v = s.trim();
//        try {
//            // "2025-09-09T12:34:56"
//            return LocalDateTime.parse(v, ISO_TS);
//        } catch (DateTimeParseException ignore) {
//            // "2025-09-09"
//            try { return LocalDate.parse(v).atStartOfDay(); }
//            catch (DateTimeParseException ex) {
//                throw new IllegalArgumentException("Invalid noteDateTime: " + v);
//            }
//        }
//    }
//
//    /* ---------- external bridge safe wrappers ---------- */
//
//    private void safeExternalCreate(ProviderNote e){
//        try { externalStorage.onCreated(e); }
//        catch (Exception ex){ log.warn("external create failed: {}", ex.getMessage()); }
//    }
//    private void safeExternalUpdate(ProviderNote e){
//        try { externalStorage.onUpdated(e); }
//        catch (Exception ex){ log.warn("external update failed: {}", ex.getMessage()); }
//    }
//    private void safeExternalDelete(ProviderNote e){
//        try { externalStorage.onDeleted(e); }
//        catch (Exception ex){ log.warn("external delete failed: {}", ex.getMessage()); }
//    }
//}




package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ProviderNoteDto;
import com.qiaben.ciyex.entity.ProviderNote;
import com.qiaben.ciyex.repository.ProviderNoteRepository;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProviderNoteService {

    private final ProviderNoteRepository repo;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    // Create
    public ProviderNoteDto create(Long orgId, Long patientId, Long encounterId, ProviderNoteDto dto) {
        ProviderNote e = new ProviderNote();
        e.setOrgId(orgId);
        e.setPatientId(patientId);
        e.setEncounterId(encounterId);
        applyDto(e, dto);
        e = repo.save(e);
        return toDto(e);
    }

    // Read one
    public ProviderNoteDto getOne(Long orgId, Long patientId, Long encounterId, Long id) {
        ProviderNote e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Provider note not found"));
        return toDto(e);
    }

    // List
    public List<ProviderNoteDto> list(Long orgId, Long patientId, Long encounterId) {
        return repo.findByOrgIdAndPatientIdAndEncounterId(orgId, patientId, encounterId)
                .stream().map(this::toDto).toList();
    }

    // Update (blocked if signed)
    public ProviderNoteDto update(Long orgId, Long patientId, Long encounterId, Long id, ProviderNoteDto dto) {
        ProviderNote e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Provider note not found"));
        if (Boolean.TRUE.equals(e.getESigned())) {
            throw new IllegalStateException("Signed provider notes are read-only.");
        }
        applyDto(e, dto);
        e = repo.save(e);
        return toDto(e);
    }

    // Delete (blocked if signed)
    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
        ProviderNote e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Provider note not found"));
        if (Boolean.TRUE.equals(e.getESigned())) {
            throw new IllegalStateException("Signed provider notes cannot be deleted.");
        }
        repo.delete(e);
    }

    // eSign (idempotent)
    public ProviderNoteDto eSign(Long orgId, Long patientId, Long encounterId, Long id, String signedBy) {
        ProviderNote e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Provider note not found"));
        if (Boolean.TRUE.equals(e.getESigned())) return toDto(e);

        e.setESigned(true);
        e.setSignedBy(StringUtils.hasText(signedBy) ? signedBy : "system");
        e.setSignedAt(OffsetDateTime.now(ZoneOffset.UTC));
        e = repo.save(e);
        return toDto(e);
    }
    public byte[] renderPdf(Long orgId, Long patientId, Long encounterId, Long id) {
        ProviderNote e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Provider note not found"));

        e.setPrintedAt(OffsetDateTime.now(ZoneOffset.UTC));
        repo.save(e);

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
                draw(cs, x, y, "Note ID:", String.valueOf(id)); y -= 18;
                if (e.getNoteDateTime() != null) { draw(cs, x, y, "Date/Time:", e.getNoteDateTime().toString()); y -= 18; }
                if (e.getAuthorPractitionerId() != null) { draw(cs, x, y, "Author:", String.valueOf(e.getAuthorPractitionerId())); y -= 18; }

                if (StringUtils.hasText(e.getNoteTitle())) { draw(cs, x, y, "Title:", e.getNoteTitle()); y -= 18; }
                if (StringUtils.hasText(e.getNoteTypeCode())) { draw(cs, x, y, "Type:", e.getNoteTypeCode()); y -= 18; }
                if (StringUtils.hasText(e.getNoteStatus())) { draw(cs, x, y, "Status:", e.getNoteStatus()); y -= 18; }

                // SOAP sections (using draw)
                y -= 24;
                if (StringUtils.hasText(e.getSubjective())) { draw(cs, x, y, "S (Subjective):", e.getSubjective()); y -= 18; }
                if (StringUtils.hasText(e.getObjective())) { draw(cs, x, y, "O (Objective):", e.getObjective()); y -= 18; }
                if (StringUtils.hasText(e.getAssessment())) { draw(cs, x, y, "A (Assessment):", e.getAssessment()); y -= 18; }
                if (StringUtils.hasText(e.getPlan())) { draw(cs, x, y, "P (Plan):", e.getPlan()); y -= 18; }
                if (StringUtils.hasText(e.getNarrative())) { draw(cs, x, y, "Narrative:", e.getNarrative()); y -= 18; }

                // Signature info
                y -= 20;
                draw(cs, x, y, "eSigned:", Boolean.TRUE.equals(e.getESigned()) ? "Yes" : "No"); y -= 18;
                if (e.getSignedAt() != null) { draw(cs, x, y, "Signed At:", e.getSignedAt().format(ISO)); y -= 18; }
                if (StringUtils.hasText(e.getSignedBy())) { draw(cs, x, y, "Signed By:", e.getSignedBy()); y -= 18; }
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate Provider Note PDF", ex);
        }
    }

    // Reuse draw for every field
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


    private static void multiline(PDPageContentStream cs, float x, float y, String label, String text) throws IOException {
        if (!StringUtils.hasText(text)) return;
        draw(cs, x, y, label, ""); y -= 14;
        for (String ln : text.split("\\R")) {
            cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x, y); cs.showText(ln); cs.endText();
            y -= 14;
        }
    }

    private ProviderNoteDto toDto(ProviderNote e) {
        ProviderNoteDto d = new ProviderNoteDto();
        d.setId(e.getId());
        d.setOrgId(e.getOrgId());
        d.setPatientId(e.getPatientId());
        d.setEncounterId(e.getEncounterId());

        d.setNoteTitle(e.getNoteTitle());
        d.setNoteTypeCode(e.getNoteTypeCode());
        d.setNoteStatus(e.getNoteStatus());
        d.setNoteDateTime(e.getNoteDateTime() != null ? e.getNoteDateTime().toString() : null);
        d.setAuthorPractitionerId(e.getAuthorPractitionerId());

        d.setSubjective(e.getSubjective());
        d.setObjective(e.getObjective());
        d.setAssessment(e.getAssessment());
        d.setPlan(e.getPlan());
        d.setNarrative(e.getNarrative());
        d.setExternalId(e.getExternalId());

        d.setESigned(e.getESigned());
        d.setSignedAt(e.getSignedAt() != null ? e.getSignedAt().format(ISO) : null);
        d.setSignedBy(e.getSignedBy());
        d.setPrintedAt(e.getPrintedAt() != null ? e.getPrintedAt().format(ISO) : null);

        var a = new ProviderNoteDto.Audit();
        if (e.getCreatedAt() != null) a.setCreatedDate(e.getCreatedAt().toLocalDate().format(DAY));
        if (e.getUpdatedAt() != null) a.setLastModifiedDate(e.getUpdatedAt().toLocalDate().format(DAY));
        d.setAudit(a);

        return d;
    }

    private void applyDto(ProviderNote e, ProviderNoteDto d) {
        e.setNoteTitle(d.getNoteTitle());
        e.setNoteTypeCode(d.getNoteTypeCode());
        e.setNoteStatus(d.getNoteStatus());
        e.setNoteDateTime(parseDateTime(d.getNoteDateTime()));

        e.setAuthorPractitionerId(d.getAuthorPractitionerId());
        e.setSubjective(d.getSubjective());
        e.setObjective(d.getObjective());
        e.setAssessment(d.getAssessment());
        e.setPlan(d.getPlan());
        e.setNarrative(d.getNarrative());
        e.setExternalId(d.getExternalId());
        // eSign fields set only via eSign()
    }

    private static LocalDateTime parseDateTime(String s) {
        if (!StringUtils.hasText(s)) return null;
        try {
            // accept '2025-09-11T08:30' or '2025-09-11T08:30:00'
            return LocalDateTime.parse(s.replace("Z","").trim());
        } catch (Exception ignore) {
            return null;
        }
    }
}
