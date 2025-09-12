//package com.qiaben.ciyex.service;
//
//import com.qiaben.ciyex.dto.DateTimeFinalizedDto;
//import com.qiaben.ciyex.entity.DateTimeFinalized;
//import com.qiaben.ciyex.repository.DateTimeFinalizedRepository;
//import com.qiaben.ciyex.storage.ExternalDateTimeFinalizedStorage;
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
//public class DateTimeFinalizedService {
//
//    private final DateTimeFinalizedRepository repo;
//    private final Optional<ExternalDateTimeFinalizedStorage> external;
//
//    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//
//    public DateTimeFinalizedDto create(Long orgId, Long patientId, Long encounterId, DateTimeFinalizedDto in) {
//        DateTimeFinalized e = DateTimeFinalized.builder()
//                .orgId(orgId).patientId(patientId).encounterId(encounterId)
//                .targetType(in.getTargetType())
//                .targetId(in.getTargetId())
//                .targetVersion(in.getTargetVersion())
//                .finalizedAt(in.getFinalizedAt())
//                .finalizedBy(in.getFinalizedBy())
//                .finalizerRole(in.getFinalizerRole())
//                .method(in.getMethod())
//                .status(in.getStatus())
//                .reason(in.getReason())
//                .comments(in.getComments())
//                .contentHash(in.getContentHash())
//                .providerSignatureId(in.getProviderSignatureId())
//                .signoffId(in.getSignoffId())
//                .build();
//
//        final DateTimeFinalized saved = repo.save(e);
//
//        external.ifPresent(ext -> {
//            final DateTimeFinalized ref = saved;
//            String extId = ext.create(mapToDto(ref));
//            ref.setExternalId(extId);
//            repo.save(ref);
//        });
//
//        return mapToDto(saved);
//    }
//
//    public DateTimeFinalizedDto update(Long orgId, Long patientId, Long encounterId, Long id, DateTimeFinalizedDto in) {
//        DateTimeFinalized e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Finalization record not found"));
//
//        e.setTargetType(in.getTargetType());
//        e.setTargetId(in.getTargetId());
//        e.setTargetVersion(in.getTargetVersion());
//        e.setFinalizedAt(in.getFinalizedAt());
//        e.setFinalizedBy(in.getFinalizedBy());
//        e.setFinalizerRole(in.getFinalizerRole());
//        e.setMethod(in.getMethod());
//        e.setStatus(in.getStatus());
//        e.setReason(in.getReason());
//        e.setComments(in.getComments());
//        e.setContentHash(in.getContentHash());
//        e.setProviderSignatureId(in.getProviderSignatureId());
//        e.setSignoffId(in.getSignoffId());
//
//        final DateTimeFinalized updated = repo.save(e);
//
//        external.ifPresent(ext -> {
//            final DateTimeFinalized ref = updated;
//            if (ref.getExternalId() != null) {
//                ext.update(ref.getExternalId(), mapToDto(ref));
//            }
//        });
//
//        return mapToDto(updated);
//    }
//
//    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
//        DateTimeFinalized e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Finalization record not found"));
//
//        external.ifPresent(ext -> {
//            if (e.getExternalId() != null) ext.delete(e.getExternalId());
//        });
//
//        repo.delete(e);
//    }
//
//    public DateTimeFinalizedDto getOne(Long orgId, Long patientId, Long encounterId, Long id) {
//        DateTimeFinalized e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Finalization record not found"));
//        return mapToDto(e);
//    }
//
//    public List<DateTimeFinalizedDto> getAllByPatient(Long orgId, Long patientId) {
//        return repo.findByOrgIdAndPatientId(orgId, patientId).stream().map(this::mapToDto).toList();
//    }
//
//    public List<DateTimeFinalizedDto> getAllByEncounter(Long orgId, Long patientId, Long encounterId) {
//        return repo.findByOrgIdAndPatientIdAndEncounterId(orgId, patientId, encounterId).stream().map(this::mapToDto).toList();
//    }
//
//    private DateTimeFinalizedDto mapToDto(DateTimeFinalized e) {
//        DateTimeFinalizedDto dto = new DateTimeFinalizedDto();
//        dto.setId(e.getId());
//        dto.setExternalId(e.getExternalId());
//        dto.setOrgId(e.getOrgId());
//        dto.setPatientId(e.getPatientId());
//        dto.setEncounterId(e.getEncounterId());
//        dto.setTargetType(e.getTargetType());
//        dto.setTargetId(e.getTargetId());
//        dto.setTargetVersion(e.getTargetVersion());
//        dto.setFinalizedAt(e.getFinalizedAt());
//        dto.setFinalizedBy(e.getFinalizedBy());
//        dto.setFinalizerRole(e.getFinalizerRole());
//        dto.setMethod(e.getMethod());
//        dto.setStatus(e.getStatus());
//        dto.setReason(e.getReason());
//        dto.setComments(e.getComments());
//        dto.setContentHash(e.getContentHash());
//        dto.setProviderSignatureId(e.getProviderSignatureId());
//        dto.setSignoffId(e.getSignoffId());
//
//        DateTimeFinalizedDto.Audit a = new DateTimeFinalizedDto.Audit();
//        if (e.getCreatedAt() != null) a.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
//        if (e.getUpdatedAt() != null) a.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
//        dto.setAudit(a);
//        return dto;
//    }
//}


