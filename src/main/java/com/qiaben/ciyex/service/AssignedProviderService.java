//package com.qiaben.ciyex.service;
//
//import com.qiaben.ciyex.dto.AssignedProviderDto;
//import com.qiaben.ciyex.entity.AssignedProvider;
//import com.qiaben.ciyex.repository.AssignedProviderRepository;
//import com.qiaben.ciyex.storage.ExternalAssignedProviderStorage;
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
//public class AssignedProviderService {
//
//    private final AssignedProviderRepository repo;
//    private final Optional<ExternalAssignedProviderStorage> external;
//
//    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//
//    public AssignedProviderDto create(Long orgId, Long patientId, Long encounterId, AssignedProviderDto in) {
//        AssignedProvider e = AssignedProvider.builder()
//                .orgId(orgId).patientId(patientId).encounterId(encounterId)
//                .providerId(in.getProviderId())
//                .role(in.getRole())
//                .startDate(in.getStartDate())
//                .endDate(in.getEndDate())
//                .status(in.getStatus())
//                .notes(in.getNotes())
//                .build();
//
//        final AssignedProvider saved = repo.save(e);
//
//        external.ifPresent(ext -> {
//            final AssignedProvider ref = saved;
//            String extId = ext.create(mapToDto(ref));
//            ref.setExternalId(extId);
//            repo.save(ref);
//        });
//
//        return mapToDto(saved);
//    }
//
//    public AssignedProviderDto update(Long orgId, Long patientId, Long encounterId, Long id, AssignedProviderDto in) {
//        AssignedProvider e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Assigned provider not found"));
//
//        e.setProviderId(in.getProviderId());
//        e.setRole(in.getRole());
//        e.setStartDate(in.getStartDate());
//        e.setEndDate(in.getEndDate());
//        e.setStatus(in.getStatus());
//        e.setNotes(in.getNotes());
//
//        final AssignedProvider updated = repo.save(e);
//
//        external.ifPresent(ext -> {
//            final AssignedProvider ref = updated;
//            if (ref.getExternalId() != null) {
//                ext.update(ref.getExternalId(), mapToDto(ref));
//            }
//        });
//
//        return mapToDto(updated);
//    }
//
//    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
//        AssignedProvider e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Assigned provider not found"));
//
//        external.ifPresent(ext -> {
//            if (e.getExternalId() != null) ext.delete(e.getExternalId());
//        });
//
//        repo.delete(e);
//    }
//
//    public AssignedProviderDto getOne(Long orgId, Long patientId, Long encounterId, Long id) {
//        AssignedProvider e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Assigned provider not found"));
//        return mapToDto(e);
//    }
//
//    public List<AssignedProviderDto> getAllByPatient(Long orgId, Long patientId) {
//        return repo.findByOrgIdAndPatientId(orgId, patientId).stream().map(this::mapToDto).toList();
//    }
//
//    public List<AssignedProviderDto> getAllByEncounter(Long orgId, Long patientId, Long encounterId) {
//        return repo.findByOrgIdAndPatientIdAndEncounterId(orgId, patientId, encounterId).stream().map(this::mapToDto).toList();
//    }
//
//    private AssignedProviderDto mapToDto(AssignedProvider e) {
//        AssignedProviderDto dto = new AssignedProviderDto();
//        dto.setId(e.getId());
//        dto.setExternalId(e.getExternalId());
//        dto.setOrgId(e.getOrgId());
//        dto.setPatientId(e.getPatientId());
//        dto.setEncounterId(e.getEncounterId());
//
//        dto.setProviderId(e.getProviderId());
//        dto.setRole(e.getRole());
//        dto.setStartDate(e.getStartDate());
//        dto.setEndDate(e.getEndDate());
//        dto.setStatus(e.getStatus());
//        dto.setNotes(e.getNotes());
//
//        AssignedProviderDto.Audit a = new AssignedProviderDto.Audit();
//        if (e.getCreatedAt() != null) a.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
//        if (e.getUpdatedAt() != null) a.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
//        dto.setAudit(a);
//        return dto;
//    }
//}

package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.AssignedProviderDto;
import com.qiaben.ciyex.entity.AssignedProvider;
import com.qiaben.ciyex.repository.AssignedProviderRepository;
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
public class AssignedProviderService {

    private final AssignedProviderRepository repo;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Create
    public AssignedProviderDto create(Long orgId, Long patientId, Long encounterId, AssignedProviderDto dto) {
        AssignedProvider e = new AssignedProvider();
        e.setOrgId(orgId);
        e.setPatientId(patientId);
        e.setEncounterId(encounterId);
        applyDto(e, dto);
        e = repo.save(e);
        return toDto(e);
    }

    // Read
    public AssignedProviderDto getOne(Long orgId, Long patientId, Long encounterId, Long id) {
        AssignedProvider e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Assigned provider not found"));
        return toDto(e);
    }

