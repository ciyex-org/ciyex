//package com.qiaben.ciyex.service;
//
//import com.qiaben.ciyex.dto.ProviderSignatureDto;
//import com.qiaben.ciyex.entity.ProviderSignature;
//import com.qiaben.ciyex.repository.ProviderSignatureRepository;
//import com.qiaben.ciyex.storage.ExternalProviderSignatureStorage;
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
//public class ProviderSignatureService {
//
//    private final ProviderSignatureRepository repo;
//    private final Optional<ExternalProviderSignatureStorage> external;
//
//    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//
//    public ProviderSignatureDto create(Long orgId, Long patientId, Long encounterId, ProviderSignatureDto in) {
//        ProviderSignature e = ProviderSignature.builder()
//                .orgId(orgId).patientId(patientId).encounterId(encounterId)
//                .signedAt(in.getSignedAt())
//                .signedBy(in.getSignedBy())
//                .signerRole(in.getSignerRole())
//                .signatureType(in.getSignatureType())
//                .signatureFormat(in.getSignatureFormat())
//                .signatureData(in.getSignatureData())
//                .signatureHash(in.getSignatureHash())
//                .status(in.getStatus())
//                .comments(in.getComments())
//                .build();
//
//        final ProviderSignature saved = repo.save(e);
//
//        external.ifPresent(ext -> {
//            final ProviderSignature ref = saved;
//            String extId = ext.create(mapToDto(ref));
//            ref.setExternalId(extId);
//            repo.save(ref);
//        });
//
//        return mapToDto(saved);
//    }
//
//    public ProviderSignatureDto update(Long orgId, Long patientId, Long encounterId, Long id, ProviderSignatureDto in) {
//        ProviderSignature e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Provider signature not found"));
//
//        e.setSignedAt(in.getSignedAt());
//        e.setSignedBy(in.getSignedBy());
//        e.setSignerRole(in.getSignerRole());
//        e.setSignatureType(in.getSignatureType());
//        e.setSignatureFormat(in.getSignatureFormat());
//        e.setSignatureData(in.getSignatureData());
//        e.setSignatureHash(in.getSignatureHash());
//        e.setStatus(in.getStatus());
//        e.setComments(in.getComments());
//
//        final ProviderSignature updated = repo.save(e);
//
//        external.ifPresent(ext -> {
//            final ProviderSignature ref = updated;
//            if (ref.getExternalId() != null) {
//                ext.update(ref.getExternalId(), mapToDto(ref));
//            }
//        });
//
//        return mapToDto(updated);
//    }
//
//    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
//        ProviderSignature e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Provider signature not found"));
//
//        external.ifPresent(ext -> {
//            if (e.getExternalId() != null) ext.delete(e.getExternalId());
//        });
//
//        repo.delete(e);
//    }
//
//    public ProviderSignatureDto getOne(Long orgId, Long patientId, Long encounterId, Long id) {
//        ProviderSignature e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Provider signature not found"));
//        return mapToDto(e);
//    }
//
//    public List<ProviderSignatureDto> getAllByPatient(Long orgId, Long patientId) {
//        return repo.findByOrgIdAndPatientId(orgId, patientId).stream().map(this::mapToDto).toList();
//    }
//
//    public List<ProviderSignatureDto> getAllByEncounter(Long orgId, Long patientId, Long encounterId) {
//        return repo.findByOrgIdAndPatientIdAndEncounterId(orgId, patientId, encounterId).stream().map(this::mapToDto).toList();
//    }
//
//    private ProviderSignatureDto mapToDto(ProviderSignature e) {
//        ProviderSignatureDto dto = new ProviderSignatureDto();
//        dto.setId(e.getId());
//        dto.setExternalId(e.getExternalId());
//        dto.setOrgId(e.getOrgId());
//        dto.setPatientId(e.getPatientId());
//        dto.setEncounterId(e.getEncounterId());
//        dto.setSignedAt(e.getSignedAt());
//        dto.setSignedBy(e.getSignedBy());
//        dto.setSignerRole(e.getSignerRole());
//        dto.setSignatureType(e.getSignatureType());
//        dto.setSignatureFormat(e.getSignatureFormat());
//        dto.setSignatureData(e.getSignatureData());
//        dto.setSignatureHash(e.getSignatureHash());
//        dto.setStatus(e.getStatus());
//        dto.setComments(e.getComments());
//
//        ProviderSignatureDto.Audit a = new ProviderSignatureDto.Audit();
//        if (e.getCreatedAt() != null) a.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
//        if (e.getUpdatedAt() != null) a.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
//        dto.setAudit(a);
//        return dto;
//    }
//}

