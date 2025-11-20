//package com.qiaben.ciyex.service;
//
//import com.qiaben.ciyex.dto.ReviewOfSystemDto;
//import com.qiaben.ciyex.entity.ReviewOfSystem;
//import com.qiaben.ciyex.repository.ReviewOfSystemRepository;
//import com.qiaben.ciyex.storage.ExternalReviewOfSystemStorage;
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
//public class ReviewOfSystemService {
//
//    private final ReviewOfSystemRepository repo;
//    private final Optional<ExternalReviewOfSystemStorage> external;
//
//    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//
//    // CREATE
//    public ReviewOfSystemDto create(Long patientId, Long encounterId, ReviewOfSystemDto in) {
//        ReviewOfSystem e = ReviewOfSystem.builder()

//                .systemName(in.getSystemName())
//                .isNegative(in.getIsNegative())
//                .notes(in.getNotes())
//                .systemDetails(in.getSystemDetails() == null ? List.of() : in.getSystemDetails())
//                .build();
//
//        final ReviewOfSystem saved = repo.save(e);
//
//        external.ifPresent(ext -> {
//            final ReviewOfSystem ref = saved;
//            String extId = ext.create(mapToDto(ref));
//            ref.setExternalId(extId);
//            repo.save(ref);
//        });
//
//        return mapToDto(saved);
//    }
//
//    // UPDATE
//    public ReviewOfSystemDto update(Long patientId, Long encounterId, Long id, ReviewOfSystemDto in) {
//        ReviewOfSystem e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("ROS not found"));
//
//        e.setSystemName(in.getSystemName());
//        e.setIsNegative(in.getIsNegative());
//        e.setNotes(in.getNotes());
//        e.setSystemDetails(in.getSystemDetails() == null ? List.of() : in.getSystemDetails());
//
//        final ReviewOfSystem updated = repo.save(e);
//
//        external.ifPresent(ext -> {
//            final ReviewOfSystem ref = updated;
//            if (ref.getExternalId() != null) ext.update(ref.getExternalId(), mapToDto(ref));
//        });
//
//        return mapToDto(updated);
//    }
//
//    // DELETE
//    public void delete(Long patientId, Long encounterId, Long id) {
//        ReviewOfSystem e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("ROS not found"));
//
//        external.ifPresent(ext -> { if (e.getExternalId() != null) ext.delete(e.getExternalId()); });
//        repo.delete(e);
//    }
//
//    // GET ONE
//    public ReviewOfSystemDto getOne(Long patientId, Long encounterId, Long id) {
//        ReviewOfSystem e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("ROS not found"));
//        return mapToDto(e);
//    }
//
//    // LISTS
//    public List<ReviewOfSystemDto> getAllByPatient(Long patientId) {
//        return repo.findByPatientId(patientId).stream().map(this::mapToDto).toList();
//    }
//
//    public List<ReviewOfSystemDto> getAllByEncounter(Long patientId, Long encounterId) {
//        return repo.findByPatientIdAndEncounterId(patientId, encounterId).stream().map(this::mapToDto).toList();
//    }
//
//    // MAPPING
//    private ReviewOfSystemDto mapToDto(ReviewOfSystem e) {
//        ReviewOfSystemDto dto = new ReviewOfSystemDto();
//        dto.setId(e.getId());
//        dto.setExternalId(e.getExternalId());
//        dto.setOrgId(e.getOrgId());
//        dto.setPatientId(e.getPatientId());
//        dto.setEncounterId(e.getEncounterId());
//        dto.setSystemName(e.getSystemName());
//        dto.setIsNegative(e.getIsNegative());
//        dto.setNotes(e.getNotes());
//        dto.setSystemDetails(e.getSystemDetails());
//
//        if (e.getCreatedAt() != null)
//            dto.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
//        if (e.getUpdatedAt() != null)
//            dto.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
//
//        return dto;
//    }
//}




package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ReviewOfSystemDto;
import com.qiaben.ciyex.entity.ReviewOfSystem;
import com.qiaben.ciyex.repository.ReviewOfSystemRepository;
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
public class ReviewOfSystemService {
    public List<ReviewOfSystemDto> getAllByPatient(Long patientId) {
        return repo.findByPatientId(patientId)
            .stream().map(this::toDto).toList();
    }

    private final ReviewOfSystemRepository repo;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // CREATE
    public ReviewOfSystemDto create(Long patientId, Long encounterId, ReviewOfSystemDto dto) {
        ReviewOfSystem e = new ReviewOfSystem();
        e.setPatientId(patientId);
        e.setEncounterId(encounterId);
        applyDto(e, dto);
        e = repo.save(e);
        return toDto(e);
    }