    public List<AssignedProviderDto> list(Long orgId, Long patientId, Long encounterId) {
        return repo.findByOrgIdAndPatientIdAndEncounterId(orgId, patientId, encounterId)
                .stream().map(this::toDto).toList();
    }

    // Update (LOCKED if eSigned)
    public AssignedProviderDto update(Long orgId, Long patientId, Long encounterId, Long id, AssignedProviderDto dto) {
        AssignedProvider e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Assigned provider not found"));

        if (Boolean.TRUE.equals(e.getESigned())) {
            throw new IllegalStateException("Signed records are read-only.");
        }

        applyDto(e, dto);
        e = repo.save(e);
        return toDto(e);
    }

    // Delete (BLOCKED if eSigned)
    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
        AssignedProvider e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Assigned provider not found"));

        if (Boolean.TRUE.equals(e.getESigned())) {
            throw new IllegalStateException("Signed records cannot be deleted.");
        }

        repo.delete(e);
    }

    // eSign (idempotent)
    public AssignedProviderDto eSign(Long orgId, Long patientId, Long encounterId, Long id, String signedBy) {
        AssignedProvider e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Assigned provider not found"));

        if (Boolean.TRUE.equals(e.getESigned())) {
            return toDto(e);
        }

        e.setESigned(Boolean.TRUE);
        e.setSignedBy(StringUtils.hasText(signedBy) ? signedBy : "system");
        e.setSignedAt(java.time.OffsetDateTime.now(ZoneOffset.UTC));
        e = repo.save(e);
        return toDto(e);
    }

    // Print (PDF) — also stamps printedAt
    public byte[] renderPdf(Long orgId, Long patientId, Long encounterId, Long id) {
        AssignedProvider e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Assigned provider not found"));

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
                cs.showText("Assigned Provider");
                cs.endText();

                // Meta lines
                y -= 26;
                draw(cs, x, y, "Patient ID:", String.valueOf(patientId)); y -= 16;
                draw(cs, x, y, "Encounter ID:", String.valueOf(encounterId)); y -= 16;
                draw(cs, x, y, "Record ID:", String.valueOf(id)); y -= 22;

                // Content
                draw(cs, x, y, "Provider ID:", String.valueOf(e.getProviderId())); y -= 16;
                draw(cs, x, y, "Role:", nullTo(e.getRole(), "-")); y -= 16;
                draw(cs, x, y, "Start Date:", nullTo(e.getStartDate(), "-")); y -= 16;
                draw(cs, x, y, "End Date:", nullTo(e.getEndDate(), "-")); y -= 16;
                draw(cs, x, y, "Status:", nullTo(e.getStatus(), "-")); y -= 22;
                draw(cs, x, y, "Notes:", nullTo(e.getNotes(), "-")); y -= 22;

                // eSign footer
                draw(cs, x, y, "eSigned:", Boolean.TRUE.equals(e.getESigned()) ? "Yes" : "No"); y -= 16;
                if (e.getSignedAt() != null) { draw(cs, x, y, "Signed At:", e.getSignedAt().toString()); y -= 16; }
                if (StringUtils.hasText(e.getSignedBy())) { draw(cs, x, y, "Signed By:", e.getSignedBy()); y -= 16; }
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate Assigned Provider PDF", ex);
        }
    }

    // ---- helpers ----
    private static void draw(PDPageContentStream cs, float x, float y, String label, String value) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 12); cs.newLineAtOffset(x, y); cs.showText(label); cs.endText();
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x + 140, y); cs.showText(value != null ? value : "-"); cs.endText();
    }

    private static String nullTo(String v, String fb) { return (v == null || v.isBlank()) ? fb : v; }

    private AssignedProviderDto toDto(AssignedProvider e) {
        AssignedProviderDto d = new AssignedProviderDto();
        d.setId(e.getId());
        d.setExternalId(e.getExternalId());
        d.setOrgId(e.getOrgId());
        d.setPatientId(e.getPatientId());
        d.setEncounterId(e.getEncounterId());
        d.setProviderId(e.getProviderId());
        d.setRole(e.getRole());
        d.setStartDate(e.getStartDate());
        d.setEndDate(e.getEndDate());
        d.setStatus(e.getStatus());
        d.setNotes(e.getNotes());

        d.setESigned(e.getESigned());
        d.setSignedAt(e.getSignedAt());
        d.setSignedBy(e.getSignedBy());
        d.setPrintedAt(e.getPrintedAt());

        AssignedProviderDto.Audit a = new AssignedProviderDto.Audit();
        if (e.getCreatedAt() != null) a.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
        if (e.getUpdatedAt() != null) a.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
        d.setAudit(a);

        return d;
    }

    private void applyDto(AssignedProvider e, AssignedProviderDto d) {
        e.setExternalId(d.getExternalId());
        e.setProviderId(d.getProviderId());
        e.setRole(d.getRole());
        e.setStartDate(d.getStartDate());
        e.setEndDate(d.getEndDate());
        e.setStatus(d.getStatus());
        e.setNotes(d.getNotes());
        // eSign fields are controlled by eSign()
    }
}
