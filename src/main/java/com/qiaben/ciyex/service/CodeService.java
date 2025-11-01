//package com.qiaben.ciyex.service;
//
//import com.qiaben.ciyex.dto.CodeDto;
//import com.qiaben.ciyex.entity.Code;
//import com.qiaben.ciyex.repository.CodeRepository;
//import com.qiaben.ciyex.storage.ExternalCodeStorage;
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
//public class CodeService {
//
//    private final CodeRepository repo;
//    private final Optional<ExternalCodeStorage> external;
//
//    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//
//    public CodeDto create(Long patientId, Long encounterId, CodeDto in) {
//        Code e = Code.builder()

//                .codeType(in.getCodeType()).code(in.getCode()).modifier(in.getModifier())
//                .active(in.getActive())
//                .description(in.getDescription()).shortDescription(in.getShortDescription())
//                .category(in.getCategory())
//                .diagnosisReporting(in.getDiagnosisReporting())
//                .serviceReporting(in.getServiceReporting())
//                .relateTo(in.getRelateTo())
//                .feeStandard(in.getFeeStandard())
//                .build();
//
//        final Code saved = repo.save(e);
//
//        external.ifPresent(ext -> {
//            final Code ref = saved;
//            String extId = ext.create(mapToDto(ref));
//            ref.setExternalId(extId);
//            repo.save(ref);
//        });
//
//        return mapToDto(saved);
//    }
//
//    public CodeDto update(Long patientId, Long encounterId, Long id, CodeDto in) {
//        Code e = repo.findByPatientIdAndEncounterId(patientId, encounterId).stream()
//                .filter(c -> c.getId().equals(id))
//                .findFirst()
//                .orElseThrow(() -> new IllegalArgumentException("Code not found in this encounter"));
//
//        e.setCodeType(in.getCodeType());
//        e.setCode(in.getCode());
//        e.setModifier(in.getModifier());
//        e.setActive(in.getActive());
//        e.setDescription(in.getDescription());
//        e.setShortDescription(in.getShortDescription());
//        e.setCategory(in.getCategory());
//        e.setDiagnosisReporting(in.getDiagnosisReporting());
//        e.setServiceReporting(in.getServiceReporting());
//        e.setRelateTo(in.getRelateTo());
//        e.setFeeStandard(in.getFeeStandard());
//
//        final Code updated = repo.save(e);
//
//        external.ifPresent(ext -> {
//            final Code ref = updated;
//            if (ref.getExternalId() != null) ext.update(ref.getExternalId(), mapToDto(ref));
//        });
//
//        return mapToDto(updated);
//    }
//
//    public void delete(Long patientId, Long encounterId, Long id) {
//        Code e = repo.findByPatientIdAndEncounterId(patientId, encounterId).stream()
//                .filter(c -> c.getId().equals(id))
//                .findFirst().orElseThrow(() -> new IllegalArgumentException("Code not found"));
//        external.ifPresent(ext -> { if (e.getExternalId() != null) ext.delete(e.getExternalId()); });
//        repo.delete(e);
//    }
//
//    public CodeDto getOne(Long patientId, Long encounterId, Long id) {
//        return repo.findByPatientIdAndEncounterId(patientId, encounterId).stream()
//                .filter(c -> c.getId().equals(id))
//                .findFirst()
//                .map(this::mapToDto)
//                .orElseThrow(() -> new IllegalArgumentException("Code not found"));
//    }
//
//    public List<CodeDto> getAllByPatient(Long patientId) {
//        return repo.findByPatientId(patientId).stream().map(this::mapToDto).toList();
//    }
//
//    public List<CodeDto> getAllByEncounter(Long patientId, Long encounterId) {
//        return repo.findByPatientIdAndEncounterId(patientId, encounterId)
//                .stream().map(this::mapToDto).toList();
//    }
//
//    public List<CodeDto> searchInEncounter(Long patientId, Long encounterId,
//                                           String codeType, Boolean active, String q) {
//        return repo.searchInEncounter(patientId, encounterId, codeType, active, q)
//                .stream().map(this::mapToDto).toList();
//    }
//
//    private CodeDto mapToDto(Code e) {
//        CodeDto dto = new CodeDto();
//        dto.setId(e.getId());
//        dto.setExternalId(e.getExternalId());
//        dto.setOrgId(e.getOrgId());
//        dto.setPatientId(e.getPatientId());
//        dto.setEncounterId(e.getEncounterId());
//        dto.setCodeType(e.getCodeType());
//        dto.setCode(e.getCode());
//        dto.setModifier(e.getModifier());
//        dto.setActive(e.getActive());
//        dto.setDescription(e.getDescription());
//        dto.setShortDescription(e.getShortDescription());
//        dto.setCategory(e.getCategory());
//        dto.setDiagnosisReporting(e.getDiagnosisReporting());
//        dto.setServiceReporting(e.getServiceReporting());
//        dto.setRelateTo(e.getRelateTo());
//        dto.setFeeStandard(e.getFeeStandard());
//
//        CodeDto.Audit a = new CodeDto.Audit();
//        if (e.getCreatedAt() != null) a.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
//        if (e.getUpdatedAt() != null) a.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
//        dto.setAudit(a);
//        return dto;
//    }
//}




