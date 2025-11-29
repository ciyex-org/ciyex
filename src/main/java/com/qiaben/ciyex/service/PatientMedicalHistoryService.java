//package com.qiaben.ciyex.service;
//
//import com.qiaben.ciyex.dto.PatientMedicalHistoryDto;
//import com.qiaben.ciyex.entity.PatientMedicalHistory;
//import com.qiaben.ciyex.repository.PatientMedicalHistoryRepository;
//import com.qiaben.ciyex.storage.ExternalPatientMedicalHistoryStorage;
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
//public class PatientMedicalHistoryService {
//
//    private final PatientMedicalHistoryRepository repo;
//    private final Optional<ExternalPatientMedicalHistoryStorage> external;
//
//    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//
//    // CREATE
//    public PatientMedicalHistoryDto create(Long patientId, Long encounterId, PatientMedicalHistoryDto in) {
//        PatientMedicalHistory toSave = PatientMedicalHistory.builder()

//                .patientId(patientId)
//                .encounterId(encounterId)
//                .description(in.getDescription())
//                .build();
//
//        // persist locally
//        final PatientMedicalHistory saved = repo.save(toSave);
//
//        // use final references inside lambda
//        external.ifPresent(ext -> {
//            final PatientMedicalHistory snapshotRef = saved; // final reference
//            String externalId = ext.create(mapToDto(snapshotRef));
//            snapshotRef.setExternalId(externalId);
//            repo.save(snapshotRef);
//        });
//
//        return mapToDto(saved);
//    }
//
//    // UPDATE
//    public PatientMedicalHistoryDto update(Long patientId, Long encounterId, Long id, PatientMedicalHistoryDto in) {
//        PatientMedicalHistory entity = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("PMH not found"));
//
//        entity.setDescription(in.getDescription());
//        final PatientMedicalHistory updated = repo.save(entity);
//
//        // capture a final reference for lambda use
//        external.ifPresent(ext -> {
//            final PatientMedicalHistory e = updated;
//            if (e.getExternalId() != null) {
//                ext.update(e.getExternalId(), mapToDto(e));
//            }
//        });
//
//        return mapToDto(updated);
//    }
//
//    // DELETE
//    public void delete(Long patientId, Long encounterId, Long id) {
//        PatientMedicalHistory entity = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("PMH not found"));
//
//        final PatientMedicalHistory toDelete = entity; // final for lambda
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
//    public PatientMedicalHistoryDto getOne(Long patientId, Long encounterId, Long id) {
//        PatientMedicalHistory entity = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
//                .orElseThrow(() -> new IllegalArgumentException("PMH not found"));
//        return mapToDto(entity);
//    }
//
//    // GET ALL by patient
//    public List<PatientMedicalHistoryDto> getAllByPatient(Long patientId) {
//        return repo.findByPatientId(patientId)
//                .stream().map(this::mapToDto).toList();
//    }
//
//    // GET ALL by patient + encounter
//    public List<PatientMedicalHistoryDto> getAllByEncounter(Long patientId, Long encounterId) {
//        return repo.findByPatientIdAndEncounterId(patientId, encounterId)
//                .stream().map(this::mapToDto).toList();
//    }
//
//    // Mapping
//    private PatientMedicalHistoryDto mapToDto(PatientMedicalHistory e) {
//        PatientMedicalHistoryDto dto = new PatientMedicalHistoryDto();
//        dto.setId(e.getId());
//        dto.setExternalId(e.getExternalId());
//        dto.setOrgId(e.getOrgId());
//        dto.setPatientId(e.getPatientId());
//        dto.setEncounterId(e.getEncounterId());
//        dto.setDescription(e.getDescription());
//
//        PatientMedicalHistoryDto.Audit audit = new PatientMedicalHistoryDto.Audit();
//        if (e.getCreatedAt() != null) {
//            audit.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
//        }
//        if (e.getUpdatedAt() != null) {
//            audit.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
//        }
//        dto.setAudit(audit);
//
//        return dto;
//    }
//}





