package org.ciyex.ehr.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import org.ciyex.ehr.dto.ChiefComplaintDto;
import org.ciyex.ehr.fhir.FhirClientService;
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
 * ChiefComplaint Service - FHIR Only.
 * All chief complaint data is stored in HAPI FHIR server as Condition resources.
 */
@Service
@Slf4j
public class ChiefComplaintService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    // In-memory cache for e-sign/print metadata (keyed by FHIR ID)
    private final Map<String, SignMetadata> signMetadataCache = new ConcurrentHashMap<>();

    @Autowired
    public ChiefComplaintService(FhirClientService fhirClientService, PracticeContextService practiceContextService) {
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
    public List<ChiefComplaintDto> getAllByPatient(Long patientId) {
        validatePathVariable(patientId, "Patient ID");
        log.debug("Getting FHIR Conditions (chief complaint) for patient: {}", patientId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Condition.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .where(new TokenClientParam("category").exactly()
                        .systemAndCode("http://terminology.hl7.org/CodeSystem/condition-category", "encounter-diagnosis"))
                
                .returnBundle(Bundle.class)
                .execute();

        return extractChiefComplaintDtos(bundle, patientId, null);
    }

    // ✅ Create chief complaint
    public ChiefComplaintDto create(Long patientId, Long encounterId, ChiefComplaintDto dto) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        log.info("Creating chief complaint in FHIR for patient: {}, encounter: {}", patientId, encounterId);

        Condition condition = toFhirCondition(dto, patientId, encounterId);
        MethodOutcome outcome = fhirClientService.create(condition, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setId(Long.parseLong(fhirId));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);
        
        Condition created = (Condition) outcome.getResource();
        if (created != null && created.hasMeta()) {
            populateAudit(dto, created.getMeta());
        }

        log.info("Created FHIR Condition (chief complaint) with ID: {}", fhirId);
        return dto;
    }

    // ✅ List chief complaints for encounter
    public List<ChiefComplaintDto> list(Long patientId, Long encounterId) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        log.debug("Listing FHIR Conditions (chief complaint) for patient: {}, encounter: {}", patientId, encounterId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Condition.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .where(new TokenClientParam("category").exactly()
                        .systemAndCode("http://terminology.hl7.org/CodeSystem/condition-category", "encounter-diagnosis"))
                
                .returnBundle(Bundle.class)
                .execute();

        return extractChiefComplaintDtos(bundle, patientId, encounterId);
    }

    // ✅ Get one chief complaint
    public ChiefComplaintDto getOne(Long patientId, Long encounterId, Long id) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validatePathVariable(id, "Chief Complaint ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        String fhirId = String.valueOf(id);
        log.debug("Getting FHIR Condition (chief complaint) with ID: {}", fhirId);

        try {
            Condition condition = fhirClientService.read(Condition.class, fhirId, getPracticeId());
            ChiefComplaintDto dto = toChiefComplaintDto(condition, patientId, encounterId);
            dto.setId(id);
            return dto;
        } catch (Exception e) {
            throw new IllegalArgumentException("Chief Complaint ID is invalid. Chief Complaint not found: " + id);
        }
    }

    // ✅ Update chief complaint
    public ChiefComplaintDto update(Long patientId, Long encounterId, Long id, ChiefComplaintDto dto) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validatePathVariable(id, "Chief Complaint ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        String fhirId = String.valueOf(id);
        log.info("Updating FHIR Condition (chief complaint) with ID: {}", fhirId);

        // Validate resource exists
        try {
            fhirClientService.read(Condition.class, fhirId, getPracticeId());
        } catch (Exception e) {
            throw new IllegalArgumentException("Chief Complaint ID is invalid. Chief Complaint not found: " + id);
        }

        // Check if signed
        SignMetadata meta = signMetadataCache.get(fhirId);
        if (meta != null && Boolean.TRUE.equals(meta.eSigned)) {
            throw new IllegalStateException("Signed chief complaint is read-only.");
        }

        Condition condition = toFhirCondition(dto, patientId, encounterId);
        condition.setId(fhirId);
        fhirClientService.update(condition, getPracticeId());

        return getOne(patientId, encounterId, id);
    }

    // ✅ Delete chief complaint
    public void delete(Long patientId, Long encounterId, Long id) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validatePathVariable(id, "Chief Complaint ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        String fhirId = String.valueOf(id);
        log.info("Deleting FHIR Condition (chief complaint) with ID: {}", fhirId);

        // Validate resource exists
        try {
            fhirClientService.read(Condition.class, fhirId, getPracticeId());
        } catch (Exception e) {
            throw new IllegalArgumentException("Chief Complaint ID is invalid. Chief Complaint not found: " + id);
        }

        // Check if signed
        SignMetadata meta = signMetadataCache.get(fhirId);
        if (meta != null && Boolean.TRUE.equals(meta.eSigned)) {
            throw new IllegalStateException("Signed chief complaint cannot be deleted.");
        }

        fhirClientService.delete(Condition.class, fhirId, getPracticeId());
        signMetadataCache.remove(fhirId);
    }

    // ✅ eSign chief complaint
    public ChiefComplaintDto eSign(Long patientId, Long encounterId, Long id, String signedBy) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validatePathVariable(id, "Chief Complaint ID");
        String fhirId = String.valueOf(id);
        log.info("E-signing FHIR Condition (chief complaint) with ID: {}", fhirId);

        SignMetadata meta = signMetadataCache.computeIfAbsent(fhirId, k -> new SignMetadata());

        if (Boolean.TRUE.equals(meta.eSigned)) {
            return getOne(patientId, encounterId, id);
        }

        meta.eSigned = true;
        meta.signedBy = StringUtils.hasText(signedBy) ? signedBy : "system";
        meta.signedAt = OffsetDateTime.now(ZoneOffset.UTC);

        ChiefComplaintDto dto = getOne(patientId, encounterId, id);
        dto.setESigned(meta.eSigned);
        dto.setSignedAt(meta.signedAt.toString());
        dto.setSignedBy(meta.signedBy);

        return dto;
    }

    // ✅ Render PDF
    public byte[] renderPdf(Long patientId, Long encounterId, Long id) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validatePathVariable(id, "Chief Complaint ID");
        String fhirId = String.valueOf(id);
        log.info("Rendering PDF for FHIR Condition (chief complaint) with ID: {}", fhirId);

        ChiefComplaintDto dto = getOne(patientId, encounterId, id);

        // Update print timestamp
        SignMetadata meta = signMetadataCache.computeIfAbsent(fhirId, k -> new SignMetadata());
        meta.printedAt = OffsetDateTime.now(ZoneOffset.UTC);

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float x = 64, y = 740;

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.newLineAtOffset(x, y);
                cs.showText("Chief Complaint");
                cs.endText();

                y -= 26;
                draw(cs, x, y, "Patient ID:", String.valueOf(patientId)); y -= 16;
                draw(cs, x, y, "Encounter ID:", String.valueOf(encounterId)); y -= 16;
                draw(cs, x, y, "Record ID:", fhirId); y -= 20;

                if (StringUtils.hasText(dto.getComplaint())) { draw(cs, x, y, "Complaint:", dto.getComplaint()); y -= 16; }
                if (StringUtils.hasText(dto.getSeverity()))  { draw(cs, x, y, "Severity:", dto.getSeverity());   y -= 16; }
                if (StringUtils.hasText(dto.getStatus()))    { draw(cs, x, y, "Status:", dto.getStatus());       y -= 16; }

                if (StringUtils.hasText(dto.getDetails())) {
                    y -= 8;
                    draw(cs, x, y, "Details:", dto.getDetails()); y -= 14;
                }

                y -= 10;
                draw(cs, x, y, "eSigned:", Boolean.TRUE.equals(meta.eSigned) ? "Yes" : "No"); y -= 16;
                if (meta.signedAt != null) { draw(cs, x, y, "Signed At:", meta.signedAt.toString()); y -= 16; }
                if (StringUtils.hasText(meta.signedBy)) { draw(cs, x, y, "Signed By:", meta.signedBy); y -= 16; }
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate Chief Complaint PDF", ex);
        }
    }

    // ========== FHIR Mapping Methods ==========

    private Condition toFhirCondition(ChiefComplaintDto dto, Long patientId, Long encounterId) {
        Condition condition = new Condition();

        // Patient reference
        condition.setSubject(new Reference("Patient/" + patientId));

        // Encounter reference
        if (encounterId != null) {
            condition.setEncounter(new Reference("Encounter/" + encounterId));
        }

        // Category: encounter-diagnosis (for chief complaint)
        condition.addCategory()
                .addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-category")
                .setCode("encounter-diagnosis")
                .setDisplay("Encounter Diagnosis");

        // Clinical status
        String status = dto.getStatus() != null ? dto.getStatus().toLowerCase() : "active";
        condition.setClinicalStatus(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                        .setCode(status)));

        // Verification status
        condition.setVerificationStatus(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/condition-ver-status")
                        .setCode("confirmed")));

        // Code (complaint as text)
        if (dto.getComplaint() != null) {
            condition.setCode(new CodeableConcept().setText(dto.getComplaint()));
        }

        // Severity
        if (dto.getSeverity() != null) {
            condition.setSeverity(new CodeableConcept().setText(dto.getSeverity()));
        }

        // Note (details)
        if (dto.getDetails() != null) {
            condition.addNote().setText(dto.getDetails());
        }

        return condition;
    }

    private ChiefComplaintDto toChiefComplaintDto(Condition condition, Long patientId, Long encounterId) {
        ChiefComplaintDto dto = new ChiefComplaintDto();

        if (condition.hasId()) {
            String fhirId = condition.getIdElement().getIdPart();
            dto.setId(Long.parseLong(fhirId));
            dto.setFhirId(fhirId);
            dto.setExternalId(fhirId);
        }

        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);

        // Complaint from code text
        if (condition.hasCode() && condition.getCode().hasText()) {
            dto.setComplaint(condition.getCode().getText());
        }

        // Severity
        if (condition.hasSeverity() && condition.getSeverity().hasText()) {
            dto.setSeverity(condition.getSeverity().getText());
        }

        // Status from clinical status
        if (condition.hasClinicalStatus() && condition.getClinicalStatus().hasCoding()) {
            dto.setStatus(condition.getClinicalStatus().getCodingFirstRep().getCode());
        }

        // Details from note
        if (condition.hasNote()) {
            dto.setDetails(condition.getNoteFirstRep().getText());
        }
        
        if (condition.hasMeta()) {
            populateAudit(dto, condition.getMeta());
        }

        // Check sign metadata
        String fhirId = dto.getFhirId();
        if (fhirId != null) {
            SignMetadata meta = signMetadataCache.get(fhirId);
            if (meta != null) {
                dto.setESigned(meta.eSigned);
                dto.setSignedAt(meta.signedAt != null ? meta.signedAt.toString() : null);
                dto.setSignedBy(meta.signedBy);
                dto.setPrintedAt(meta.printedAt != null ? meta.printedAt.toString() : null);
            }
        }

        return dto;
    }

    private List<ChiefComplaintDto> extractChiefComplaintDtos(Bundle bundle, Long patientId, Long encounterId) {
        List<ChiefComplaintDto> items = new ArrayList<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Condition) {
                    items.add(toChiefComplaintDto((Condition) entry.getResource(), patientId, encounterId));
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
    
    private void populateAudit(ChiefComplaintDto dto, Meta meta) {
        ChiefComplaintDto.Audit audit = new ChiefComplaintDto.Audit();
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
