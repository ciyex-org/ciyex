package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import com.qiaben.ciyex.dto.PlanDto;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plan Service - FHIR Only.
 * All plan data is stored in HAPI FHIR server as CarePlan resources.
 */
@Service
@Slf4j
public class PlanService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    // In-memory cache for e-sign/print metadata (keyed by FHIR ID)
    private final Map<String, SignMetadata> signMetadataCache = new ConcurrentHashMap<>();

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Autowired
    public PlanService(FhirClientService fhirClientService, PracticeContextService practiceContextService) {
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
    public List<PlanDto> getAllByPatient(Long patientId) {
        validatePathVariable(patientId, "Patient ID");
        log.debug("Getting FHIR CarePlans for patient: {}", patientId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(CarePlan.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                
                .returnBundle(Bundle.class)
                .execute();

        return extractPlanDtos(bundle, patientId, null);
    }

    // ✅ Create Plan
    public PlanDto create(Long patientId, Long encounterId, PlanDto dto) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        log.info("Creating Plan in FHIR for patient: {}, encounter: {}", patientId, encounterId);

        CarePlan carePlan = toFhirCarePlan(dto, patientId, encounterId);
        MethodOutcome outcome = fhirClientService.create(carePlan, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setId(Long.parseLong(fhirId));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);
        
        CarePlan created = (CarePlan) outcome.getResource();
        if (created != null && created.hasMeta()) {
            populateAudit(dto, created.getMeta());
        }

        log.info("Created FHIR CarePlan with ID: {}", fhirId);
        return dto;
    }

    // ✅ List Plans for encounter
    public List<PlanDto> list(Long patientId, Long encounterId) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        log.debug("Listing FHIR CarePlans for patient: {}, encounter: {}", patientId, encounterId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(CarePlan.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                
                .returnBundle(Bundle.class)
                .execute();

        return extractPlanDtos(bundle, patientId, encounterId);
    }

    // ✅ Get one Plan
    public PlanDto getOne(Long patientId, Long encounterId, Long id) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validatePathVariable(id, "Plan ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        String fhirId = String.valueOf(id);
        log.debug("Getting FHIR CarePlan with ID: {}", fhirId);

        try {
            CarePlan carePlan = fhirClientService.read(CarePlan.class, fhirId, getPracticeId());
            PlanDto dto = toPlanDto(carePlan, patientId, encounterId);
            dto.setId(id);
            return dto;
        } catch (Exception e) {
            throw new IllegalArgumentException("Plan ID is invalid. Plan not found: " + id);
        }
    }

    // ✅ Update Plan
    public PlanDto update(Long patientId, Long encounterId, Long id, PlanDto dto) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validatePathVariable(id, "Plan ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        String fhirId = String.valueOf(id);
        log.info("Updating FHIR CarePlan with ID: {}", fhirId);

        try {
            fhirClientService.read(CarePlan.class, fhirId, getPracticeId());
        } catch (Exception e) {
            throw new IllegalArgumentException("Plan ID is invalid. Plan not found: " + id);
        }

        SignMetadata meta = signMetadataCache.get(fhirId);
        if (meta != null && Boolean.TRUE.equals(meta.eSigned)) {
            throw new IllegalStateException("Signed plan is read-only.");
        }

        CarePlan carePlan = toFhirCarePlan(dto, patientId, encounterId);
        carePlan.setId(fhirId);
        fhirClientService.update(carePlan, getPracticeId());

        return getOne(patientId, encounterId, id);
    }

    // ✅ Delete Plan
    public String delete(Long patientId, Long encounterId, Long id) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validatePathVariable(id, "Plan ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        String fhirId = String.valueOf(id);
        log.info("Deleting FHIR CarePlan with ID: {}", fhirId);

        try {
            fhirClientService.read(CarePlan.class, fhirId, getPracticeId());
        } catch (Exception e) {
            throw new IllegalArgumentException("Plan ID is invalid. Plan not found: " + id);
        }

        SignMetadata meta = signMetadataCache.get(fhirId);
        if (meta != null && Boolean.TRUE.equals(meta.eSigned)) {
            throw new IllegalStateException("Signed plan cannot be deleted.");
        }

        fhirClientService.delete(CarePlan.class, fhirId, getPracticeId());
        signMetadataCache.remove(fhirId);
        
        return "Plan deleted successfully";
    }

    // ✅ eSign Plan
    public PlanDto eSign(Long patientId, Long encounterId, Long id, String signedBy) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validatePathVariable(id, "Plan ID");
        String fhirId = String.valueOf(id);
        log.info("E-signing FHIR CarePlan with ID: {}", fhirId);

        SignMetadata meta = signMetadataCache.computeIfAbsent(fhirId, k -> new SignMetadata());

        if (Boolean.TRUE.equals(meta.eSigned)) {
            return getOne(patientId, encounterId, id);
        }

        meta.eSigned = true;
        meta.signedBy = StringUtils.hasText(signedBy) ? signedBy : "system";
        meta.signedAt = OffsetDateTime.now(ZoneOffset.UTC);

        PlanDto dto = getOne(patientId, encounterId, id);
        dto.setESigned(meta.eSigned);
        dto.setSignedAt(meta.signedAt != null ? meta.signedAt.format(ISO) : null);
        dto.setSignedBy(meta.signedBy);

        return dto;
    }

    // ✅ Render PDF
    public byte[] renderPdf(Long patientId, Long encounterId, Long id) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validatePathVariable(id, "Plan ID");
        String fhirId = String.valueOf(id);
        log.info("Rendering PDF for FHIR CarePlan with ID: {}", fhirId);

        PlanDto dto = getOne(patientId, encounterId, id);

        // Update print timestamp
        SignMetadata meta = signMetadataCache.computeIfAbsent(fhirId, k -> new SignMetadata());
        meta.printedAt = OffsetDateTime.now(ZoneOffset.UTC);

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float x = 50, y = 750;
                float labelWidth = 150;

                // Title
                title(cs, x, y, "Encounter Plan"); y -= 30;
                
                // Metadata section
                row(cs, x, y, labelWidth, "Patient ID:", String.valueOf(patientId)); y -= 18;
                row(cs, x, y, labelWidth, "Encounter ID:", String.valueOf(encounterId)); y -= 18;
                row(cs, x, y, labelWidth, "Plan ID:", fhirId); y -= 25;

                // FREE-TEXT BLOCKS with proper alignment
                y = blockAligned(cs, x, y, labelWidth, "Diagnostic Plan:", dto.getDiagnosticPlan());
                y = blockAligned(cs, x, y, labelWidth, "Plan:", dto.getPlan());
                y = blockAligned(cs, x, y, labelWidth, "Notes:", dto.getNotes());

                // Additional fields
                if (StringUtils.hasText(dto.getFollowUpVisit())) {
                    row(cs, x, y, labelWidth, "Follow-Up Visit:", dto.getFollowUpVisit()); y -= 18;
                }
                if (StringUtils.hasText(dto.getReturnWorkSchool())) {
                    row(cs, x, y, labelWidth, "Return Work/School:", dto.getReturnWorkSchool()); y -= 18;
                }

                // Signature section
                y -= 15;
                row(cs, x, y, labelWidth, "eSigned:", Boolean.TRUE.equals(meta.eSigned) ? "Yes" : "No"); y -= 18;
                if (meta.signedAt != null) { row(cs, x, y, labelWidth, "Signed At:", meta.signedAt.format(ISO)); y -= 18; }
                if (StringUtils.hasText(meta.signedBy)) { row(cs, x, y, labelWidth, "Signed By:", meta.signedBy); y -= 18; }
                if (meta.printedAt != null) { row(cs, x, y, labelWidth, "Printed At:", meta.printedAt.format(ISO)); y -= 18; }
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate Plan PDF", ex);
        }
    }

    // ========== FHIR Mapping Methods ==========

    private CarePlan toFhirCarePlan(PlanDto dto, Long patientId, Long encounterId) {
        CarePlan carePlan = new CarePlan();

        // Patient reference
        carePlan.setSubject(new Reference("Patient/" + patientId));

        // Encounter reference
        if (encounterId != null) {
            carePlan.setEncounter(new Reference("Encounter/" + encounterId));
        }

        // Status
        carePlan.setStatus(CarePlan.CarePlanStatus.ACTIVE);

        // Intent
        carePlan.setIntent(CarePlan.CarePlanIntent.PLAN);

        // Title
        carePlan.setTitle("Encounter Plan");

        // Description (combine plan fields)
        StringBuilder description = new StringBuilder();
        if (StringUtils.hasText(dto.getDiagnosticPlan())) {
            description.append("Diagnostic Plan: ").append(dto.getDiagnosticPlan()).append("\n");
        }
        if (StringUtils.hasText(dto.getPlan())) {
            description.append("Plan: ").append(dto.getPlan()).append("\n");
        }
        if (StringUtils.hasText(dto.getNotes())) {
            description.append("Notes: ").append(dto.getNotes()).append("\n");
        }
        if (StringUtils.hasText(dto.getFollowUpVisit())) {
            description.append("Follow-Up Visit: ").append(dto.getFollowUpVisit()).append("\n");
        }
        if (StringUtils.hasText(dto.getReturnWorkSchool())) {
            description.append("Return Work/School: ").append(dto.getReturnWorkSchool()).append("\n");
        }
        carePlan.setDescription(description.toString());

        return carePlan;
    }

    private PlanDto toPlanDto(CarePlan carePlan, Long patientId, Long encounterId) {
        PlanDto dto = new PlanDto();

        if (carePlan.hasId()) {
            String fhirId = carePlan.getIdElement().getIdPart();
            dto.setId(Long.parseLong(fhirId));
            dto.setFhirId(fhirId);
            dto.setExternalId(fhirId);
        }

        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);

        // Parse description back to fields
        if (carePlan.hasDescription()) {
            String desc = carePlan.getDescription();
            for (String line : desc.split("\n")) {
                if (line.startsWith("Diagnostic Plan: ")) {
                    dto.setDiagnosticPlan(line.substring(17));
                } else if (line.startsWith("Plan: ")) {
                    dto.setPlan(line.substring(6));
                } else if (line.startsWith("Notes: ")) {
                    dto.setNotes(line.substring(7));
                } else if (line.startsWith("Follow-Up Visit: ")) {
                    dto.setFollowUpVisit(line.substring(17));
                } else if (line.startsWith("Return Work/School: ")) {
                    dto.setReturnWorkSchool(line.substring(20));
                }
            }
        }

        // Check sign metadata
        String fhirId = dto.getFhirId();
        if (fhirId != null) {
            SignMetadata meta = signMetadataCache.get(fhirId);
            if (meta != null) {
                dto.setESigned(meta.eSigned);
                dto.setSignedAt(meta.signedAt != null ? meta.signedAt.format(ISO) : null);
                dto.setSignedBy(meta.signedBy);
                dto.setPrintedAt(meta.printedAt != null ? meta.printedAt.format(ISO) : null);
            }
        }
        
        if (carePlan.hasMeta()) {
            populateAudit(dto, carePlan.getMeta());
        }

        return dto;
    }

    private List<PlanDto> extractPlanDtos(Bundle bundle, Long patientId, Long encounterId) {
        List<PlanDto> items = new ArrayList<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof CarePlan) {
                    items.add(toPlanDto((CarePlan) entry.getResource(), patientId, encounterId));
                }
            }
        }
        return items;
    }

    // ========== PDF Helpers ==========

    private static void title(PDPageContentStream cs, float x, float y, String t) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 18); cs.newLineAtOffset(x, y); cs.showText(t); cs.endText();
    }

    private static void row(PDPageContentStream cs, float x, float y, float labelWidth, String k, String v) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 11); cs.newLineAtOffset(x, y); cs.showText(k); cs.endText();
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 11); cs.newLineAtOffset(x + labelWidth, y); cs.showText(v != null ? v : "-"); cs.endText();
    }

    private static void text(PDPageContentStream cs, float x, float y, String s) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 11); cs.newLineAtOffset(x, y); cs.showText(s); cs.endText();
    }

    private static float block(PDPageContentStream cs, float x, float y, String label, String value) throws IOException {
        if (!StringUtils.hasText(value)) return y;

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
        cs.newLineAtOffset(x, y);
        cs.showText(label + ":");
        cs.endText();
        y -= 18;

        final float maxWidth = 612f - (50f * 2) - 20f;
        for (String line : wrap(PDType1Font.HELVETICA, 11, value, maxWidth)) {
            text(cs, x + 10, y, line);
            y -= 15;
        }
        return y - 8;
    }

    private static float blockAligned(PDPageContentStream cs, float x, float y, float labelWidth, String label, String value) throws IOException {
        if (!StringUtils.hasText(value)) return y;

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
        cs.newLineAtOffset(x, y);
        cs.showText(label);
        cs.endText();

        final float maxWidth = 612f - (50f * 2) - labelWidth - 10f;
        List<String> lines = wrap(PDType1Font.HELVETICA, 11, value, maxWidth);
        
        for (int i = 0; i < lines.size(); i++) {
            text(cs, x + labelWidth, y, lines.get(i));
            if (i < lines.size() - 1) y -= 15;
        }
        
        return y - 18;
    }

    private static List<String> wrap(PDType1Font font, int fontSize, String text, float maxWidth) throws IOException {
        List<String> out = new ArrayList<>();
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
    
    private void populateAudit(PlanDto dto, Meta meta) {
        PlanDto.Audit audit = new PlanDto.Audit();
        if (meta.hasLastUpdated()) {
            audit.setLastModifiedDate(meta.getLastUpdated().toInstant().atOffset(ZoneOffset.UTC).toLocalDate().toString());
            audit.setCreatedDate(meta.getLastUpdated().toInstant().atOffset(ZoneOffset.UTC).toLocalDate().toString());
        }
        dto.setAudit(audit);
    }
    
    // ✅ Validate path variables
    private void validatePathVariable(Long value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is invalid. " + fieldName + " cannot be null");
        }
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " is invalid. " + fieldName + " must be a positive number. Provided: " + value);
        }
    }
    
    private void validatePatientExists(Long patientId) {
        try {
            fhirClientService.read(Patient.class, String.valueOf(patientId), getPracticeId());
        } catch (Exception e) {
            throw new IllegalArgumentException("Patient ID is invalid. Patient not found: " + patientId);
        }
    }
    
    private void validateEncounterExists(Long encounterId) {
        try {
            fhirClientService.read(Encounter.class, String.valueOf(encounterId), getPracticeId());
        } catch (Exception e) {
            throw new IllegalArgumentException("Encounter ID is invalid. Encounter not found: " + encounterId);
        }
    }
}