package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.PatientMedicalHistoryDto;
import com.qiaben.ciyex.entity.PatientMedicalHistory;
import com.qiaben.ciyex.repository.PatientMedicalHistoryRepository;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.storage.StorageType;
import com.qiaben.ciyex.storage.fhir.FhirExternalPatientMedicalHistoryStorage;
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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class PatientMedicalHistoryService {
    public List<PatientMedicalHistoryDto> getAllByPatient(Long patientId) {
        return repo.findByPatientId(patientId)
            .stream().map(this::toDto).toList();
    }

    private final PatientMedicalHistoryRepository repo;
    private final EncounterService encounterService;
    private final com.qiaben.ciyex.repository.PatientRepository patientRepository;
    private final com.qiaben.ciyex.repository.EncounterRepository encounterRepository;

    @Autowired
    private ExternalStorageResolver storageResolver;

    @Autowired
    private OrgIntegrationConfigProvider configProvider;

    @Autowired(required = false)
    private FhirExternalPatientMedicalHistoryStorage fhirExternalPatientMedicalHistoryStorage;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Create
    public PatientMedicalHistoryDto create(Long patientId, Long encounterId, PatientMedicalHistoryDto dto) {
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

        // Step 4: Create the patient medical history
        PatientMedicalHistory e = new PatientMedicalHistory();
        e.setPatientId(patientId);
        e.setEncounterId(encounterId);
        applyDto(e, dto);
        e = repo.save(e);

        // Step 5: External sync
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        log.info("PatientMedicalHistory create - storageType for current org: {}", storageType);

        if (storageType != null) {
            try {
                if ("fhir".equals(storageType)) {
                    String externalId = fhirExternalPatientMedicalHistoryStorage.create(toDto(e));
                    if (externalId != null) {
                        e.setExternalId(externalId);
                        e.setFhirId(externalId);
                        e = repo.save(e);
                    }
                } else {
                    ExternalStorage<PatientMedicalHistoryDto> storage = storageResolver.resolve(PatientMedicalHistoryDto.class);
                    if (storage != null) {
                        String externalId = storage.create(toDto(e));
                        if (externalId != null) {
                            e.setExternalId(externalId);
                            e.setFhirId(externalId);
                            e = repo.save(e);
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("Failed to sync PMH to external storage", ex);
            }
        } else if (fhirExternalPatientMedicalHistoryStorage != null) {
            try {
                String externalId = fhirExternalPatientMedicalHistoryStorage.create(toDto(e));
                if (externalId != null) {
                    e.setExternalId(externalId);
                    e.setFhirId(externalId);
                    e = repo.save(e);
                }
            } catch (Exception ex) {
                log.warn("Failed to sync PMH to FHIR", ex);
            }
        } else {
            log.info("No external storage configured for PMH");
        }

        if (e.getExternalId() == null) {
            String generatedId = "PMH-" + System.currentTimeMillis();
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

    // Read one
    public PatientMedicalHistoryDto getOne(Long patientId, Long encounterId, Long id) {
        PatientMedicalHistory e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Patient Medical History not found with ID: %d for Patient ID: %d and Encounter ID: %d. Please verify all IDs are correct.",
                        id, patientId, encounterId)
                ));
        return toDto(e);
    }

    // List
    public List<PatientMedicalHistoryDto> list(Long patientId, Long encounterId) {
        return repo.findByPatientIdAndEncounterId(patientId, encounterId)
                .stream().map(this::toDto).toList();
    }

    // Update (blocked if signed)
    public PatientMedicalHistoryDto update(Long patientId, Long encounterId, Long id, PatientMedicalHistoryDto dto) {
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

        // Step 4: Find the patient medical history
        PatientMedicalHistory e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Patient Medical History not found with ID: %d for Patient ID: %d and Encounter ID: %d. Please verify all IDs are correct.",
                        id, patientId, encounterId)
                ));

        // Step 5: Check if entry itself is signed
        if (Boolean.TRUE.equals(e.getESigned())) {
            throw new IllegalStateException("Signed entries are read-only.");
        }

        // Step 6: Update the entry
        applyDto(e, dto);
        e = repo.save(e);

        // Step 7: External sync
        if (e.getExternalId() != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            try {
                if ("fhir".equals(storageType)) {
                    fhirExternalPatientMedicalHistoryStorage.update(toDto(e), e.getExternalId());
                } else {
                    ExternalStorage<PatientMedicalHistoryDto> storage = storageResolver.resolve(PatientMedicalHistoryDto.class);
                    if (storage != null) {
                        storage.update(toDto(e), e.getExternalId());
                    }
                }
            } catch (Exception ex) {
                log.warn("Failed to sync PMH update to external storage", ex);
            }
        }

        return toDto(e);
    }

    // Delete (blocked if signed)
    public void delete(Long patientId, Long encounterId, Long id) {
        // Step 1: Check if encounter is signed - prevent modification
        encounterService.validateEncounterNotSigned(encounterId, patientId);

        // Step 2: Find the patient medical history
        PatientMedicalHistory e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Patient Medical History not found with ID: %d for Patient ID: %d and Encounter ID: %d. Please verify all IDs are correct.",
                        id, patientId, encounterId)
                ));

        // Step 3: Check if entry itself is signed
        if (Boolean.TRUE.equals(e.getESigned())) {
            throw new IllegalStateException("Signed entries cannot be deleted.");
        }

        // Step 4: External sync
        if (e.getExternalId() != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            try {
                if ("fhir".equals(storageType)) {
                    fhirExternalPatientMedicalHistoryStorage.delete(e.getExternalId());
                } else {
                    ExternalStorage<PatientMedicalHistoryDto> storage = storageResolver.resolve(PatientMedicalHistoryDto.class);
                    if (storage != null) {
                        storage.delete(e.getExternalId());
                    }
                }
            } catch (Exception ex) {
                log.warn("Failed to sync PMH delete to external storage", ex);
            }
        }

        repo.delete(e);
    }

    // eSign (idempotent)
    public PatientMedicalHistoryDto eSign(Long patientId, Long encounterId, Long id, String signedBy) {
        PatientMedicalHistory e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Patient Medical History not found with ID: %d for Patient ID: %d and Encounter ID: %d. Please verify all IDs are correct.",
                        id, patientId, encounterId)
                ));
        if (Boolean.TRUE.equals(e.getESigned())) return toDto(e);

        e.setESigned(true);
        e.setSignedBy(StringUtils.hasText(signedBy) ? signedBy : "system");
        e.setSignedAt(java.time.OffsetDateTime.now(ZoneOffset.UTC));
        e = repo.save(e);
        return toDto(e);
    }

    // Print (PDF) — stamps printedAt
    public byte[] renderPdf(Long patientId, Long encounterId, Long id) {
        PatientMedicalHistory e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Patient Medical History not found with ID: %d for Patient ID: %d and Encounter ID: %d. Please verify all IDs are correct.",
                        id, patientId, encounterId)
                ));

        e.setPrintedAt(java.time.OffsetDateTime.now(ZoneOffset.UTC));
        repo.save(e);

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float x = 64, y = 740;

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.newLineAtOffset(x, y);
                cs.showText("Patient Medical History");
                cs.endText();

                y -= 26;
                draw(cs, x, y, "Patient ID:", String.valueOf(patientId)); y -= 16;
                draw(cs, x, y, "Encounter ID:", String.valueOf(encounterId)); y -= 16;
                draw(cs, x, y, "ID:", String.valueOf(id)); y -= 22;

                if (e.getConditionName() != null) { draw(cs, x, y, "Condition:", e.getConditionName()); y -= 16; }
                if (e.getStatus() != null) { draw(cs, x, y, "Status:", e.getStatus()); y -= 16; }
                if (e.getIsChronic() != null) { draw(cs, x, y, "Chronic:", e.getIsChronic() ? "Yes" : "No"); y -= 16; }
                if (e.getDiagnosisDate() != null) { draw(cs, x, y, "Diagnosis Date:", e.getDiagnosisDate().toString()); y -= 16; }
                if (e.getOnsetDate() != null) { draw(cs, x, y, "Onset Date:", e.getOnsetDate().toString()); y -= 16; }
                if (e.getResolvedDate() != null) { draw(cs, x, y, "Resolved Date:", e.getResolvedDate().toString()); y -= 16; }

                if (has(e.getDescription())) { draw(cs, x, y, "Description:", e.getDescription()); y -= 16; }
                if (has(e.getTreatmentDetails())) { draw(cs, x, y, "Treatment:", e.getTreatmentDetails()); y -= 16; }
                if (has(e.getDiagnosisDetails())) { draw(cs, x, y, "Diagnosis Details:", e.getDiagnosisDetails()); y -= 16; }
                if (has(e.getNotes())) { draw(cs, x, y, "Notes:", e.getNotes()); y -= 16; }

                y -= 8;
                draw(cs, x, y, "eSigned:", Boolean.TRUE.equals(e.getESigned()) ? "Yes" : "No"); y -= 16;
                if (e.getSignedAt() != null) { draw(cs, x, y, "Signed At:", e.getSignedAt().toString()); y -= 16; }
                if (has(e.getSignedBy())) { draw(cs, x, y, "Signed By:", e.getSignedBy()); y -= 16; }
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate Patient Medical History PDF", ex);
        }
    }

    // ----- helpers
    private static boolean has(String s) { return s != null && !s.isBlank(); }

    private static void draw(PDPageContentStream cs, float x, float y, String label, String value) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 12); cs.newLineAtOffset(x, y); cs.showText(label); cs.endText();
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x + 140, y); cs.showText(value != null ? value : "-"); cs.endText();
    }

    private PatientMedicalHistoryDto toDto(PatientMedicalHistory e) {
        PatientMedicalHistoryDto d = new PatientMedicalHistoryDto();
        d.setId(e.getId());
        d.setPatientId(e.getPatientId());
        d.setEncounterId(e.getEncounterId());
        d.setExternalId(e.getExternalId());
        d.setFhirId(e.getFhirId());

        d.setMedicalCondition(e.getMedicalCondition());
        d.setConditionName(e.getConditionName());
        d.setStatus(e.getStatus());
        d.setIsChronic(e.getIsChronic());

        d.setDiagnosisDate(e.getDiagnosisDate());
        d.setOnsetDate(e.getOnsetDate());
        d.setResolvedDate(e.getResolvedDate());

        d.setTreatmentDetails(e.getTreatmentDetails());
        d.setDiagnosisDetails(e.getDiagnosisDetails());
        d.setNotes(e.getNotes());
        d.setDescription(e.getDescription());

        d.setESigned(e.getESigned());
        d.setSignedAt(e.getSignedAt());
        d.setSignedBy(e.getSignedBy());
        d.setPrintedAt(e.getPrintedAt());

        PatientMedicalHistoryDto.Audit a = new PatientMedicalHistoryDto.Audit();
        if (e.getCreatedDate() != null) {
            a.setCreatedDate(DTF.format(e.getCreatedDate().atZone(java.time.ZoneId.systemDefault())));
        }
        if (e.getLastModifiedDate() != null) {
            a.setLastModifiedDate(DTF.format(e.getLastModifiedDate().atZone(java.time.ZoneId.systemDefault())));
        }
        d.setAudit(a);
        return d;
    }

    private void applyDto(PatientMedicalHistory e, PatientMedicalHistoryDto d) {
        e.setExternalId(d.getExternalId());
        e.setFhirId(d.getFhirId());
        e.setMedicalCondition(d.getMedicalCondition());
        e.setConditionName(d.getConditionName());
        e.setStatus(d.getStatus());
        e.setIsChronic(d.getIsChronic());

        e.setDiagnosisDate(d.getDiagnosisDate());
        e.setOnsetDate(d.getOnsetDate());
        e.setResolvedDate(d.getResolvedDate());
        e.setTreatmentDetails(d.getTreatmentDetails());
        e.setDiagnosisDetails(d.getDiagnosisDetails());
        e.setNotes(d.getNotes());
        e.setDescription(d.getDescription());
        // eSign fields managed only via eSign()
    }
}
