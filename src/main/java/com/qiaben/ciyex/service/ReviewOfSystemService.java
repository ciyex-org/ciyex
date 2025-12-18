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
//
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.dto.ReviewOfSystemDto;
import com.qiaben.ciyex.entity.ReviewOfSystem;
import com.qiaben.ciyex.repository.ReviewOfSystemRepository;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.storage.ExternalStorage;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewOfSystemService {
    public List<ReviewOfSystemDto> getAllByPatient(Long patientId) {
        return repo.findByPatientId(patientId)
            .stream().map(this::toDto).toList();
    }

    private final ReviewOfSystemRepository repo;
    private final com.qiaben.ciyex.repository.PatientRepository patientRepository;
    private final com.qiaben.ciyex.repository.EncounterRepository encounterRepository;
    private final EncounterService encounterService;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private com.qiaben.ciyex.storage.fhir.FhirExternalReviewOfSystemStorage fhirStorage;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // CREATE
    public ReviewOfSystemDto create(Long patientId, Long encounterId, ReviewOfSystemDto dto) {
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
        // Step 4: Create the review of system
        ReviewOfSystem e = new ReviewOfSystem();
        e.setPatientId(patientId);
        e.setEncounterId(encounterId);
        applyDto(e, dto);
        e = repo.save(e);
        
        // Step 5: Optional external FHIR sync
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        log.info("ReviewOfSystem create - storageType for current org: {}", storageType);

        if (storageType != null) {
            try {
                log.info("Attempting FHIR sync for ReviewOfSystem ID: {}", e.getId());
                ExternalStorage<ReviewOfSystemDto> ext = storageResolver.resolve(ReviewOfSystemDto.class);
                log.info("Resolved external storage: {}", ext.getClass().getName());

                ReviewOfSystemDto snapshot = toDto(e);
                String externalId = ext.create(snapshot);
                log.info("FHIR create returned externalId: {}", externalId);

                if (externalId != null && !externalId.isEmpty()) {
                    e.setExternalId(externalId);
                    e = repo.save(e);
                    log.info("Created FHIR resource for ReviewOfSystem ID: {} with externalId: {}", e.getId(), externalId);
                } else {
                    log.warn("FHIR create returned null or empty externalId for ReviewOfSystem ID: {}", e.getId());
                }
            } catch (Exception ex) {
                log.error("Failed to sync ReviewOfSystem to external storage", ex);
            }
        } else if (fhirStorage != null) {
            try {
                log.info("No storage type configured, falling back to direct FHIR storage for ReviewOfSystem ID: {}", e.getId());
                ReviewOfSystemDto snapshot = toDto(e);
                String externalId = fhirStorage.create(snapshot);
                log.info("FHIR fallback create returned externalId: {}", externalId);

                if (externalId != null && !externalId.isEmpty()) {
                    e.setExternalId(externalId);
                    e = repo.save(e);
                    log.info("Created FHIR resource (fallback) for ReviewOfSystem ID: {} with externalId: {}", e.getId(), externalId);
                }
            } catch (Exception ex) {
                log.error("Failed to sync ReviewOfSystem to external storage (fallback)", ex);
            }
        }
        
        if (e.getExternalId() == null) {
            String generatedId = "ROS-" + System.currentTimeMillis();
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
        // Step 4: Find the review of system
        ReviewOfSystem e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Review of System not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id)
                ));
        if (Boolean.TRUE.equals(e.getESigned())) {
            throw new IllegalStateException("Signed ROS entries are read-only.");
        }
        applyDto(e, dto);
        e = repo.save(e);

        // Step 7: Optional external FHIR sync
        if (e.getExternalId() != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            log.info("ReviewOfSystem update - storageType for current org: {}", storageType);

            if (storageType != null) {
                try {
                    log.info("Attempting FHIR sync for ReviewOfSystem ID: {}", e.getId());
                    ExternalStorage<ReviewOfSystemDto> ext = storageResolver.resolve(ReviewOfSystemDto.class);
                    log.info("Resolved external storage: {}", ext.getClass().getName());

                    ReviewOfSystemDto snapshot = toDto(e);
                    ext.update(snapshot, e.getExternalId());
                    log.info("Updated FHIR resource for ReviewOfSystem ID: {} with externalId: {}", e.getId(), e.getExternalId());
                } catch (Exception ex) {
                    log.error("Failed to sync ReviewOfSystem update to external storage", ex);
                }
            } else if (fhirStorage != null) {
                try {
                    log.info("No storage type configured, falling back to direct FHIR storage for ReviewOfSystem ID: {}", e.getId());
                    ReviewOfSystemDto snapshot = toDto(e);
                    fhirStorage.update(snapshot, e.getExternalId());
                    log.info("Updated FHIR resource (fallback) for ReviewOfSystem ID: {} with externalId: {}", e.getId(), e.getExternalId());
                } catch (Exception ex) {
                    log.error("Failed to sync ReviewOfSystem update to external storage (fallback)", ex);
                }
            }
        }

        return toDto(e);
    }

    // DELETE (locked if signed)
    public void delete(Long patientId, Long encounterId, Long id) {
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
        // Step 4: Find the review of system
        ReviewOfSystem e = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Review of System not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id)
                ));
        if (Boolean.TRUE.equals(e.getESigned())) {
            throw new IllegalStateException("Signed ROS entries cannot be deleted.");
        }

        // Step 5: Optional external FHIR sync
        if (e.getExternalId() != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            log.info("ReviewOfSystem delete - storageType for current org: {}", storageType);

            if (storageType != null) {
                try {
                    log.info("Attempting FHIR sync for ReviewOfSystem ID: {}", e.getId());
                    ExternalStorage<ReviewOfSystemDto> ext = storageResolver.resolve(ReviewOfSystemDto.class);
                    log.info("Resolved external storage: {}", ext.getClass().getName());

                    ext.delete(e.getExternalId());
                    log.info("Deleted FHIR resource for ReviewOfSystem ID: {} with externalId: {}", e.getId(), e.getExternalId());
                } catch (Exception ex) {
                    log.error("Failed to sync ReviewOfSystem delete to external storage", ex);
                }
            } else if (fhirStorage != null) {
                try {
                    log.info("No storage type configured, falling back to direct FHIR storage for ReviewOfSystem ID: {}", e.getId());
                    fhirStorage.delete(e.getExternalId());
                    log.info("Deleted FHIR resource (fallback) for ReviewOfSystem ID: {} with externalId: {}", e.getId(), e.getExternalId());
                } catch (Exception ex) {
                    log.error("Failed to sync ReviewOfSystem delete to external storage (fallback)", ex);
                }
            }
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

        ReviewOfSystemDto dto = toDto(e);

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
                draw(cs, x, y, "ROS ID:", String.valueOf(id)); y -= 20;

                // Render each system category
                y = renderSystemCategory(cs, x, y, "Constitutional", dto.getConstitutional());
                y = renderSystemCategory(cs, x, y, "Eyes", dto.getEyes());
                y = renderSystemCategory(cs, x, y, "ENT", dto.getEnt());
                y = renderSystemCategory(cs, x, y, "Neck", dto.getNeck());
                y = renderSystemCategory(cs, x, y, "Cardiovascular", dto.getCardiovascular());
                y = renderSystemCategory(cs, x, y, "Respiratory", dto.getRespiratory());
                y = renderSystemCategory(cs, x, y, "Gastrointestinal", dto.getGastrointestinal());
                y = renderSystemCategory(cs, x, y, "Genitourinary (Male)", dto.getGenitourinaryMale());
                y = renderSystemCategory(cs, x, y, "Genitourinary (Female)", dto.getGenitourinaryFemale());
                y = renderSystemCategory(cs, x, y, "Musculoskeletal", dto.getMusculoskeletal());
                y = renderSystemCategory(cs, x, y, "Skin", dto.getSkin());
                y = renderSystemCategory(cs, x, y, "Neurologic", dto.getNeurologic());
                y = renderSystemCategory(cs, x, y, "Psychiatric", dto.getPsychiatric());
                y = renderSystemCategory(cs, x, y, "Endocrine", dto.getEndocrine());
                y = renderSystemCategory(cs, x, y, "Hematologic/Lymphatic", dto.getHematologicLymphatic());
                y = renderSystemCategory(cs, x, y, "Allergic/Immunologic", dto.getAllergicImmunologic());

                // eSign footer
                y -= 10;
                draw(cs, x, y, "eSigned:", Boolean.TRUE.equals(e.getESigned()) ? "Yes" : "No"); y -= 16;
                if (e.getSignedAt() != null) { draw(cs, x, y, "Signed At:", e.getSignedAt().toString()); y -= 16; }
                if (StringUtils.hasText(e.getSignedBy())) { draw(cs, x, y, "Signed By:", e.getSignedBy()); }
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate ROS PDF", ex);
        }
    }

    private float renderSystemCategory(PDPageContentStream cs, float x, float y, String categoryName, Object category) throws IOException {
        if (category == null) return y;

        cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(categoryName);
        cs.endText();
        y -= 14;

        // Use reflection to get all boolean fields and note
        try {
            java.lang.reflect.Field[] fields = category.getClass().getDeclaredFields();
            List<String> positiveSymptoms = new ArrayList<>();
            String note = null;

            for (java.lang.reflect.Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(category);
                
                if (field.getName().equals("note") && value instanceof String) {
                    note = (String) value;
                } else if (value instanceof Boolean && Boolean.TRUE.equals(value)) {
                    // Convert camelCase to readable format
                    String symptomName = field.getName().replaceAll("([A-Z])", " $1").trim();
                    symptomName = symptomName.substring(0, 1).toUpperCase() + symptomName.substring(1);
                    positiveSymptoms.add(symptomName);
                }
            }

            if (!positiveSymptoms.isEmpty()) {
                cs.setFont(PDType1Font.HELVETICA, 10);
                for (String symptom : positiveSymptoms) {
                    cs.beginText();
                    cs.newLineAtOffset(x + 10, y);
                    cs.showText("+ " + symptom);
                    cs.endText();
                    y -= 12;
                }
            } else {
                cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 10);
                cs.beginText();
                cs.newLineAtOffset(x + 10, y);
                cs.showText("All Negative");
                cs.endText();
                y -= 12;
            }

            if (StringUtils.hasText(note)) {
                cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 9);
                cs.beginText();
                cs.newLineAtOffset(x + 10, y);
                cs.showText("Note: " + note);
                cs.endText();
                y -= 12;
            }
        } catch (Exception ex) {
            log.error("Error rendering system category", ex);
        }

        y -= 6; // spacing between categories
        return y;
    }

    // ----- helpers
    private static void draw(PDPageContentStream cs, float x, float y, String label, String value) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 12); cs.newLineAtOffset(x, y); cs.showText(label); cs.endText();
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x + 140, y); cs.showText(value != null ? value : "-"); cs.endText();
    }

    private ReviewOfSystemDto toDto(ReviewOfSystem e) {
        ReviewOfSystemDto d = new ReviewOfSystemDto();
        d.setId(e.getId());
        d.setExternalId(e.getExternalId());
        d.setFhirId(e.getFhirId());
        d.setPatientId(e.getPatientId());
        d.setEncounterId(e.getEncounterId());

        // Deserialize JSON data
        if (StringUtils.hasText(e.getRosData())) {
            try {
                ReviewOfSystemDto temp = objectMapper.readValue(e.getRosData(), ReviewOfSystemDto.class);
                d.setConstitutional(temp.getConstitutional());
                d.setEyes(temp.getEyes());
                d.setEnt(temp.getEnt());
                d.setNeck(temp.getNeck());
                d.setCardiovascular(temp.getCardiovascular());
                d.setRespiratory(temp.getRespiratory());
                d.setGastrointestinal(temp.getGastrointestinal());
                d.setGenitourinaryMale(temp.getGenitourinaryMale());
                d.setGenitourinaryFemale(temp.getGenitourinaryFemale());
                d.setMusculoskeletal(temp.getMusculoskeletal());
                d.setSkin(temp.getSkin());
                d.setNeurologic(temp.getNeurologic());
                d.setPsychiatric(temp.getPsychiatric());
                d.setEndocrine(temp.getEndocrine());
                d.setHematologicLymphatic(temp.getHematologicLymphatic());
                d.setAllergicImmunologic(temp.getAllergicImmunologic());
            } catch (Exception ex) {
                log.error("Failed to deserialize ROS data", ex);
            }
        }

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
        try {
            // Serialize the entire DTO (excluding metadata) to JSON
            String json = objectMapper.writeValueAsString(d);
            e.setRosData(json);
        } catch (Exception ex) {
            log.error("Failed to serialize ROS data", ex);
            throw new RuntimeException("Failed to save ROS data", ex);
        }
        // eSign fields managed only via eSign()
    }
}