package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.CodeDto;
import com.qiaben.ciyex.dto.CodeTypeDto;
import com.qiaben.ciyex.entity.Code;
import com.qiaben.ciyex.repository.CodeRepository;
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
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeService {

    private final CodeRepository repo;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    // CREATE
    public CodeDto create(Long patientId, Long encounterId, CodeDto dto) {
        Code e = new Code();
        e.setPatientId(patientId);
        e.setEncounterId(encounterId);
        applyDto(e, dto);
        e = repo.save(e);
        return toDto(e);
    }
    // GET ALL by patient
    public List<CodeDto> getAllByPatient(Long patientId) {
        return repo.findByPatientId(patientId)
                .stream().map(this::toDto).toList();
    }


    // LIST
    public List<CodeDto> list(Long patientId, Long encounterId) {
        return repo.findByPatientIdAndEncounterId(patientId, encounterId)
                .stream().map(this::toDto).toList();
    }

    // GET ONE
    public CodeDto getOne(Long patientId, Long encounterId, Long id) {
        Code e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Code not found"));
        return toDto(e);
    }

    // UPDATE (blocked if signed)
    public CodeDto update(Long patientId, Long encounterId, Long id, CodeDto dto) {
        Code e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Code not found"));
        if (Boolean.TRUE.equals(e.getESigned())) {
            throw new IllegalStateException("Signed codes are read-only.");
        }
        applyDto(e, dto);
        e = repo.save(e);
        return toDto(e);
    }

    // DELETE (blocked if signed)
    public void delete(Long patientId, Long encounterId, Long id) {
        Code e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Code not found"));
        if (Boolean.TRUE.equals(e.getESigned())) {
            throw new IllegalStateException("Signed codes cannot be deleted.");
        }
        repo.delete(e);
    }

    // ESIGN (idempotent, no request body)
    public CodeDto eSign(Long patientId, Long encounterId, Long id, String signedBy) {
        Code e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Code not found"));
        if (Boolean.TRUE.equals(e.getESigned())) return toDto(e);

        e.setESigned(true);
        e.setSignedBy(StringUtils.hasText(signedBy) ? signedBy : "system");
        e.setSignedAt(OffsetDateTime.now(ZoneOffset.UTC));
        e = repo.save(e);
        return toDto(e);
    }

    // PRINT (PDF) — stamps printedAt
    public byte[] renderPdf(Long patientId, Long encounterId, Long id) {
        Code e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Code not found"));

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
                cs.showText("Billing Code");
                cs.endText();

                y -= 26;
                draw(cs, x, y, "Patient ID:", String.valueOf(patientId)); y -= 16;
                draw(cs, x, y, "Encounter ID:", String.valueOf(encounterId)); y -= 16;
                draw(cs, x, y, "Code ID:", String.valueOf(id)); y -= 20;

                draw(cs, x, y, "Type:", orDash(e.getCodeType())); y -= 16;
                draw(cs, x, y, "Code:", orDash(e.getCode()) + (StringUtils.hasText(e.getModifier()) ? "-" + e.getModifier() : "")); y -= 16;
                draw(cs, x, y, "Status:", Boolean.TRUE.equals(e.getActive()) ? "Active" : "Inactive"); y -= 16;

                if (e.getFeeStandard() != null) { draw(cs, x, y, "Fee (Std):", e.getFeeStandard().toPlainString()); y -= 16; }
                if (StringUtils.hasText(e.getCategory())) { draw(cs, x, y, "Category:", e.getCategory()); y -= 16; }
                if (StringUtils.hasText(e.getRelateTo())) { draw(cs, x, y, "Relate To:", e.getRelateTo()); y -= 16; }

                String rep = (Boolean.TRUE.equals(e.getDiagnosisReporting()) ? "Dx" : "")
                        + ((Boolean.TRUE.equals(e.getDiagnosisReporting()) && Boolean.TRUE.equals(e.getServiceReporting())) ? " · " : "")
                        + (Boolean.TRUE.equals(e.getServiceReporting()) ? "Service" : "");
                if (StringUtils.hasText(rep)) { draw(cs, x, y, "Reporting:", rep); y -= 16; }

                if (StringUtils.hasText(e.getShortDescription())) {
                    draw(cs, x, y, "Short Description:", ""); y -= 14;
                    textBlock(cs, x + 16, y, e.getShortDescription()); y -= 4;
                }
                if (StringUtils.hasText(e.getDescription())) {
                    y -= 8;
                    draw(cs, x, y, "Description:", ""); y -= 14;
                    textBlock(cs, x + 16, y, e.getDescription()); y -= 4;
                }

                y -= 10;
                draw(cs, x, y, "eSigned:", Boolean.TRUE.equals(e.getESigned()) ? "Yes" : "No"); y -= 16;
                if (e.getSignedAt() != null) { draw(cs, x, y, "Signed At:", e.getSignedAt().format(ISO)); y -= 16; }
                if (StringUtils.hasText(e.getSignedBy())) { draw(cs, x, y, "Signed By:", e.getSignedBy()); y -= 16; }

                y -= 10;
                if (e.getCreatedAt() != null) { draw(cs, x, y, "Created:", e.getCreatedAt().format(DAY)); y -= 16; }
                if (e.getUpdatedAt() != null) { draw(cs, x, y, "Updated:", e.getUpdatedAt().format(DAY)); y -= 16; }
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate Code PDF", ex);
        }
    }

    // --- helpers
    private static String orDash(String s) { return StringUtils.hasText(s) ? s : "-"; }

    private static void draw(PDPageContentStream cs, float x, float y, String label, String value) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 12); cs.newLineAtOffset(x, y); cs.showText(label); cs.endText();
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x + 140, y); cs.showText(value != null ? value : "-"); cs.endText();
    }

    private static void textBlock(PDPageContentStream cs, float x, float startY, String text) throws IOException {
        float y = startY;
        for (String ln : text.split("\\R")) {
            cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x, y); cs.showText(ln); cs.endText();
            y -= 14;
        }
    }

    private CodeDto toDto(Code e) {
        CodeDto d = new CodeDto();
        d.setId(e.getId());
        d.setExternalId(e.getExternalId());
        d.setPatientId(e.getPatientId());
        d.setEncounterId(e.getEncounterId());

        d.setCodeType(e.getCodeType());
        d.setCode(e.getCode());
        d.setModifier(e.getModifier());
        d.setActive(e.getActive());
        d.setDescription(e.getDescription());
        d.setShortDescription(e.getShortDescription());
        d.setCategory(e.getCategory());
        d.setDiagnosisReporting(e.getDiagnosisReporting());
        d.setServiceReporting(e.getServiceReporting());
        d.setRelateTo(e.getRelateTo());
        d.setFeeStandard(e.getFeeStandard() != null ? e.getFeeStandard().doubleValue() : null);

        d.setESigned(e.getESigned());
        d.setSignedAt(e.getSignedAt() != null ? e.getSignedAt().format(ISO) : null);
        d.setSignedBy(e.getSignedBy());
        d.setPrintedAt(e.getPrintedAt() != null ? e.getPrintedAt().format(ISO) : null);

        CodeDto.Audit a = new CodeDto.Audit();
        if (e.getCreatedAt() != null) a.setCreatedDate(e.getCreatedAt().toLocalDate().format(DAY));
        if (e.getUpdatedAt() != null) a.setLastModifiedDate(e.getUpdatedAt().toLocalDate().format(DAY));
        d.setAudit(a);

        return d;
    }

    private void applyDto(Code e, CodeDto d) {
        e.setExternalId(d.getExternalId());
        e.setCodeType(d.getCodeType());
        e.setCode(d.getCode());
        e.setModifier(d.getModifier());
        e.setActive(d.getActive());
        e.setDescription(d.getDescription());
        e.setShortDescription(d.getShortDescription());
        e.setCategory(d.getCategory());
        e.setDiagnosisReporting(d.getDiagnosisReporting());
        e.setServiceReporting(d.getServiceReporting());
        e.setRelateTo(d.getRelateTo());
        e.setFeeStandard(d.getFeeStandard() != null ? BigDecimal.valueOf(d.getFeeStandard()) : null);
        // eSign fields managed only via eSign()
    }
}
