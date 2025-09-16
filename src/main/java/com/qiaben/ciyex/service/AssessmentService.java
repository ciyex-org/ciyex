//package com.qiaben.ciyex.service;
//
//import com.qiaben.ciyex.dto.AssessmentDto;
//import com.qiaben.ciyex.entity.Assessment;
//import com.qiaben.ciyex.repository.AssessmentRepository;
//import com.qiaben.ciyex.storage.ExternalAssessmentStorage;
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
//public class AssessmentService {
//
//    private final AssessmentRepository repo;
//    private final Optional<ExternalAssessmentStorage> external;
//
//    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//
//    // CREATE
//    public AssessmentDto create(Long orgId, Long patientId, Long encounterId, AssessmentDto in) {
//        Assessment a = Assessment.builder()
//                .orgId(orgId)
//                .patientId(patientId)
//                .encounterId(encounterId)
//                .assessmentSummary(in.getAssessmentSummary())
//                .planSummary(in.getPlanSummary())
//                .notes(in.getNotes())
//                .sectionsJson(in.getSectionsJson())
//                .build();
//
//        final Assessment saved = repo.save(a);
//
//        external.ifPresent(ext -> {
//            final Assessment ref = saved;
//            String externalId = ext.create(mapToDto(ref));
//            ref.setExternalId(externalId);
//            repo.save(ref);
//        });
//
//        return mapToDto(saved);
//    }
//
//    // UPDATE
//    public AssessmentDto update(Long orgId, Long patientId, Long encounterId, Long id, AssessmentDto in) {
//        Assessment a = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Assessment not found"));
//
//        a.setAssessmentSummary(in.getAssessmentSummary());
//        a.setPlanSummary(in.getPlanSummary());
//        a.setNotes(in.getNotes());
//        a.setSectionsJson(in.getSectionsJson());
//
//        final Assessment updated = repo.save(a);
//
//        external.ifPresent(ext -> {
//            final Assessment ref = updated;
//            if (ref.getExternalId() != null) {
//                ext.update(ref.getExternalId(), mapToDto(ref));
//            }
//        });
//
//        return mapToDto(updated);
//    }
//
//    // DELETE
//    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
//        Assessment a = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Assessment not found"));
//
//        final Assessment toDelete = a;
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
//    public AssessmentDto getOne(Long orgId, Long patientId, Long encounterId, Long id) {
//        Assessment a = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Assessment not found"));
//        return mapToDto(a);
//    }
//
//    // GET ALL by patient
//    public List<AssessmentDto> getAllByPatient(Long orgId, Long patientId) {
//        return repo.findByOrgIdAndPatientId(orgId, patientId).stream().map(this::mapToDto).toList();
//    }
//
//    // GET ALL by patient + encounter
//    public List<AssessmentDto> getAllByEncounter(Long orgId, Long patientId, Long encounterId) {
//        return repo.findByOrgIdAndPatientIdAndEncounterId(orgId, patientId, encounterId).stream().map(this::mapToDto).toList();
//    }
//
//
//
//
//    // Mapping
//    private AssessmentDto mapToDto(Assessment e) {
//        AssessmentDto dto = new AssessmentDto();
//        dto.setId(e.getId());
//        dto.setExternalId(e.getExternalId());
//        dto.setOrgId(e.getOrgId());
//        dto.setPatientId(e.getPatientId());
//        dto.setEncounterId(e.getEncounterId());
//        dto.setAssessmentSummary(e.getAssessmentSummary());
//        dto.setPlanSummary(e.getPlanSummary());
//        dto.setNotes(e.getNotes());
//        dto.setSectionsJson(e.getSectionsJson());
//
//        AssessmentDto.Audit a = new AssessmentDto.Audit();
//        if (e.getCreatedAt() != null) {
//            a.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
//        }
//        if (e.getUpdatedAt() != null) {
//            a.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
//        }
//        dto.setAudit(a);
//
//        return dto;
//    }
//}





package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.AssessmentDto;
import com.qiaben.ciyex.entity.Assessment;
import com.qiaben.ciyex.repository.AssessmentRepository;
import com.qiaben.ciyex.storage.ExternalAssessmentStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Autowired;
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
public class AssessmentService {

    private final AssessmentRepository repo;

    @Autowired(required = false)
    private ExternalAssessmentStorage external; // optional

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ----- Create -----
    public AssessmentDto create(Long orgId, Long patientId, Long encounterId, AssessmentDto dto) {
        Assessment e = new Assessment();
        e.setOrgId(orgId);
        e.setPatientId(patientId);
        e.setEncounterId(encounterId);
        applyDto(e, dto);
        e = repo.save(e);

        // optional external
        try {
            if (external != null) {
                String extId = external.create(toDto(e));
                e.setExternalId(extId);
                e = repo.save(e);
            }
        } catch (Exception ex) {
            log.warn("External create failed: {}", ex.getMessage());
        }

        return toDto(e);
    }

    // ----- Read -----
    public AssessmentDto getOne(Long orgId, Long patientId, Long encounterId, Long id) {
        Assessment e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found"));
        return toDto(e);
    }

    public List<AssessmentDto> getAllByEncounter(Long orgId, Long patientId, Long encounterId) {
        return repo.findByOrgIdAndPatientIdAndEncounterId(orgId, patientId, encounterId)
                .stream().map(this::toDto).toList();
    }