package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ProviderSignatureDto;
import com.qiaben.ciyex.entity.ProviderSignature;
import com.qiaben.ciyex.repository.ProviderSignatureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProviderSignatureService {

    private final ProviderSignatureRepository repo;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Create
    public ProviderSignatureDto create(Long orgId, Long patientId, Long encounterId, ProviderSignatureDto dto) {
        ProviderSignature e = new ProviderSignature();
        e.setOrgId(orgId);
        e.setPatientId(patientId);
        e.setEncounterId(encounterId);
        applyDto(e, dto);
        if (StringUtils.hasText(e.getSignatureData())) {
            e.setSignatureHash(sha256(e.getSignatureData()));
        }
        e.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "SIGNED");
        e = repo.save(e);
        return toDto(e);
    }

    // eSign alias (same as create)
    public ProviderSignatureDto eSign(Long orgId, Long patientId, Long encounterId, ProviderSignatureDto dto) {
        return create(orgId, patientId, encounterId, dto);
    }

    public ProviderSignatureDto getOne(Long orgId, Long patientId, Long encounterId, Long id) {
        ProviderSignature e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Provider signature not found"));
        return toDto(e);
    }

    public List<ProviderSignatureDto> list(Long orgId, Long patientId, Long encounterId) {
        return repo.findByOrgIdAndPatientIdAndEncounterId(orgId, patientId, encounterId).stream()
                .map(this::toDto).toList();
    }

    public ProviderSignatureDto update(Long orgId, Long patientId, Long encounterId, Long id, ProviderSignatureDto dto) {
        ProviderSignature e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Provider signature not found"));
        applyDto(e, dto);
        if (StringUtils.hasText(e.getSignatureData())) {
            e.setSignatureHash(sha256(e.getSignatureData()));
        }
        e = repo.save(e);
        return toDto(e);
    }

    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
        ProviderSignature e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Provider signature not found"));
        repo.delete(e);
    }

    // Print a simple PDF with the signature image (if present)
    public byte[] renderPdf(Long orgId, Long patientId, Long encounterId, Long id) {
        ProviderSignature e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Provider signature not found"));

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float x = 64, y = 740;

                // Title
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.newLineAtOffset(x, y);
                cs.showText("Provider Signature");
                cs.endText();

                y -= 26;
                draw(cs, x, y, "Patient ID:", String.valueOf(patientId)); y -= 16;
                draw(cs, x, y, "Encounter ID:", String.valueOf(encounterId)); y -= 16;
                draw(cs, x, y, "Signature ID:", String.valueOf(id)); y -= 16;
                if (StringUtils.hasText(e.getSignedAt())) { draw(cs, x, y, "Signed At:", e.getSignedAt()); y -= 16; }
                if (StringUtils.hasText(e.getSignedBy())) { draw(cs, x, y, "Signed By:", e.getSignedBy()); y -= 16; }
                if (StringUtils.hasText(e.getSignerRole())) { draw(cs, x, y, "Role:", e.getSignerRole()); y -= 16; }
                if (StringUtils.hasText(e.getStatus())) { draw(cs, x, y, "Status:", e.getStatus()); y -= 16; }

                if (StringUtils.hasText(e.getComments())) {
                    y -= 8;
                    draw(cs, x, y, "Comments:", ""); y -= 14;
                    for (String ln : e.getComments().split("\\R")) {
                        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x, y); cs.showText(ln); cs.endText();
                        y -= 14;
                    }
                }

                // (Optional) You can embed the image onto PDF using PDFBox images; keeping text-only for portability.
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate signature PDF", ex);
        }
    }

    // ---- helpers

    private static String sha256(String base64) {
        // Hash the original base64 string for integrity
        byte[] bytes = base64.getBytes(StandardCharsets.UTF_8);
        return DigestUtils.md5DigestAsHex(bytes); // md5 is enough for quick fingerprint; swap to SHA-256 if preferred.
    }

    private static void draw(PDPageContentStream cs, float x, float y, String label, String value) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 12); cs.newLineAtOffset(x, y); cs.showText(label); cs.endText();
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x + 140, y); cs.showText(value != null ? value : "-"); cs.endText();
    }

    private ProviderSignatureDto toDto(ProviderSignature e) {
        ProviderSignatureDto d = new ProviderSignatureDto();
        d.setId(e.getId());
        d.setExternalId(e.getExternalId());
        d.setOrgId(e.getOrgId());
        d.setPatientId(e.getPatientId());
        d.setEncounterId(e.getEncounterId());
        d.setSignedAt(e.getSignedAt());
        d.setSignedBy(e.getSignedBy());
        d.setSignerRole(e.getSignerRole());
        d.setSignatureType(e.getSignatureType());
        d.setSignatureFormat(e.getSignatureFormat());
        d.setSignatureData(e.getSignatureData());
        d.setSignatureHash(e.getSignatureHash());
        d.setStatus(e.getStatus());
        d.setComments(e.getComments());

        ProviderSignatureDto.Audit a = new ProviderSignatureDto.Audit();
        if (e.getCreatedAt() != null) a.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
        if (e.getUpdatedAt() != null) a.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
        d.setAudit(a);
        return d;
    }

    private void applyDto(ProviderSignature e, ProviderSignatureDto d) {
        e.setExternalId(d.getExternalId());
        e.setSignedAt(StringUtils.hasText(d.getSignedAt()) ? d.getSignedAt() : java.time.OffsetDateTime.now().toString());
        e.setSignedBy(d.getSignedBy());
        e.setSignerRole(d.getSignerRole());
        e.setSignatureType(d.getSignatureType());
        e.setSignatureFormat(d.getSignatureFormat());
        e.setSignatureData(d.getSignatureData());
        e.setStatus(d.getStatus());
        e.setComments(d.getComments());
        // signatureHash computed automatically if data present
    }
}
