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
//    public AssessmentDto create(Long patientId, Long encounterId, AssessmentDto in) {
//        Assessment a = Assessment.builder()

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
//    public AssessmentDto update(Long patientId, Long encounterId, Long id, AssessmentDto in) {
//        Assessment a = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
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
//    public void delete(Long patientId, Long encounterId, Long id) {
//        Assessment a = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
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
//    public AssessmentDto getOne(Long patientId, Long encounterId, Long id) {
//        Assessment a = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("Assessment not found"));
//        return mapToDto(a);
//    }
//
//    // GET ALL by patient
//    public List<AssessmentDto> getAllByPatient(Long patientId) {
//        return repo.findByPatientId(patientId).stream().map(this::mapToDto).toList();
//    }
//
//    // GET ALL by patient + encounter
//    public List<AssessmentDto> getAllByEncounter(Long patientId, Long encounterId) {
//        return repo.findByPatientIdAndEncounterId(patientId, encounterId).stream().map(this::mapToDto).toList();
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
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.storage.fhir.FhirExternalAssessmentStorage;
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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssessmentService {
    public List<AssessmentDto> getAllByPatient(Long patientId) {
        return repo.findByPatientId(patientId)
            .stream().map(this::toDto).toList();
    }

    private final AssessmentRepository repo;
    private final EncounterService encounterService;
    private final com.qiaben.ciyex.repository.PatientRepository patientRepository;
    private final com.qiaben.ciyex.repository.EncounterRepository encounterRepository;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;

    @Autowired(required = false)
    private FhirExternalAssessmentStorage fhirStorage;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ----- Create -----
    public AssessmentDto create(Long patientId, Long encounterId, AssessmentDto dto) {
        // Step 1: Validate Patient exists
        if (!patientRepository.existsById(patientId)) {
            throw new IllegalArgumentException(
                String.format("Patient not found with ID: %d. Please provide a valid Patient ID.", patientId)
            );
        }

        // Step 2: Validate Encounter exists and belongs to the Patient
        var encounterOpt = encounterRepository.findByIdAndPatientId(encounterId, patientId);
        if (encounterOpt.isEmpty()) {
            throw new IllegalArgumentException(
                String.format("Encounter not found with ID: %d for Patient ID: %d. Please verify both Patient ID and Encounter ID are correct and that the encounter belongs to this patient.",
                    encounterId, patientId)
            );
        }

        // Step 3: Check if encounter is signed - prevent modification
        encounterService.validateEncounterNotSigned(encounterId, patientId);

        // Step 4: Create the assessment
        Assessment e = new Assessment();
        e.setPatientId(patientId);
        e.setEncounterId(encounterId);
        applyDto(e, dto);
        e = repo.save(e);

        // Step 5: Optional external FHIR sync
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        log.info("Assessment create - storageType for current org: {}", storageType);

        if (storageType != null) {
            try {
                log.info("Attempting FHIR sync for Assessment ID: {}", e.getId());
                ExternalStorage<AssessmentDto> ext = storageResolver.resolve(AssessmentDto.class);
                log.info("Resolved external storage: {}", ext.getClass().getName());

                AssessmentDto snapshot = toDto(e);
                String externalId = ext.create(snapshot);
                log.info("FHIR create returned externalId: {}", externalId);

                if (externalId != null && !externalId.isEmpty()) {
                    e.setExternalId(externalId);
                    e = repo.save(e);
                    log.info("Created FHIR resource for Assessment ID: {} with externalId: {}", e.getId(), externalId);
                } else {
                    log.warn("FHIR create returned null or empty externalId for Assessment ID: {}", e.getId());
                }
            } catch (Exception ex) {
                log.error("Failed to sync Assessment to external storage", ex);
            }
        } else if (fhirStorage != null) {
            try {
                log.info("No storage type configured, falling back to direct FHIR storage for Assessment ID: {}", e.getId());
                AssessmentDto snapshot = toDto(e);
                String externalId = fhirStorage.create(snapshot);
                log.info("FHIR fallback create returned externalId: {}", externalId);

                if (externalId != null && !externalId.isEmpty()) {
                    e.setExternalId(externalId);
                    e = repo.save(e);
                    log.info("Created FHIR resource (fallback) for Assessment ID: {} with externalId: {}", e.getId(), externalId);
                }
            } catch (Exception ex) {
                log.error("Failed to sync Assessment to external storage (fallback)", ex);
            }
        } else {
            log.warn("No storage type configured for current org and no FHIR fallback available - skipping FHIR sync for Assessment ID: {}", e.getId());
        }

        if (e.getExternalId() == null) {
            String generatedId = "AS-" + System.currentTimeMillis();
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

    // ----- Read -----
    public AssessmentDto getOne(Long patientId, Long encounterId, Long id) {
        Assessment e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Assessment not found with ID: %d for Patient ID: %d and Encounter ID: %d. Please verify all IDs are correct.",
                        id, patientId, encounterId)
                ));
        return toDto(e);
    }

    public List<AssessmentDto> getAllByEncounter(Long patientId, Long encounterId) {
        return repo.findByPatientIdAndEncounterId(patientId, encounterId)
                .stream().map(this::toDto).toList();
    }

    // ----- Update (LOCK if signed) -----
    public AssessmentDto update(Long patientId, Long encounterId, Long id, AssessmentDto dto) {
        // Step 1: Validate Patient exists
        if (!patientRepository.existsById(patientId)) {
            throw new IllegalArgumentException(
                String.format("Patient not found with ID: %d. Please provide a valid Patient ID.", patientId)
            );
        }

        // Step 2: Validate Encounter exists and belongs to the Patient
        var encounterOpt = encounterRepository.findByIdAndPatientId(encounterId, patientId);
        if (encounterOpt.isEmpty()) {
            throw new IllegalArgumentException(
                String.format("Encounter not found with ID: %d for Patient ID: %d. Please verify both Patient ID and Encounter ID are correct and that the encounter belongs to this patient.",
                    encounterId, patientId)
            );
        }

        // Step 3: Check if encounter is signed - prevent modification
        encounterService.validateEncounterNotSigned(encounterId, patientId);

        // Step 4: Find the assessment
        Assessment e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Assessment not found with ID: %d for Patient ID: %d and Encounter ID: %d. Please verify all IDs are correct.",
                        id, patientId, encounterId)
                ));

        // Step 5: Check if assessment itself is signed
        if (Boolean.TRUE.equals(e.getESigned())) {
            throw new IllegalStateException("Signed assessments are read-only.");
        }

        // Step 6: Update the assessment
        applyDto(e, dto);
        e = repo.save(e);

        // Step 7: Optional external FHIR sync
        if (e.getExternalId() != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null) {
                try {
                    ExternalStorage<AssessmentDto> ext = storageResolver.resolve(AssessmentDto.class);
                    AssessmentDto snapshot = toDto(e);
                    ext.update(snapshot, e.getExternalId());
                    log.info("Updated FHIR resource for Assessment ID: {} with externalId: {}", e.getId(), e.getExternalId());
                } catch (Exception ex) {
                    log.warn("Failed to sync Assessment update to external storage: {}", ex.getMessage());
                }
            } else if (fhirStorage != null) {
                try {
                    AssessmentDto snapshot = toDto(e);
                    fhirStorage.update(snapshot, e.getExternalId());
                    log.info("Updated FHIR resource (fallback) for Assessment ID: {} with externalId: {}", e.getId(), e.getExternalId());
                } catch (Exception ex) {
                    log.warn("Failed to sync Assessment update to external storage (fallback): {}", ex.getMessage());
                }
            }
        }

        if (e.getExternalId() == null) {
            String generatedId = "AS-" + System.currentTimeMillis();
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

    // ----- Delete (BLOCK if signed) -----
    public void delete(Long patientId, Long encounterId, Long id) {
        Assessment e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Assessment not found with ID: %d for Patient ID: %d and Encounter ID: %d. Please verify all IDs are correct.",
                        id, patientId, encounterId)
                ));

        if (Boolean.TRUE.equals(e.getESigned())) {
            throw new IllegalStateException("Signed assessments cannot be deleted.");
        }

        // Optional external FHIR sync - delete before local delete
        if (e.getExternalId() != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null) {
                try {
                    ExternalStorage<AssessmentDto> ext = storageResolver.resolve(AssessmentDto.class);
                    ext.delete(e.getExternalId());
                    log.info("Deleted FHIR resource for Assessment ID: {} with externalId: {}", e.getId(), e.getExternalId());
                } catch (Exception ex) {
                    log.warn("Failed to delete Assessment from external storage: {}", ex.getMessage());
                }
            } else if (fhirStorage != null) {
                try {
                    fhirStorage.delete(e.getExternalId());
                    log.info("Deleted FHIR resource (fallback) for Assessment ID: {} with externalId: {}", e.getId(), e.getExternalId());
                } catch (Exception ex) {
                    log.warn("Failed to delete Assessment from external storage (fallback): {}", ex.getMessage());
                }
            }
        }

        repo.delete(e);
    }

    // ----- eSign (idempotent) -----
    public AssessmentDto eSign(Long patientId, Long encounterId, Long id, String signedBy) {
        Assessment a = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Assessment not found with ID: %d for Patient ID: %d and Encounter ID: %d. Please verify all IDs are correct.",
                        id, patientId, encounterId)
                ));

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
    public byte[] renderPdf(Long patientId, Long encounterId, Long id) {
        Assessment a = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Assessment not found with ID: %d for Patient ID: %d and Encounter ID: %d. Please verify all IDs are correct.",
                        id, patientId, encounterId)
                ));

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
        d.setFhirId(e.getFhirId());
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
        e.setFhirId(d.getFhirId());
        e.setDiagnosisCode(d.getDiagnosisCode());
        e.setDiagnosisName(d.getDiagnosisName());
        e.setStatus(d.getStatus());
        e.setPriority(d.getPriority());
        e.setAssessmentText(d.getAssessmentText());
        e.setNotes(d.getNotes());
        // eSign fields are controlled by eSign()
    }
}
