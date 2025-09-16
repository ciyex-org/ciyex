//package com.qiaben.ciyex.service;
//
//import com.qiaben.ciyex.dto.PhysicalExamDto;
//import com.qiaben.ciyex.entity.PhysicalExam;
//import com.qiaben.ciyex.entity.PhysicalExamSection;
//import com.qiaben.ciyex.repository.PhysicalExamRepository;
//import com.qiaben.ciyex.storage.ExternalPhysicalExamStorage;
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
//public class PhysicalExamService {
//
//    private final PhysicalExamRepository repo;
//    private final Optional<ExternalPhysicalExamStorage> external;
//
//    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//
//    // CREATE
//    public PhysicalExamDto create(Long orgId, Long patientId, Long encounterId, PhysicalExamDto in) {
//        PhysicalExam pe = new PhysicalExam();
//        pe.setOrgId(orgId);
//        pe.setPatientId(patientId);
//        pe.setEncounterId(encounterId);
//
//        if (in.getSections() != null) {
//            for (var s : in.getSections()) {
//                PhysicalExamSection row = PhysicalExamSection.builder()
//                        .sectionKey(normalizeSection(s.getSectionKey()))
//                        .allNormal(Boolean.TRUE.equals(s.getAllNormal()))
//                        .normalText(s.getNormalText())
//                        .findings(s.getFindings())
//                        .physicalExam(pe)
//                        .build();
//                pe.getSections().add(row);
//            }
//        }
//
//        final PhysicalExam saved = repo.save(pe);
//
//        external.ifPresent(ext -> {
//            final PhysicalExam ref = saved;
//            String externalId = ext.create(mapToDto(ref));
//            ref.setExternalId(externalId);
//            repo.save(ref);
//        });
//
//        return mapToDto(saved);
//    }
//
//    // UPDATE (replace sections)
//    public PhysicalExamDto update(Long orgId, Long patientId, Long encounterId, Long id, PhysicalExamDto in) {
//        PhysicalExam pe = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Physical Exam not found"));
//
//        pe.getSections().clear();
//        if (in.getSections() != null) {
//            for (var s : in.getSections()) {
//                PhysicalExamSection row = PhysicalExamSection.builder()
//                        .sectionKey(normalizeSection(s.getSectionKey()))
//                        .allNormal(Boolean.TRUE.equals(s.getAllNormal()))
//                        .normalText(s.getNormalText())
//                        .findings(s.getFindings())
//                        .physicalExam(pe)
//                        .build();
//                pe.getSections().add(row);
//            }
//        }
//
//        final PhysicalExam updated = repo.save(pe);
//
//        external.ifPresent(ext -> {
//            final PhysicalExam ref = updated;
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
//        PhysicalExam pe = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Physical Exam not found"));
//
//        final PhysicalExam toDelete = pe;
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
//    public PhysicalExamDto getOne(Long orgId, Long patientId, Long encounterId, Long id) {
//        PhysicalExam pe = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Physical Exam not found"));
//        return mapToDto(pe);
//    }
//
//    // GET ALL by patient
//    public List<PhysicalExamDto> getAllByPatient(Long orgId, Long patientId) {
//        return repo.findByOrgIdAndPatientId(orgId, patientId).stream().map(this::mapToDto).toList();
//    }
//
//    // GET ALL by patient + encounter
//    public List<PhysicalExamDto> getAllByEncounter(Long orgId, Long patientId, Long encounterId) {
//        return repo.findByOrgIdAndPatientIdAndEncounterId(orgId, patientId, encounterId).stream().map(this::mapToDto).toList();
//    }
//
//    // --- mapping helpers ---
//
//    private PhysicalExamDto mapToDto(PhysicalExam pe) {
//        PhysicalExamDto dto = new PhysicalExamDto();
//        dto.setId(pe.getId());
//        dto.setExternalId(pe.getExternalId());
//        dto.setOrgId(pe.getOrgId());
//        dto.setPatientId(pe.getPatientId());
//        dto.setEncounterId(pe.getEncounterId());
//
//        dto.setSections(
//                pe.getSections().stream().map(s -> {
//                    PhysicalExamDto.SectionDto sd = new PhysicalExamDto.SectionDto();
//                    sd.setSectionKey(s.getSectionKey());
//                    sd.setAllNormal(s.getAllNormal());
//                    sd.setNormalText(s.getNormalText());
//                    sd.setFindings(s.getFindings());
//                    return sd;
//                }).toList()
//        );
//
//        PhysicalExamDto.Audit a = new PhysicalExamDto.Audit();
//        if (pe.getCreatedAt() != null) a.setCreatedDate(pe.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate().toString());
//        if (pe.getUpdatedAt() != null) a.setLastModifiedDate(pe.getUpdatedAt().atZone(ZoneId.systemDefault()).toLocalDate().toString());
//        dto.setAudit(a);
//
//        return dto;
//    }
//
//    private String normalizeSection(String k) {
//        if (k == null) return "OTHER";
//        String v = k.trim().toUpperCase().replace(' ', '_');
//        return switch (v) {
//            case "GENERAL", "HEENT", "NECK", "BREASTS", "CARDIOVASCULAR",
//                 "THORAX_BACK", "GASTROINTESTINAL", "GU_FEMALE", "GU_MALE",
//                 "MUSCULOSKELETAL", "SKIN", "LYMPHATIC", "NEUROLOGIC", "PSYCHIATRIC" -> v;
//            default -> "OTHER";
//        };
//    }
//}