    // ----- Update (LOCK if signed) -----
    public AssessmentDto update(Long orgId, Long patientId, Long encounterId, Long id, AssessmentDto dto) {
        Assessment e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found"));

        if (Boolean.TRUE.equals(e.getESigned())) {
            throw new IllegalStateException("Signed assessments are read-only.");
        }

        applyDto(e, dto);
        e = repo.save(e);

        try {
            if (external != null && StringUtils.hasText(e.getExternalId())) {
                external.update(e.getExternalId(), toDto(e));
            }
        } catch (Exception ex) {
            log.warn("External update failed: {}", ex.getMessage());
        }

        return toDto(e);
    }

    // ----- Delete (BLOCK if signed) -----
    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
        Assessment e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found"));

        if (Boolean.TRUE.equals(e.getESigned())) {
            throw new IllegalStateException("Signed assessments cannot be deleted.");
        }

        try {
            if (external != null && StringUtils.hasText(e.getExternalId())) {
                external.delete(e.getExternalId());
            }
        } catch (Exception ex) {
            log.warn("External delete failed: {}", ex.getMessage());
        }

        repo.delete(e);
    }

    // ----- eSign (idempotent) -----
    public AssessmentDto eSign(Long orgId, Long patientId, Long encounterId, Long id, String signedBy) {
        Assessment a = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found"));

        if (Boolean.TRUE.equals(a.getESigned())) {
            return toDto(a);
        }

        a.setESigned(Boolean.TRUE);
        a.setSignedBy(StringUtils.hasText(signedBy) ? signedBy : "system");
        a.setSignedAt(java.time.OffsetDateTime.now(ZoneOffset.UTC));
        a = repo.save(a);
        return toDto(a);
    }

    // ----- Print PDF (also stamps printedAt) -----
    public byte[] renderPdf(Long orgId, Long patientId, Long encounterId, Long id) {
        Assessment a = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found"));

        a.setPrintedAt(java.time.OffsetDateTime.now(ZoneOffset.UTC));
        repo.save(a);

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float x = 64, y = 740;

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.newLineAtOffset(x, y);
                cs.showText("Assessment");
                cs.endText();

                y -= 26;
                draw(cs, x, y, "Patient ID:", String.valueOf(patientId)); y -= 16;
                draw(cs, x, y, "Encounter ID:", String.valueOf(encounterId)); y -= 16;
                draw(cs, x, y, "Assessment ID:", String.valueOf(id)); y -= 22;

                draw(cs, x, y, "Diagnosis Code:", nullTo(a.getDiagnosisCode(), "-")); y -= 16;
                draw(cs, x, y, "Diagnosis Name:", nullTo(a.getDiagnosisName(), "-")); y -= 16;
                draw(cs, x, y, "Status:", nullTo(a.getStatus(), "-")); y -= 16;
                draw(cs, x, y, "Priority:", nullTo(a.getPriority(), "-")); y -= 22;

                draw(cs, x, y, "Assessment / Impression:", nullTo(a.getAssessmentText(), "-")); y -= 22;
                draw(cs, x, y, "Notes:", nullTo(a.getNotes(), "-")); y -= 22;

                draw(cs, x, y, "eSigned:", Boolean.TRUE.equals(a.getESigned()) ? "Yes" : "No"); y -= 16;
                if (a.getSignedAt() != null) { draw(cs, x, y, "Signed At:", a.getSignedAt().toString()); y -= 16; }
                if (StringUtils.hasText(a.getSignedBy())) { draw(cs, x, y, "Signed By:", a.getSignedBy()); y -= 16; }
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Assessment PDF", e);
        }
    }

    // ----- helpers -----
    private static void draw(PDPageContentStream cs, float x, float y, String label, String value) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 12); cs.newLineAtOffset(x, y); cs.showText(label); cs.endText();
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x + 150, y); cs.showText(value != null ? value : "-"); cs.endText();
    }

    private static String nullTo(String v, String fb) { return (v == null || v.isBlank()) ? fb : v; }

    private AssessmentDto toDto(Assessment e) {
        AssessmentDto d = new AssessmentDto();
        d.setId(e.getId());
        d.setExternalId(e.getExternalId());
        d.setOrgId(e.getOrgId());
        d.setPatientId(e.getPatientId());
        d.setEncounterId(e.getEncounterId());

        d.setDiagnosisCode(e.getDiagnosisCode());
        d.setDiagnosisName(e.getDiagnosisName());
        d.setStatus(e.getStatus());
        d.setPriority(e.getPriority());
        d.setAssessmentText(e.getAssessmentText());
        d.setNotes(e.getNotes());

        d.setESigned(e.getESigned());
        d.setSignedAt(e.getSignedAt());
        d.setSignedBy(e.getSignedBy());
        d.setPrintedAt(e.getPrintedAt());

        AssessmentDto.Audit a = new AssessmentDto.Audit();
        if (e.getCreatedAt() != null) a.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
        if (e.getUpdatedAt() != null) a.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
        d.setAudit(a);
        return d;
    }

    private void applyDto(Assessment e, AssessmentDto d) {
        e.setExternalId(d.getExternalId());
        e.setDiagnosisCode(d.getDiagnosisCode());
        e.setDiagnosisName(d.getDiagnosisName());
        e.setStatus(d.getStatus());
        e.setPriority(d.getPriority());
        e.setAssessmentText(d.getAssessmentText());
        e.setNotes(d.getNotes());
        // eSign fields are controlled by eSign()
    }
}