    // GET ONE
    public ReviewOfSystemDto getOne(Long patientId, Long encounterId, Long id) {
        ReviewOfSystem e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Review of System not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id)
                ));
        return toDto(e);
    }

    // LIST
    public List<ReviewOfSystemDto> list(Long patientId, Long encounterId) {
        return repo.findByPatientIdAndEncounterId(patientId, encounterId)
                .stream().map(this::toDto).toList();
    }

    // UPDATE (locked if signed)
    public ReviewOfSystemDto update(Long patientId, Long encounterId, Long id, ReviewOfSystemDto dto) {
        ReviewOfSystem e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Review of System not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id)
                ));
        if (Boolean.TRUE.equals(e.getESigned())) {
            throw new IllegalStateException("Signed ROS entries are read-only.");
        }
        applyDto(e, dto);
        e = repo.save(e);
        return toDto(e);
    }

    // DELETE (locked if signed)
    public void delete(Long patientId, Long encounterId, Long id) {
        ReviewOfSystem e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Review of System not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id)
                ));
        if (Boolean.TRUE.equals(e.getESigned())) {
            throw new IllegalStateException("Signed ROS entries cannot be deleted.");
        }
        repo.delete(e);
    }

    // eSIGN (idempotent)
    public ReviewOfSystemDto eSign(Long patientId, Long encounterId, Long id, String signedBy) {
        ReviewOfSystem e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Review of System not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id)
                ));
        if (Boolean.TRUE.equals(e.getESigned())) return toDto(e);

        e.setESigned(true);
        e.setSignedBy(StringUtils.hasText(signedBy) ? signedBy : "system");
        e.setSignedAt(java.time.OffsetDateTime.now(ZoneOffset.UTC));
        e = repo.save(e);
        return toDto(e);
    }

    // PRINT (PDF) — also stamps printedAt
    public byte[] renderPdf(Long patientId, Long encounterId, Long id) {
        ReviewOfSystem e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Review of System not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id)
                ));

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
                cs.showText("Review of Systems");
                cs.endText();

                // Meta
                y -= 26;
                draw(cs, x, y, "Patient ID:", String.valueOf(patientId)); y -= 16;
                draw(cs, x, y, "Encounter ID:", String.valueOf(encounterId)); y -= 16;
                draw(cs, x, y, "ROS ID:", String.valueOf(id)); y -= 16;

                // Content
                y -= 10;
                draw(cs, x, y, "System:", e.getSystemName()); y -= 16;
                draw(cs, x, y, "Status:", Boolean.TRUE.equals(e.getIsNegative()) ? "Negative" : "Positive"); y -= 18;

                if (e.getSystemDetails() != null && !e.getSystemDetails().isEmpty()) {
                    draw(cs, x, y, "Findings:", ""); y -= 14;
                    for (String d : e.getSystemDetails()) {
                        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x + 16, y);
                        cs.showText("- " + d); cs.endText();
                        y -= 14;
                    }
                    y -= 8;
                }

                if (StringUtils.hasText(e.getNotes())) {
                    draw(cs, x, y, "Notes:", ""); y -= 14;
                    for (String ln : e.getNotes().split("\\R")) {
                        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x + 16, y);
                        cs.showText(ln); cs.endText();
                        y -= 14;
                    }
                }

                // eSign footer
                y -= 10;
                draw(cs, x, y, "eSigned:", Boolean.TRUE.equals(e.getESigned()) ? "Yes" : "No"); y -= 16;
                if (e.getSignedAt() != null) { draw(cs, x, y, "Signed At:", e.getSignedAt().toString()); y -= 16; }
                if (StringUtils.hasText(e.getSignedBy())) { draw(cs, x, y, "Signed By:", e.getSignedBy()); y -= 16; }
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate ROS PDF", ex);
        }
    }

    // ----- helpers
    private static void draw(PDPageContentStream cs, float x, float y, String label, String value) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 12); cs.newLineAtOffset(x, y); cs.showText(label); cs.endText();
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x + 140, y); cs.showText(value != null ? value : "-"); cs.endText();
    }

    private ReviewOfSystemDto toDto(ReviewOfSystem e) {
        ReviewOfSystemDto d = new ReviewOfSystemDto();
        d.setId(e.getId());
        d.setPatientId(e.getPatientId());
        d.setEncounterId(e.getEncounterId());
        d.setSystemName(e.getSystemName());
        d.setIsNegative(e.getIsNegative());
        d.setNotes(e.getNotes());
        d.setSystemDetails(new ArrayList<>(e.getSystemDetails()));

        d.setESigned(e.getESigned());
        d.setSignedAt(e.getSignedAt());
        d.setSignedBy(e.getSignedBy());
        d.setPrintedAt(e.getPrintedAt());

        var a = new ReviewOfSystemDto.Audit();
        if (e.getCreatedAt() != null)
            a.setCreatedDate(DAY.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
        if (e.getUpdatedAt() != null)
            a.setLastModifiedDate(DAY.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
        d.setAudit(a);
        return d;
    }

    private void applyDto(ReviewOfSystem e, ReviewOfSystemDto d) {
        e.setSystemName(d.getSystemName());
        e.setIsNegative(d.getIsNegative());
        e.setNotes(d.getNotes());
        e.getSystemDetails().clear();
        if (d.getSystemDetails() != null) e.getSystemDetails().addAll(d.getSystemDetails());
        // eSign fields managed only via eSign()
    }
}