package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.DateTimeFinalizedDto;
import com.qiaben.ciyex.entity.DateTimeFinalized;
import com.qiaben.ciyex.repository.DateTimeFinalizedRepository;
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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DateTimeFinalizedService {

    private final DateTimeFinalizedRepository repo;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Create
    public DateTimeFinalizedDto create(Long orgId, Long patientId, Long encounterId, DateTimeFinalizedDto dto) {
        DateTimeFinalized e = new DateTimeFinalized();
        e.setOrgId(orgId);
        e.setPatientId(patientId);
        e.setEncounterId(encounterId);
        applyDto(e, dto);
        e = repo.save(e);
        return toDto(e);
    }

    // Read
    public DateTimeFinalizedDto getOne(Long orgId, Long patientId, Long encounterId, Long id) {
        DateTimeFinalized e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Finalization record not found"));
        return toDto(e);
    }

    public List<DateTimeFinalizedDto> list(Long orgId, Long patientId, Long encounterId) {
        return repo.findByOrgIdAndPatientIdAndEncounterId(orgId, patientId, encounterId)
                .stream().map(this::toDto).toList();
    }

    // Update (LOCKED if eSigned)
    public DateTimeFinalizedDto update(Long orgId, Long patientId, Long encounterId, Long id, DateTimeFinalizedDto dto) {
        DateTimeFinalized e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Finalization record not found"));

        if (Boolean.TRUE.equals(e.getESigned())) {
            throw new IllegalStateException("Signed finalizations are read-only.");
        }

        applyDto(e, dto);
        e = repo.save(e);
        return toDto(e);
    }

    // Delete (BLOCKED if eSigned)
    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
        DateTimeFinalized e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Finalization record not found"));

        if (Boolean.TRUE.equals(e.getESigned())) {
            throw new IllegalStateException("Signed finalizations cannot be deleted.");
        }
        repo.delete(e);
    }

    // eSign (idempotent)
    public DateTimeFinalizedDto eSign(Long orgId, Long patientId, Long encounterId, Long id, String signedBy) {
        DateTimeFinalized e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Finalization record not found"));

        if (Boolean.TRUE.equals(e.getESigned())) {
            return toDto(e);
        }

        e.setESigned(Boolean.TRUE);
        e.setSignedBy(StringUtils.hasText(signedBy) ? signedBy : "system");
        e.setSignedAt(java.time.OffsetDateTime.now(ZoneOffset.UTC));

        // optional: if not set, mark status as finalized on eSign
        if (!StringUtils.hasText(e.getStatus())) e.setStatus("finalized");

        e = repo.save(e);
        return toDto(e);
    }

    // Print (PDF) — stamps printedAt
    public byte[] renderPdf(Long orgId, Long patientId, Long encounterId, Long id) {
        DateTimeFinalized e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Finalization record not found"));

        e.setPrintedAt(java.time.OffsetDateTime.now(ZoneOffset.UTC));
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
                cs.showText("Date/Time Finalized");
                cs.endText();

                // Meta
                y -= 26;
                draw(cs, x, y, "Patient ID:", String.valueOf(patientId)); y -= 16;
                draw(cs, x, y, "Encounter ID:", String.valueOf(encounterId)); y -= 16;
                draw(cs, x, y, "Record ID:", String.valueOf(id)); y -= 22;

                // Content
                draw(cs, x, y, "Status:", nullTo(e.getStatus(), "finalized")); y -= 16;
                draw(cs, x, y, "Finalized At:", nullTo(e.getFinalizedAt(), "—")); y -= 16;
                draw(cs, x, y, "Finalized By:", nullTo(e.getFinalizedBy(), "—")); y -= 16;
                draw(cs, x, y, "Role:", nullTo(e.getFinalizerRole(), "—")); y -= 16;
                draw(cs, x, y, "Method:", nullTo(e.getMethod(), "—")); y -= 16;
                draw(cs, x, y, "Target:", (nullTo(e.getTargetType(), "—")
                        + (e.getTargetId() != null ? " · " + e.getTargetId() : "")
                        + (StringUtils.hasText(e.getTargetVersion()) ? " · v" + e.getTargetVersion() : ""))); y -= 16;
                if (StringUtils.hasText(e.getReason())) { draw(cs, x, y, "Reason:", e.getReason()); y -= 16; }
                if (StringUtils.hasText(e.getComments())) { draw(cs, x, y, "Notes:", e.getComments()); y -= 16; }
                if (StringUtils.hasText(e.getContentHash())) { draw(cs, x, y, "Content Hash:", e.getContentHash()); y -= 16; }
                if (e.getProviderSignatureId() != null) { draw(cs, x, y, "Provider Signature ID:", String.valueOf(e.getProviderSignatureId())); y -= 16; }
                if (e.getSignoffId() != null) { draw(cs, x, y, "Sign-off ID:", String.valueOf(e.getSignoffId())); y -= 16; }

                // eSign footer
                draw(cs, x, y, "eSigned:", Boolean.TRUE.equals(e.getESigned()) ? "Yes" : "No"); y -= 16;
                if (e.getSignedAt() != null) { draw(cs, x, y, "Signed At:", e.getSignedAt().toString()); y -= 16; }
                if (StringUtils.hasText(e.getSignedBy())) { draw(cs, x, y, "Signed By:", e.getSignedBy()); y -= 16; }
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate Date/Time Finalized PDF", ex);
        }
    }

    // ---- helpers ----
    private static void draw(PDPageContentStream cs, float x, float y, String label, String value) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 12); cs.newLineAtOffset(x, y); cs.showText(label); cs.endText();
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x + 160, y); cs.showText(value != null ? value : "-"); cs.endText();
    }

    private static String nullTo(String v, String fb) { return (v == null || v.isBlank()) ? fb : v; }

    private DateTimeFinalizedDto toDto(DateTimeFinalized e) {
        DateTimeFinalizedDto d = new DateTimeFinalizedDto();
        d.setId(e.getId());
        d.setExternalId(e.getExternalId());
        d.setOrgId(e.getOrgId());
        d.setPatientId(e.getPatientId());
        d.setEncounterId(e.getEncounterId());
        d.setTargetType(e.getTargetType());
        d.setTargetId(e.getTargetId());
        d.setTargetVersion(e.getTargetVersion());
        d.setFinalizedAt(e.getFinalizedAt());
        d.setFinalizedBy(e.getFinalizedBy());
        d.setFinalizerRole(e.getFinalizerRole());
        d.setMethod(e.getMethod());
        d.setStatus(e.getStatus());
        d.setReason(e.getReason());
        d.setComments(e.getComments());
        d.setContentHash(e.getContentHash());
        d.setProviderSignatureId(e.getProviderSignatureId());
        d.setSignoffId(e.getSignoffId());
        d.setESigned(e.getESigned());
        d.setSignedAt(e.getSignedAt());
        d.setSignedBy(e.getSignedBy());
        d.setPrintedAt(e.getPrintedAt());

        DateTimeFinalizedDto.Audit a = new DateTimeFinalizedDto.Audit();
        if (e.getCreatedAt() != null) a.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
        if (e.getUpdatedAt() != null) a.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
        d.setAudit(a);
        return d;
    }

    private void applyDto(DateTimeFinalized e, DateTimeFinalizedDto d) {
        e.setExternalId(d.getExternalId());
        e.setTargetType(d.getTargetType());
        e.setTargetId(d.getTargetId());
        e.setTargetVersion(d.getTargetVersion());
        e.setFinalizedAt(d.getFinalizedAt());
        e.setFinalizedBy(d.getFinalizedBy());
        e.setFinalizerRole(d.getFinalizerRole());
        e.setMethod(d.getMethod());
        e.setStatus(d.getStatus());
        e.setReason(d.getReason());
        e.setComments(d.getComments());
        e.setContentHash(d.getContentHash());
        e.setProviderSignatureId(d.getProviderSignatureId());
        e.setSignoffId(d.getSignoffId());
        // eSign fields are managed by eSign()
    }
}
