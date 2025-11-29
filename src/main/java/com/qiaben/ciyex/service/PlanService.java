//package com.qiaben.ciyex.service;
//
//import com.qiaben.ciyex.dto.PlanDto;
//import com.qiaben.ciyex.entity.Plan;
//import com.qiaben.ciyex.repository.PlanRepository;
//import com.qiaben.ciyex.storage.ExternalPlanStorage;
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
//public class PlanService {
//
//    private final PlanRepository repo;
//    // wire later if needed; left as Optional to match your original stub
//    private final Optional<ExternalPlanStorage> external = Optional.empty();
//
//    private static final DateTimeFormatter DTF = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
//
//    public PlanDto create(Long patientId, Long encounterId, PlanDto in) {
//        Plan p = new Plan();
//
//        p.setPatientId(patientId);
//        p.setEncounterId(encounterId);
//        p.setDiagnosticPlan(in.getDiagnosticPlan());
//        p.setPlan(in.getPlan());
//        p.setNotes(in.getNotes());
//        p.setFollowUpVisit(in.getFollowUpVisit());
//        p.setReturnWorkSchool(in.getReturnWorkSchool());
//        // ✅ direct JsonNode -> jsonb
//        p.setSectionsJson(in.getSectionsJson());
//
//        final Plan saved = repo.save(p);
//
//        external.ifPresent(ext -> {
//            String extId = ext.create(mapToDto(saved));
//            saved.setExternalId(extId);
//            repo.save(saved);
//        });
//
//        return mapToDto(saved);
//    }
//
//    public PlanDto update(Long patientId, Long encounterId, Long id, PlanDto in) {
//        Plan p = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));
//
//        p.setDiagnosticPlan(in.getDiagnosticPlan());
//        p.setPlan(in.getPlan());
//        p.setNotes(in.getNotes());
//        p.setFollowUpVisit(in.getFollowUpVisit());
//        p.setReturnWorkSchool(in.getReturnWorkSchool());
//        // ✅ direct JsonNode -> jsonb
//        p.setSectionsJson(in.getSectionsJson());
//
//        final Plan updated = repo.save(p);
//
//        external.ifPresent(ext -> {
//            if (updated.getExternalId() != null) {
//                ext.update(updated.getExternalId(), mapToDto(updated));
//            }
//        });
//
//        return mapToDto(updated);
//    }
//
//    public void delete(Long patientId, Long encounterId, Long id) {
//        Plan p = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));
//        repo.delete(p);
//        external.ifPresent(ext -> {
//            if (p.getExternalId() != null) {
//                ext.delete(p.getExternalId());
//            }
//        });
//    }
//
//    public PlanDto getOne(Long patientId, Long encounterId, Long id) {
//        Plan p = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));
//        return mapToDto(p);
//    }
//
//    public List<PlanDto> getAllByPatient(Long patientId) {
//        return repo.findByPatientId(patientId)
//                .stream().map(this::mapToDto).toList();
//    }
//
//    public List<PlanDto> getAllByEncounter(Long patientId, Long encounterId) {
//        return repo.findByPatientIdAndEncounterId(patientId, encounterId)
//                .stream().map(this::mapToDto).toList();
//    }
//
//    private PlanDto mapToDto(Plan e) {
//        PlanDto dto = new PlanDto();
//        dto.setId(e.getId());
//        dto.setExternalId(e.getExternalId());
//        dto.setOrgId(e.getOrgId());
//        dto.setPatientId(e.getPatientId());
//        dto.setEncounterId(e.getEncounterId());
//        dto.setDiagnosticPlan(e.getDiagnosticPlan());
//        dto.setPlan(e.getPlan());
//        dto.setNotes(e.getNotes());
//        dto.setFollowUpVisit(e.getFollowUpVisit());
//        dto.setReturnWorkSchool(e.getReturnWorkSchool());
//        // ✅ JsonNode passes straight through
//        dto.setSectionsJson(e.getSectionsJson());
//
//        PlanDto.Audit a = new PlanDto.Audit();
//        if (e.getCreatedAt() != null) {
//            a.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
//        }
//        if (e.getUpdatedAt() != null) {
//            a.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
//        }
//        dto.setAudit(a);
//        return dto;
//    }
//}






package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.PlanDto;
import com.qiaben.ciyex.entity.Plan;
import com.qiaben.ciyex.repository.PlanRepository;
import com.qiaben.ciyex.repository.PatientRepository;
import com.qiaben.ciyex.repository.EncounterRepository;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.storage.fhir.FhirExternalPlanStorage;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
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
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service @RequiredArgsConstructor @Slf4j
public class PlanService {

    private final PlanRepository repo;
    private final EncounterService encounterService;
    private final PatientRepository patientRepository;
    private final EncounterRepository encounterRepository;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;

