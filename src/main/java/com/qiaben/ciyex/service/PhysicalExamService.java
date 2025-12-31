package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import com.qiaben.ciyex.dto.PhysicalExamDto;
import com.qiaben.ciyex.dto.PhysicalExamSectionDto;
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
 * Physical Exam Service - FHIR Only.
 * All physical exam data is stored in HAPI FHIR server as Observation resources.
 */
@Service
@Slf4j
public class PhysicalExamService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    // In-memory cache for e-sign/print metadata (keyed by FHIR ID)
    private final Map<String, SignMetadata> signMetadataCache = new ConcurrentHashMap<>();

    @Autowired
    public PhysicalExamService(FhirClientService fhirClientService, PracticeContextService practiceContextService) {
        this.fhirClientService = fhirClientService;
        this.practiceContextService = practiceContextService;
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
    public List<PhysicalExamDto> getAllByPatient(Long patientId) {
        log.debug("Getting FHIR Observations (Physical Exam) for patient: {}", patientId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Observation.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .where(new TokenClientParam("category").exactly()
                        .systemAndCode("http://terminology.hl7.org/CodeSystem/observation-category", "exam"))
                
                .returnBundle(Bundle.class)
                .execute();

        return extractPhysicalExamDtos(bundle, patientId, null);
    }

    // ✅ Create Physical Exam
    public PhysicalExamDto create(Long patientId, Long encounterId, PhysicalExamDto dto) {
        log.info("Creating Physical Exam in FHIR for patient: {}, encounter: {}", patientId, encounterId);

        Observation observation = toFhirObservation(dto, patientId, encounterId);
        MethodOutcome outcome = fhirClientService.create(observation, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);

        log.info("Created FHIR Observation (Physical Exam) with ID: {}", fhirId);
        return dto;
    }

    // ✅ List Physical Exams for encounter
    public List<PhysicalExamDto> list(Long patientId, Long encounterId) {
        log.debug("Listing FHIR Observations (Physical Exam) for patient: {}, encounter: {}", patientId, encounterId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Observation.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .where(new TokenClientParam("category").exactly()
                        .systemAndCode("http://terminology.hl7.org/CodeSystem/observation-category", "exam"))
                
                .returnBundle(Bundle.class)
                .execute();

        return extractPhysicalExamDtos(bundle, patientId, encounterId);
    }

    // ✅ Get one Physical Exam
    public PhysicalExamDto getOne(Long patientId, Long encounterId, Long id) {
        String fhirId = String.valueOf(id);
        log.debug("Getting FHIR Observation (Physical Exam) with ID: {}", fhirId);

        try {
            Observation observation = fhirClientService.read(Observation.class, fhirId, getPracticeId());
            return toPhysicalExamDto(observation, patientId, encounterId);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Physical Exam not found with ID: %d for Patient ID: %d and Encounter ID: %d",
                            id, patientId, encounterId));
        }
    }

    // ✅ Update Physical Exam
    public PhysicalExamDto update(Long patientId, Long encounterId, Long id, PhysicalExamDto dto) {
        String fhirId = String.valueOf(id);
        log.info("Updating FHIR Observation (Physical Exam) with ID: {}", fhirId);

        // Check if signed
        SignMetadata meta = signMetadataCache.get(fhirId);
        if (meta != null && Boolean.TRUE.equals(meta.eSigned)) {
            throw new IllegalStateException("Signed physical exams are read-only.");
        }

        Observation observation = toFhirObservation(dto, patientId, encounterId);
        observation.setId(fhirId);
        fhirClientService.update(observation, getPracticeId());

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        return dto;
    }

    // ✅ Delete Physical Exam
    public void delete(Long patientId, Long encounterId, Long id) {
        String fhirId = String.valueOf(id);
        log.info("Deleting FHIR Observation (Physical Exam) with ID: {}", fhirId);

        // Check if signed
        SignMetadata meta = signMetadataCache.get(fhirId);
        if (meta != null && Boolean.TRUE.equals(meta.eSigned)) {
            throw new IllegalStateException("Signed physical exams cannot be deleted.");
        }

        fhirClientService.delete(Observation.class, fhirId, getPracticeId());
        signMetadataCache.remove(fhirId);
    }

    // ✅ eSign Physical Exam
    public PhysicalExamDto eSign(Long patientId, Long encounterId, Long id, String signedBy) {
        String fhirId = String.valueOf(id);
        log.info("E-signing FHIR Observation (Physical Exam) with ID: {}", fhirId);

        SignMetadata meta = signMetadataCache.computeIfAbsent(fhirId, k -> new SignMetadata());

        if (Boolean.TRUE.equals(meta.eSigned)) {
            return getOne(patientId, encounterId, id);
        }

        meta.eSigned = true;
        meta.signedBy = StringUtils.hasText(signedBy) ? signedBy : "system";
        meta.signedAt = OffsetDateTime.now(ZoneOffset.UTC);

        PhysicalExamDto dto = getOne(patientId, encounterId, id);
        dto.setESigned(meta.eSigned);
        dto.setSignedAt(meta.signedAt);
        dto.setSignedBy(meta.signedBy);

        return dto;
    }

    // ✅ Render PDF
    public byte[] renderPdf(Long patientId, Long encounterId, Long id) {
        String fhirId = String.valueOf(id);
        log.info("Rendering PDF for FHIR Observation (Physical Exam) with ID: {}", fhirId);

        PhysicalExamDto dto = getOne(patientId, encounterId, id);

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
                cs.showText("Physical Examination");
                cs.endText();

                // Meta
                y -= 26;
                draw(cs, x, y, "Patient ID:", String.valueOf(patientId)); y -= 16;
                draw(cs, x, y, "Encounter ID:", String.valueOf(encounterId)); y -= 16;
                draw(cs, x, y, "Record ID:", fhirId); y -= 22;

                // Sections
                if (dto.getSections() != null) {
                    for (PhysicalExamSectionDto s : dto.getSections()) {
                        draw(cs, x, y, "Section:", s.getSectionKey()); y -= 16;
                        draw(cs, x, y, "Status:", Boolean.TRUE.equals(s.getAllNormal()) ? "All normal" : "Abnormal"); y -= 16;
                        if (StringUtils.hasText(s.getNormalText())) { draw(cs, x, y, "Normal Text:", s.getNormalText()); y -= 16; }
                        if (StringUtils.hasText(s.getFindings())) { draw(cs, x, y, "Findings:", s.getFindings()); y -= 16; }
                        y -= 8;
                    }
                }

                // eSign footer
                y -= 8;
                draw(cs, x, y, "eSigned:", Boolean.TRUE.equals(meta.eSigned) ? "Yes" : "No"); y -= 16;
                if (meta.signedAt != null) { draw(cs, x, y, "Signed At:", meta.signedAt.toString()); y -= 16; }
                if (StringUtils.hasText(meta.signedBy)) { draw(cs, x, y, "Signed By:", meta.signedBy); y -= 16; }
                if (meta.printedAt != null) { draw(cs, x, y, "Printed At:", meta.printedAt.toString()); y -= 16; }
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate Physical Exam PDF", ex);
        }
    }

    // ========== FHIR Mapping Methods ==========

    private Observation toFhirObservation(PhysicalExamDto dto, Long patientId, Long encounterId) {
        Observation observation = new Observation();

        // Patient reference
        observation.setSubject(new Reference("Patient/" + patientId));

        // Encounter reference
        if (encounterId != null) {
            observation.setEncounter(new Reference("Encounter/" + encounterId));
        }

        // Status
        observation.setStatus(Observation.ObservationStatus.FINAL);

        // Category: exam
        observation.addCategory()
                .addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
                .setCode("exam")
                .setDisplay("Exam");

        // Code
        observation.setCode(new CodeableConcept().setText("Physical Examination"));

        // Sections as components
        if (dto.getSections() != null) {
            for (PhysicalExamSectionDto section : dto.getSections()) {
                Observation.ObservationComponentComponent component = observation.addComponent();
                component.setCode(new CodeableConcept().setText(section.getSectionKey()));

                StringBuilder valueText = new StringBuilder();
                if (Boolean.TRUE.equals(section.getAllNormal())) {
                    valueText.append("All Normal");
                    if (StringUtils.hasText(section.getNormalText())) {
                        valueText.append(": ").append(section.getNormalText());
                    }
                } else if (StringUtils.hasText(section.getFindings())) {
                    valueText.append("Findings: ").append(section.getFindings());
                }
                component.setValue(new StringType(valueText.toString()));
            }
        }

        return observation;
    }

    private PhysicalExamDto toPhysicalExamDto(Observation observation, Long patientId, Long encounterId) {
        PhysicalExamDto dto = new PhysicalExamDto();

        if (observation.hasId()) {
            dto.setFhirId(observation.getIdElement().getIdPart());
            dto.setExternalId(observation.getIdElement().getIdPart());
        }

        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);

        // Parse sections from components
        List<PhysicalExamSectionDto> sections = new ArrayList<>();
        if (observation.hasComponent()) {
            for (Observation.ObservationComponentComponent component : observation.getComponent()) {
                PhysicalExamSectionDto section = new PhysicalExamSectionDto();
                if (component.hasCode() && component.getCode().hasText()) {
                    section.setSectionKey(component.getCode().getText());
                }
                if (component.hasValueStringType()) {
                    String value = component.getValueStringType().getValue();
                    if (value != null && value.startsWith("All Normal")) {
                        section.setAllNormal(true);
                        if (value.contains(": ")) {
                            section.setNormalText(value.substring(value.indexOf(": ") + 2));
                        }
                    } else if (value != null && value.startsWith("Findings: ")) {
                        section.setAllNormal(false);
                        section.setFindings(value.substring(10));
                    }
                }
                sections.add(section);
            }
        }
        dto.setSections(sections);

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

    private List<PhysicalExamDto> extractPhysicalExamDtos(Bundle bundle, Long patientId, Long encounterId) {
        List<PhysicalExamDto> items = new ArrayList<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Observation) {
                    items.add(toPhysicalExamDto((Observation) entry.getResource(), patientId, encounterId));
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
