package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.dto.ReviewOfSystemDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Review of System Service - FHIR Only.
 * All ROS data is stored in HAPI FHIR server as Observation resources.
 */
@Service
@Slf4j
public class ReviewOfSystemService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;
    private final ObjectMapper objectMapper;

    // In-memory cache for e-sign/print metadata (keyed by FHIR ID)
    private final Map<String, SignMetadata> signMetadataCache = new ConcurrentHashMap<>();

    @Autowired
    public ReviewOfSystemService(FhirClientService fhirClientService, PracticeContextService practiceContextService, ObjectMapper objectMapper) {
        this.fhirClientService = fhirClientService;
        this.practiceContextService = practiceContextService;
        this.objectMapper = objectMapper;
    }

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // Helper class for e-sign metadata
    private static class SignMetadata {
        Boolean eSigned = false;
        OffsetDateTime signedAt;
        String signedBy;
        OffsetDateTime printedAt;
    }

    // ✅ Get all by patient
    public List<ReviewOfSystemDto> getAllByPatient(Long patientId) {
        log.debug("Getting FHIR Observations (ROS) for patient: {}", patientId);

        Bundle bundle = fhirClientService.getClient().search()
                .forResource(Observation.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .where(new TokenClientParam("category").exactly()
                        .systemAndCode("http://terminology.hl7.org/CodeSystem/observation-category", "survey"))
                .withAdditionalHeader("X-Request-Tenant-Id", getPracticeId())
                .returnBundle(Bundle.class)
                .execute();

        return extractRosDtos(bundle, patientId, null);
    }

    // ✅ Create ROS
    public ReviewOfSystemDto create(Long patientId, Long encounterId, ReviewOfSystemDto dto) {
        log.info("Creating ROS in FHIR for patient: {}, encounter: {}", patientId, encounterId);

        Observation observation = toFhirObservation(dto, patientId, encounterId);
        MethodOutcome outcome = fhirClientService.create(observation, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);

        log.info("Created FHIR Observation (ROS) with ID: {}", fhirId);
        return dto;
    }

    // ✅ List ROS for encounter
    public List<ReviewOfSystemDto> list(Long patientId, Long encounterId) {
        log.debug("Listing FHIR Observations (ROS) for patient: {}, encounter: {}", patientId, encounterId);

        Bundle bundle = fhirClientService.getClient().search()
                .forResource(Observation.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .where(new TokenClientParam("category").exactly()
                        .systemAndCode("http://terminology.hl7.org/CodeSystem/observation-category", "survey"))
                .withAdditionalHeader("X-Request-Tenant-Id", getPracticeId())
                .returnBundle(Bundle.class)
                .execute();

        return extractRosDtos(bundle, patientId, encounterId);
    }

    // ✅ Get one ROS
    public ReviewOfSystemDto getOne(Long patientId, Long encounterId, Long id) {
        String fhirId = String.valueOf(id);
        log.debug("Getting FHIR Observation (ROS) with ID: {}", fhirId);

        try {
            Observation observation = fhirClientService.read(Observation.class, fhirId, getPracticeId());
            return toRosDto(observation, patientId, encounterId);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Review of System not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id));
        }
    }

    // ✅ Update ROS
    public ReviewOfSystemDto update(Long patientId, Long encounterId, Long id, ReviewOfSystemDto dto) {
        String fhirId = String.valueOf(id);
        log.info("Updating FHIR Observation (ROS) with ID: {}", fhirId);

        // Check if signed
        SignMetadata meta = signMetadataCache.get(fhirId);
        if (meta != null && Boolean.TRUE.equals(meta.eSigned)) {
            throw new IllegalStateException("Signed ROS entries are read-only.");
        }

        Observation observation = toFhirObservation(dto, patientId, encounterId);
        observation.setId(fhirId);
        fhirClientService.update(observation, getPracticeId());

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        return dto;
    }

    // ✅ Delete ROS
    public void delete(Long patientId, Long encounterId, Long id) {
        String fhirId = String.valueOf(id);
        log.info("Deleting FHIR Observation (ROS) with ID: {}", fhirId);

        // Check if signed
        SignMetadata meta = signMetadataCache.get(fhirId);
        if (meta != null && Boolean.TRUE.equals(meta.eSigned)) {
            throw new IllegalStateException("Signed ROS entries cannot be deleted.");
        }

        fhirClientService.delete(Observation.class, fhirId, getPracticeId());
        signMetadataCache.remove(fhirId);
    }

    // ✅ eSign ROS
    public ReviewOfSystemDto eSign(Long patientId, Long encounterId, Long id, String signedBy) {
        String fhirId = String.valueOf(id);
        log.info("E-signing FHIR Observation (ROS) with ID: {}", fhirId);

        SignMetadata meta = signMetadataCache.computeIfAbsent(fhirId, k -> new SignMetadata());

        if (Boolean.TRUE.equals(meta.eSigned)) {
            return getOne(patientId, encounterId, id);
        }

        meta.eSigned = true;
        meta.signedBy = StringUtils.hasText(signedBy) ? signedBy : "system";
        meta.signedAt = OffsetDateTime.now(ZoneOffset.UTC);

        ReviewOfSystemDto dto = getOne(patientId, encounterId, id);
        dto.setESigned(meta.eSigned);
        dto.setSignedAt(meta.signedAt);
        dto.setSignedBy(meta.signedBy);

        return dto;
    }

    // ✅ Render PDF
    public byte[] renderPdf(Long patientId, Long encounterId, Long id) {
        String fhirId = String.valueOf(id);
        log.info("Rendering PDF for FHIR Observation (ROS) with ID: {}", fhirId);

        ReviewOfSystemDto dto = getOne(patientId, encounterId, id);

        // Update print timestamp
        SignMetadata meta = signMetadataCache.computeIfAbsent(fhirId, k -> new SignMetadata());
        meta.printedAt = OffsetDateTime.now(ZoneOffset.UTC);

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
                draw(cs, x, y, "ROS ID:", fhirId); y -= 20;

                // eSign footer
                y -= 10;
                draw(cs, x, y, "eSigned:", Boolean.TRUE.equals(meta.eSigned) ? "Yes" : "No"); y -= 16;
                if (meta.signedAt != null) { draw(cs, x, y, "Signed At:", meta.signedAt.toString()); y -= 16; }
                if (StringUtils.hasText(meta.signedBy)) { draw(cs, x, y, "Signed By:", meta.signedBy); y -= 16; }
                if (meta.printedAt != null) { draw(cs, x, y, "Printed At:", meta.printedAt.toString()); y -= 16; }
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate ROS PDF", ex);
        }
    }

    // ========== FHIR Mapping Methods ==========

    private Observation toFhirObservation(ReviewOfSystemDto dto, Long patientId, Long encounterId) {
        Observation observation = new Observation();

        // Patient reference
        observation.setSubject(new Reference("Patient/" + patientId));

        // Encounter reference
        if (encounterId != null) {
            observation.setEncounter(new Reference("Encounter/" + encounterId));
        }

        // Status
        observation.setStatus(Observation.ObservationStatus.FINAL);

        // Category: survey
        observation.addCategory()
                .addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
                .setCode("survey")
                .setDisplay("Survey");

        // Code
        observation.setCode(new CodeableConcept().setText("Review of Systems"));

        // Store ROS data as JSON in note
        try {
            String rosJson = objectMapper.writeValueAsString(dto);
            observation.addNote().setText(rosJson);
        } catch (Exception ex) {
            log.error("Failed to serialize ROS data", ex);
        }

        return observation;
    }

    private ReviewOfSystemDto toRosDto(Observation observation, Long patientId, Long encounterId) {
        ReviewOfSystemDto dto = new ReviewOfSystemDto();

        if (observation.hasId()) {
            dto.setFhirId(observation.getIdElement().getIdPart());
            dto.setExternalId(observation.getIdElement().getIdPart());
        }

        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);

        // Parse ROS data from note
        if (observation.hasNote()) {
            String rosJson = observation.getNoteFirstRep().getText();
            if (StringUtils.hasText(rosJson)) {
                try {
                    ReviewOfSystemDto temp = objectMapper.readValue(rosJson, ReviewOfSystemDto.class);
                    dto.setConstitutional(temp.getConstitutional());
                    dto.setEyes(temp.getEyes());
                    dto.setEnt(temp.getEnt());
                    dto.setNeck(temp.getNeck());
                    dto.setCardiovascular(temp.getCardiovascular());
                    dto.setRespiratory(temp.getRespiratory());
                    dto.setGastrointestinal(temp.getGastrointestinal());
                    dto.setGenitourinaryMale(temp.getGenitourinaryMale());
                    dto.setGenitourinaryFemale(temp.getGenitourinaryFemale());
                    dto.setMusculoskeletal(temp.getMusculoskeletal());
                    dto.setSkin(temp.getSkin());
                    dto.setNeurologic(temp.getNeurologic());
                    dto.setPsychiatric(temp.getPsychiatric());
                    dto.setEndocrine(temp.getEndocrine());
                    dto.setHematologicLymphatic(temp.getHematologicLymphatic());
                    dto.setAllergicImmunologic(temp.getAllergicImmunologic());
                } catch (Exception ex) {
                    log.error("Failed to deserialize ROS data", ex);
                }
            }
        }

        // Check sign metadata
        String fhirId = dto.getFhirId();
        if (fhirId != null) {
            SignMetadata meta = signMetadataCache.get(fhirId);
            if (meta != null) {
                dto.setESigned(meta.eSigned);
                dto.setSignedAt(meta.signedAt);
                dto.setSignedBy(meta.signedBy);
                dto.setPrintedAt(meta.printedAt);
            }
        }

        return dto;
    }

    private List<ReviewOfSystemDto> extractRosDtos(Bundle bundle, Long patientId, Long encounterId) {
        List<ReviewOfSystemDto> items = new ArrayList<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Observation) {
                    items.add(toRosDto((Observation) entry.getResource(), patientId, encounterId));
                }
            }
        }
        return items;
    }

    // ========== PDF Helpers ==========

    private static void draw(PDPageContentStream cs, float x, float y, String label, String value) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 12); cs.newLineAtOffset(x, y); cs.showText(label); cs.endText();
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x + 140, y); cs.showText(value != null ? value : "-"); cs.endText();
    }
}