package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.PhysicalExamDto;
import com.qiaben.ciyex.dto.PhysicalExamSectionDto;
import com.qiaben.ciyex.entity.PhysicalExam;
import com.qiaben.ciyex.entity.PhysicalExamSection;
import com.qiaben.ciyex.repository.PhysicalExamRepository;
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
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhysicalExamService {

    private final PhysicalExamRepository repo;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Create
    public PhysicalExamDto create(Long orgId, Long patientId, Long encounterId, PhysicalExamDto dto) {
        PhysicalExam e = new PhysicalExam();
        e.setOrgId(orgId);
        e.setPatientId(patientId);
        e.setEncounterId(encounterId);
        applySections(e, dto.getSections());
        e = repo.save(e);
        return toDto(e);
    }

    // Read
    public PhysicalExamDto getOne(Long orgId, Long patientId, Long encounterId, Long id) {
        PhysicalExam e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Physical exam not found"));
        return toDto(e);
    }

    public List<PhysicalExamDto> list(Long orgId, Long patientId, Long encounterId) {
        return repo.findByOrgIdAndPatientIdAndEncounterId(orgId, patientId, encounterId)
                .stream().map(this::toDto).toList();
    }

    // Update (LOCKED if signed)
    public PhysicalExamDto update(Long orgId, Long patientId, Long encounterId, Long id, PhysicalExamDto dto) {
        PhysicalExam e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Physical exam not found"));
        if (Boolean.TRUE.equals(e.getESigned())) throw new IllegalStateException("Signed physical exams are read-only.");

        e.getSections().clear();
        applySections(e, dto.getSections());
        e = repo.save(e);
        return toDto(e);
    }

    // Delete (BLOCKED if signed)
    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
        PhysicalExam e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Physical exam not found"));
        if (Boolean.TRUE.equals(e.getESigned())) throw new IllegalStateException("Signed physical exams cannot be deleted.");
        repo.delete(e);
    }

    // eSign (idempotent)
    public PhysicalExamDto eSign(Long orgId, Long patientId, Long encounterId, Long id, String signedBy) {
        PhysicalExam e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Physical exam not found"));
        if (Boolean.TRUE.equals(e.getESigned())) return toDto(e);

        e.setESigned(true);
        e.setSignedBy(StringUtils.hasText(signedBy) ? signedBy : "system");
        e.setSignedAt(java.time.OffsetDateTime.now(ZoneOffset.UTC));
        e = repo.save(e);
        return toDto(e);
    }

    // Print (PDF) — stamps printedAt
    public byte[] renderPdf(Long orgId, Long patientId, Long encounterId, Long id) {
        PhysicalExam e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Physical exam not found"));

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
                cs.showText("Physical Examination");
                cs.endText();

                // Meta
                y -= 26;
                draw(cs, x, y, "Patient ID:", String.valueOf(patientId)); y -= 16;
                draw(cs, x, y, "Encounter ID:", String.valueOf(encounterId)); y -= 16;
                draw(cs, x, y, "Record ID:", String.valueOf(id)); y -= 22;

                // Sections
                for (PhysicalExamSection s : e.getSections()) {
                    draw(cs, x, y, "Section:", s.getSectionKey()); y -= 16;
                    draw(cs, x, y, "Status:", Boolean.TRUE.equals(s.getAllNormal()) ? "All normal" : "Abnormal"); y -= 16;
                    if (StringUtils.hasText(s.getNormalText())) { draw(cs, x, y, "Normal Text:", s.getNormalText()); y -= 16; }
                    if (StringUtils.hasText(s.getFindings())) { draw(cs, x, y, "Findings:", s.getFindings()); y -= 16; }
                    y -= 8;
                }

                // eSign footer
                y -= 8;
                draw(cs, x, y, "eSigned:", Boolean.TRUE.equals(e.getESigned()) ? "Yes" : "No"); y -= 16;
                if (e.getSignedAt() != null) { draw(cs, x, y, "Signed At:", e.getSignedAt().toString()); y -= 16; }
                if (StringUtils.hasText(e.getSignedBy())) { draw(cs, x, y, "Signed By:", e.getSignedBy()); y -= 16; }
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate Physical Exam PDF", ex);
        }
    }

    // --- helpers ---
    private void applySections(PhysicalExam e, List<PhysicalExamSectionDto> sections) {
        if (sections == null) return;
        for (PhysicalExamSectionDto d : sections) {
            PhysicalExamSection s = new PhysicalExamSection();
            s.setPhysicalExam(e);
            s.setSectionKey(d.getSectionKey());
            s.setAllNormal(Boolean.TRUE.equals(d.getAllNormal()));
            s.setNormalText(d.getNormalText());
            s.setFindings(d.getFindings());
            e.getSections().add(s);
        }
    }

    private static void draw(PDPageContentStream cs, float x, float y, String label, String value) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 12); cs.newLineAtOffset(x, y); cs.showText(label); cs.endText();
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x + 140, y); cs.showText(value != null ? value : "-"); cs.endText();
    }

    private PhysicalExamDto toDto(PhysicalExam e) {
        PhysicalExamDto d = new PhysicalExamDto();
        d.setId(e.getId());
        d.setExternalId(e.getExternalId());
        d.setOrgId(e.getOrgId());
        d.setPatientId(e.getPatientId());
        d.setEncounterId(e.getEncounterId());

        List<PhysicalExamSectionDto> list = new ArrayList<>();
        for (PhysicalExamSection s : e.getSections()) {
            PhysicalExamSectionDto sd = new PhysicalExamSectionDto();
            sd.setSectionKey(s.getSectionKey());
            sd.setAllNormal(s.getAllNormal());
            sd.setFindings(s.getFindings());
            sd.setNormalText(s.getNormalText());
            list.add(sd);
        }
        d.setSections(list);

        d.setESigned(e.getESigned());
        d.setSignedAt(e.getSignedAt());
        d.setSignedBy(e.getSignedBy());
        d.setPrintedAt(e.getPrintedAt());

        var a = new PhysicalExamDto.Audit();
        if (e.getCreatedAt() != null) a.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
        if (e.getUpdatedAt() != null) a.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
        d.setAudit(a);

        return d;
    }
}