    @Autowired(required = false)
    private FhirExternalPlanStorage fhirStorage;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public PlanDto create(Long patientId, Long encounterId, PlanDto dto) {
        // Validate patient existence
        boolean patientExists = patientRepository.existsById(patientId);
        boolean encounterExists = encounterRepository.findByIdAndPatientId(encounterId, patientId).isPresent();
        if (!patientExists && !encounterExists) {
            throw new IllegalArgumentException("Patient and Encounter not found");
        } else if (!patientExists) {
            throw new IllegalArgumentException("Patient not found");
        } else if (!encounterExists) {
            throw new IllegalArgumentException("Encounter not found");
        }
        // Check if encounter is signed - prevent modification
        encounterService.validateEncounterNotSigned(encounterId, patientId);
        Plan e = new Plan(); e.setPatientId(patientId); e.setEncounterId(encounterId);
        applyEditable(e, dto);
        e = repo.save(e);
        
        // Step 5: Optional external FHIR sync
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        log.info("Plan create - storageType for current org: {}", storageType);

        if (storageType != null) {
            try {
                log.info("Attempting FHIR sync for Plan ID: {}", e.getId());
                ExternalStorage<PlanDto> ext = storageResolver.resolve(PlanDto.class);
                log.info("Resolved external storage: {}", ext.getClass().getName());

                PlanDto snapshot = toDto(e);
                String externalId = ext.create(snapshot);
                log.info("FHIR create returned externalId: {}", externalId);

                if (externalId != null && !externalId.isEmpty()) {
                    e.setExternalId(externalId);
                    e = repo.save(e);
                    log.info("Created FHIR resource for Plan ID: {} with externalId: {}", e.getId(), externalId);
                } else {
                    log.warn("FHIR create returned null or empty externalId for Plan ID: {}", e.getId());
                }
            } catch (Exception ex) {
                log.error("Failed to sync Plan to external storage", ex);
            }
        } else if (fhirStorage != null) {
            try {
                log.info("No storage type configured, falling back to direct FHIR storage for Plan ID: {}", e.getId());
                PlanDto snapshot = toDto(e);
                String externalId = fhirStorage.create(snapshot);
                log.info("FHIR fallback create returned externalId: {}", externalId);

                if (externalId != null && !externalId.isEmpty()) {
                    e.setExternalId(externalId);
                    e = repo.save(e);
                    log.info("Created FHIR resource (fallback) for Plan ID: {} with externalId: {}", e.getId(), externalId);
                }
            } catch (Exception ex) {
                log.error("Failed to sync Plan to external storage (fallback)", ex);
            }
        } else {
            log.warn("No storage type configured for current org and no FHIR fallback available - skipping FHIR sync for Plan ID: {}", e.getId());
        }

        if (e.getExternalId() == null) {
            String generatedId = "PL-" + System.currentTimeMillis();
            e.setExternalId(generatedId);
            e.setFhirId(generatedId);
            e = repo.save(e);
            log.info("Auto-generated externalId: {}", generatedId);
        } else {
            e.setFhirId(e.getExternalId());
            e = repo.save(e);
        }

        return toDto(e);
    }

    public List<PlanDto> list(Long patientId, Long encounterId) {
        return repo.findByPatientIdAndEncounterId(patientId, encounterId)
                .stream().map(this::toDto).toList();
    }

        // GET ALL BY PATIENT
        public List<PlanDto> getAllByPatient(Long patientId) {
            return repo.findByPatientId(patientId)
                    .stream().map(this::toDto).toList();
        }

    public PlanDto getOne(Long patientId, Long encounterId, Long id) {
        Plan e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Plan not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id)
                ));
        // Check if encounter is signed - prevent modification
        encounterService.validateEncounterNotSigned(encounterId, patientId);

        return toDto(e);
    }

    public PlanDto update(Long patientId, Long encounterId, Long id, PlanDto dto) {
        // Validate patient existence
        boolean patientExists = patientRepository.existsById(patientId);
        boolean encounterExists = encounterRepository.findByIdAndPatientId(encounterId, patientId).isPresent();
        if (!patientExists && !encounterExists) {
            throw new IllegalArgumentException("Patient and Encounter not found");
        } else if (!patientExists) {
            throw new IllegalArgumentException("Patient not found");
        } else if (!encounterExists) {
            throw new IllegalArgumentException("Encounter not found");
        }
        Plan e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Plan not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id)
                ));
        if (Boolean.TRUE.equals(e.getESigned())) throw new IllegalStateException("Signed plan is read-only.");
        applyEditable(e, dto);
        e = repo.save(e);

        // Step 7: Optional external FHIR sync
        if (e.getExternalId() != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            log.info("Plan update - storageType for current org: {}", storageType);

            if (storageType != null) {
                try {
                    log.info("Attempting FHIR sync for Plan ID: {}", e.getId());
                    ExternalStorage<PlanDto> ext = storageResolver.resolve(PlanDto.class);
                    log.info("Resolved external storage: {}", ext.getClass().getName());

                    PlanDto snapshot = toDto(e);
                    ext.update(snapshot, e.getExternalId());
                    log.info("Updated FHIR resource for Plan ID: {} with externalId: {}", e.getId(), e.getExternalId());
                } catch (Exception ex) {
                    log.error("Failed to sync Plan update to external storage", ex);
                }
            } else if (fhirStorage != null) {
                try {
                    log.info("No storage type configured, falling back to direct FHIR storage for Plan ID: {}", e.getId());
                    PlanDto snapshot = toDto(e);
                    fhirStorage.update(snapshot, e.getExternalId());
                    log.info("Updated FHIR resource (fallback) for Plan ID: {} with externalId: {}", e.getId(), e.getExternalId());
                } catch (Exception ex) {
                    log.error("Failed to sync Plan update to external storage (fallback)", ex);
                }
            } else {
                log.warn("No storage type configured for current org and no FHIR fallback available - skipping FHIR sync for Plan ID: {}", e.getId());
            }
        }

        // Check if encounter is signed - prevent modification
        encounterService.validateEncounterNotSigned(encounterId, patientId);
        return toDto(e);
    }

    public void delete(Long patientId, Long encounterId, Long id) {
        Plan e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Plan not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id)
                ));
        if (Boolean.TRUE.equals(e.getESigned())) throw new IllegalStateException("Signed plan cannot be deleted.");

        // Optional external FHIR sync
        if (e.getExternalId() != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            log.info("Plan delete - storageType for current org: {}", storageType);

            if (storageType != null) {
                try {
                    log.info("Attempting FHIR delete for Plan ID: {}", e.getId());
                    ExternalStorage<PlanDto> ext = storageResolver.resolve(PlanDto.class);
                    log.info("Resolved external storage: {}", ext.getClass().getName());

                    ext.delete(e.getExternalId());
                    log.info("Deleted FHIR resource for Plan ID: {} with externalId: {}", e.getId(), e.getExternalId());
                } catch (Exception ex) {
                    log.error("Failed to sync Plan delete to external storage", ex);
                }
            } else if (fhirStorage != null) {
                try {
                    log.info("No storage type configured, falling back to direct FHIR storage for Plan ID: {}", e.getId());
                    fhirStorage.delete(e.getExternalId());
                    log.info("Deleted FHIR resource (fallback) for Plan ID: {} with externalId: {}", e.getId(), e.getExternalId());
                } catch (Exception ex) {
                    log.error("Failed to sync Plan delete to external storage (fallback)", ex);
                }
            } else {
                log.warn("No storage type configured for current org and no FHIR fallback available - skipping FHIR sync for Plan ID: {}", e.getId());
            }
        }

        repo.delete(e);
    }

    public PlanDto eSign(Long patientId, Long encounterId, Long id, String signedBy) {
        Plan e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Plan not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id)
                ));
        if (Boolean.TRUE.equals(e.getESigned())) return toDto(e);

        e.setESigned(true);
        e.setSignedBy(StringUtils.hasText(signedBy) ? signedBy : "system");
        e.setSignedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return toDto(repo.save(e));
    }

    public byte[] renderPdf(Long patientId, Long encounterId, Long id) {
        Plan e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Plan not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id)
                ));
        e.setPrintedAt(OffsetDateTime.now(ZoneOffset.UTC));
        repo.save(e);

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float x = 64, y = 740;

                // Title + meta
                title(cs, x, y, "Encounter Plan"); y -= 26;
                row(cs, x, y, "Patient ID:", String.valueOf(patientId));     y -= 16;
                row(cs, x, y, "Encounter ID:", String.valueOf(encounterId)); y -= 16;
                row(cs, x, y, "Plan ID:", String.valueOf(id));               y -= 20;

                // FREE-TEXT BLOCKS – each returns updated y (no extra y-= after the call)
                y = block(cs, x, y, "Diagnostic Plan", e.getDiagnosticPlan());
                y = block(cs, x, y, "Plan",            e.getPlan());
                y = block(cs, x, y, "Notes",           e.getNotes());

                // Rows
                if (StringUtils.hasText(e.getFollowUpVisit())) {
                    row(cs, x, y, "Follow-Up Visit:", e.getFollowUpVisit()); y -= 16;
                }
                if (StringUtils.hasText(e.getReturnWorkSchool())) {
                    row(cs, x, y, "Return Work/School:", e.getReturnWorkSchool()); y -= 16;
                }

                // Sections (render as wrapped block)
                if (StringUtils.hasText(e.getSectionsJson())) {
                    y = block(cs, x, y, "Sections", e.getSectionsJson());
                }

                y -= 10;
                row(cs, x, y, "eSigned:", Boolean.TRUE.equals(e.getESigned()) ? "Yes" : "No"); y -= 16;
                if (e.getSignedAt() != null) { row(cs, x, y, "Signed At:", e.getSignedAt().format(ISO)); y -= 16; }
                if (StringUtils.hasText(e.getSignedBy())) { row(cs, x, y, "Signed By:", e.getSignedBy()); y -= 16; }

                y -= 10;
                if (e.getCreatedAt() != null) { row(cs, x, y, "Created:", DAY.format(e.getCreatedAt())); y -= 16; }
                if (e.getUpdatedAt() != null) { row(cs, x, y, "Updated:", DAY.format(e.getUpdatedAt())); y -= 16; }
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate Plan PDF", ex);
        }
    }

    // ---- mapping / drawing helpers
    private void applyEditable(Plan e, PlanDto d) {
        e.setDiagnosticPlan(d.getDiagnosticPlan());
        e.setPlan(d.getPlan());
        e.setNotes(d.getNotes());
        e.setFollowUpVisit(d.getFollowUpVisit());
        e.setReturnWorkSchool(d.getReturnWorkSchool());
        e.setSectionsJson(d.getSectionsJson()); // keep as string
    }

    private PlanDto toDto(Plan e) {
        PlanDto d = new PlanDto();
        d.setId(e.getId());
        d.setExternalId(e.getExternalId());
        d.setFhirId(e.getFhirId());
        d.setPatientId(e.getPatientId()); d.setEncounterId(e.getEncounterId());
        d.setDiagnosticPlan(e.getDiagnosticPlan()); d.setPlan(e.getPlan()); d.setNotes(e.getNotes());
        d.setFollowUpVisit(e.getFollowUpVisit()); d.setReturnWorkSchool(e.getReturnWorkSchool());
        d.setSectionsJson(e.getSectionsJson());
        d.setESigned(e.getESigned());
        d.setSignedAt(e.getSignedAt() != null ? e.getSignedAt().format(ISO) : null);
        d.setSignedBy(e.getSignedBy());
        d.setPrintedAt(e.getPrintedAt() != null ? e.getPrintedAt().format(ISO) : null);

        PlanDto.Audit a = new PlanDto.Audit();
        if (e.getCreatedAt() != null) a.setCreatedDate(DAY.format(e.getCreatedAt()));
        if (e.getUpdatedAt() != null) a.setLastModifiedDate(DAY.format(e.getUpdatedAt()));
        d.setAudit(a);
        return d;
    }

    private static void title(PDPageContentStream cs, float x, float y, String t) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 18); cs.newLineAtOffset(x, y); cs.showText(t); cs.endText();
    }
    private static void row(PDPageContentStream cs, float x, float y, String k, String v) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 12); cs.newLineAtOffset(x, y); cs.showText(k); cs.endText();
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x + 140, y); cs.showText(v != null ? v : "-"); cs.endText();
    }
    private static void text(PDPageContentStream cs, float x, float y, String s) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x, y); cs.showText(s); cs.endText();
    }
    private static float block(PDPageContentStream cs, float x, float y, String label, String value) throws IOException {
        if (!StringUtils.hasText(value)) return y;

        // Label
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
        cs.newLineAtOffset(x, y);
        cs.showText(label + ":");
        cs.endText();
        y -= 16;

        // Content (indented + wrapped)
        final float maxWidth = 612f - (64f * 2) - 16f; // pageWidth - margins - indent
        for (String line : wrap(PDType1Font.HELVETICA, 12, value, maxWidth)) {
            text(cs, x + 16, y, line);
            y -= 14;
        }
        return y - 6; // padding after block
    }

    /** Simple word-wrap for PDFBox (no hyphenation). */
    private static java.util.List<String> wrap(PDType1Font font, int fontSize, String text, float maxWidth) throws IOException {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String para : text.split("\\R")) {
            if (!StringUtils.hasText(para)) { out.add(""); continue; }
            String[] words = para.split("\\s+");
            StringBuilder line = new StringBuilder();
            for (String w : words) {
                String candidate = line.length() == 0 ? w : line + " " + w;
                float width = font.getStringWidth(candidate) / 1000f * fontSize;
                if (width <= maxWidth) {
                    line.setLength(0);
                    line.append(candidate);
                } else {
                    out.add(line.toString());
                    line.setLength(0);
                    line.append(w);
                }
            }
            out.add(line.toString());
        }
        return out;
    }
}
