package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import com.qiaben.ciyex.dto.AssessmentDto;
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
 * Assessment Service - FHIR Only.
 * All assessment data is stored in HAPI FHIR server as ClinicalImpression resources.
 */
@Service
@Slf4j
public class AssessmentService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    // In-memory cache for e-sign/print metadata (keyed by FHIR ID)
    private final Map<String, SignMetadata> signMetadataCache = new ConcurrentHashMap<>();

    @Autowired
    public AssessmentService(FhirClientService fhirClientService, PracticeContextService practiceContextService) {
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
    public List<AssessmentDto> getAllByPatient(Long patientId) {
        log.debug("Getting FHIR ClinicalImpressions (assessment) for patient: {}", patientId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(ClinicalImpression.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                
                .returnBundle(Bundle.class)
                .execute();

        return extractAssessmentDtos(bundle, patientId, null);
    }

    // ✅ Create assessment
    public AssessmentDto create(Long patientId, Long encounterId, AssessmentDto dto) {
        log.info("Creating assessment in FHIR for patient: {}, encounter: {}", patientId, encounterId);

        ClinicalImpression ci = toFhirClinicalImpression(dto, patientId, encounterId);
        MethodOutcome outcome = fhirClientService.create(ci, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setId(Long.parseLong(fhirId));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);

        
        // Populate audit info
        ClinicalImpression created = (ClinicalImpression) outcome.getResource();
        if (created != null && created.hasMeta()) {
            populateAudit(dto, created.getMeta());
        }

        log.info("Created FHIR ClinicalImpression (assessment) with ID: {}", fhirId);
        return dto;
    }

    // ✅ Get one assessment
    public AssessmentDto getOne(Long patientId, Long encounterId, Long id) {
        String fhirId = String.valueOf(id);
        log.debug("Getting FHIR ClinicalImpression (assessment) with ID: {}", fhirId);

        try {
            ClinicalImpression ci = fhirClientService.read(ClinicalImpression.class, fhirId, getPracticeId());
            AssessmentDto dto = toAssessmentDto(ci, patientId, encounterId);
            dto.setId(id);
            return dto;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Assessment not found with ID: %d for Patient ID: %d and Encounter ID: %d",
                            id, patientId, encounterId));
        }
    }

    // ✅ Get all by encounter
    public List<AssessmentDto> getAllByEncounter(Long patientId, Long encounterId) {
        log.debug("Listing FHIR ClinicalImpressions (assessment) for patient: {}, encounter: {}", patientId, encounterId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(ClinicalImpression.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                
                .returnBundle(Bundle.class)
                .execute();

        return extractAssessmentDtos(bundle, patientId, encounterId);
    }

    // ✅ Update assessment
    public AssessmentDto update(Long patientId, Long encounterId, Long id, AssessmentDto dto) {
        String fhirId = String.valueOf(id);
        log.info("Updating FHIR ClinicalImpression (assessment) with ID: {}", fhirId);

        // Check if signed
        SignMetadata meta = signMetadataCache.get(fhirId);
        if (meta != null && Boolean.TRUE.equals(meta.eSigned)) {
            throw new IllegalStateException("Signed assessments are read-only.");
        }

        ClinicalImpression ci = toFhirClinicalImpression(dto, patientId, encounterId);
        ci.setId(fhirId);
        fhirClientService.update(ci, getPracticeId());

        // Re-read to get updated metadata and return complete DTO
        return getOne(patientId, encounterId, id);
    }

    // ✅ Delete assessment
    public void delete(Long patientId, Long encounterId, Long id) {
        String fhirId = String.valueOf(id);
        log.info("Deleting FHIR ClinicalImpression (assessment) with ID: {}", fhirId);

        // Check if signed
        SignMetadata meta = signMetadataCache.get(fhirId);
        if (meta != null && Boolean.TRUE.equals(meta.eSigned)) {
            throw new IllegalStateException("Signed assessments cannot be deleted.");
        }

        fhirClientService.delete(ClinicalImpression.class, fhirId, getPracticeId());
        signMetadataCache.remove(fhirId);
    }

    // ✅ eSign assessment
    public AssessmentDto eSign(Long patientId, Long encounterId, Long id, String signedBy) {
        String fhirId = String.valueOf(id);
        log.info("E-signing FHIR ClinicalImpression (assessment) with ID: {}", fhirId);

        SignMetadata meta = signMetadataCache.computeIfAbsent(fhirId, k -> new SignMetadata());

        if (Boolean.TRUE.equals(meta.eSigned)) {
            return getOne(patientId, encounterId, id);
        }

        meta.eSigned = true;
        meta.signedBy = StringUtils.hasText(signedBy) ? signedBy : "system";
        meta.signedAt = OffsetDateTime.now(ZoneOffset.UTC);

        AssessmentDto dto = getOne(patientId, encounterId, id);
        dto.setESigned(meta.eSigned);
        dto.setSignedAt(meta.signedAt);
        dto.setSignedBy(meta.signedBy);

        return dto;
    }

    // ✅ Render PDF
    public byte[] renderPdf(Long patientId, Long encounterId, Long id) {
        String fhirId = String.valueOf(id);
        log.info("Rendering PDF for FHIR ClinicalImpression (assessment) with ID: {}", fhirId);

        AssessmentDto dto = getOne(patientId, encounterId, id);

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
                cs.showText("Assessment");
                cs.endText();

                y -= 26;
                draw(cs, x, y, "Patient ID:", String.valueOf(patientId)); y -= 16;
                draw(cs, x, y, "Encounter ID:", String.valueOf(encounterId)); y -= 16;
                draw(cs, x, y, "Assessment ID:", fhirId); y -= 22;

                draw(cs, x, y, "Diagnosis Code:", nullTo(dto.getDiagnosisCode(), "-")); y -= 16;
                draw(cs, x, y, "Diagnosis Name:", nullTo(dto.getDiagnosisName(), "-")); y -= 16;
                draw(cs, x, y, "Status:", nullTo(dto.getStatus(), "-")); y -= 16;
                draw(cs, x, y, "Priority:", nullTo(dto.getPriority(), "-")); y -= 22;

                draw(cs, x, y, "Assessment / Impression:", nullTo(dto.getAssessmentText(), "-")); y -= 22;
                draw(cs, x, y, "Notes:", nullTo(dto.getNotes(), "-")); y -= 22;

                draw(cs, x, y, "eSigned:", Boolean.TRUE.equals(meta.eSigned) ? "Yes" : "No"); y -= 16;
                if (meta.signedAt != null) { draw(cs, x, y, "Signed At:", meta.signedAt.toString()); y -= 16; }
                if (StringUtils.hasText(meta.signedBy)) { draw(cs, x, y, "Signed By:", meta.signedBy); y -= 16; }
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Assessment PDF", e);
        }
    }

    // ========== FHIR Mapping Methods ==========

    private ClinicalImpression toFhirClinicalImpression(AssessmentDto dto, Long patientId, Long encounterId) {
        ClinicalImpression ci = new ClinicalImpression();

        // Patient reference
        ci.setSubject(new Reference("Patient/" + patientId));

        // Encounter reference
        if (encounterId != null) {
            ci.setEncounter(new Reference("Encounter/" + encounterId));
        }

        // Status
        String status = dto.getStatus() != null ? dto.getStatus().toLowerCase() : "completed";
        ci.setStatus(mapToClinicalImpressionStatus(status));

        // Description (assessment text)
        if (dto.getAssessmentText() != null) {
            ci.setDescription(dto.getAssessmentText());
        }

        // Priority - store as extension
        if (dto.getPriority() != null) {
            ci.addExtension("http://ciyex.com/fhir/priority", new StringType(dto.getPriority()));
        }

        // Finding (diagnosis)
        if (dto.getDiagnosisCode() != null || dto.getDiagnosisName() != null) {
            ClinicalImpression.ClinicalImpressionFindingComponent finding = ci.addFinding();
            CodeableConcept code = new CodeableConcept();
            if (dto.getDiagnosisCode() != null) {
                code.addCoding().setCode(dto.getDiagnosisCode());
            }
            if (dto.getDiagnosisName() != null) {
                code.setText(dto.getDiagnosisName());
            }
            finding.setItemCodeableConcept(code);
        }

        // Note
        if (dto.getNotes() != null) {
            ci.addNote().setText(dto.getNotes());
        }

        return ci;
    }

    private AssessmentDto toAssessmentDto(ClinicalImpression ci, Long patientId, Long encounterId) {
        AssessmentDto dto = new AssessmentDto();

        if (ci.hasId()) {
            String fhirId = ci.getIdElement().getIdPart();
            dto.setId(Long.parseLong(fhirId));
            dto.setFhirId(fhirId);
            dto.setExternalId(fhirId);
        }

        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);

        // Assessment text from description
        if (ci.hasDescription()) {
            dto.setAssessmentText(ci.getDescription());
        }

        // Status
        if (ci.hasStatus()) {
            dto.setStatus(ci.getStatus().toCode());
        }

        // Diagnosis from finding
        if (ci.hasFinding()) {
            ClinicalImpression.ClinicalImpressionFindingComponent finding = ci.getFindingFirstRep();
            if (finding.hasItemCodeableConcept()) {
                CodeableConcept code = finding.getItemCodeableConcept();
                if (code.hasCoding()) {
                    dto.setDiagnosisCode(code.getCodingFirstRep().getCode());
                }
                if (code.hasText()) {
                    dto.setDiagnosisName(code.getText());
                }
            }
        }

        // Notes
        if (ci.hasNote()) {
            dto.setNotes(ci.getNoteFirstRep().getText());
        }
        
        // Priority - retrieve from extension
        if (ci.hasExtension()) {
            ci.getExtension().stream()
                .filter(ext -> ext.getUrl().equals("http://ciyex.com/fhir/priority"))
                .findFirst()
                .ifPresent(ext -> {
                    if (ext.getValue() instanceof StringType) {
                        dto.setPriority(((StringType) ext.getValue()).getValue());
                    }
                });
        }
        
        // Populate audit from FHIR metadata
        if (ci.hasMeta()) {
            populateAudit(dto, ci.getMeta());
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

    private List<AssessmentDto> extractAssessmentDtos(Bundle bundle, Long patientId, Long encounterId) {
        List<AssessmentDto> items = new ArrayList<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof ClinicalImpression) {
                    items.add(toAssessmentDto((ClinicalImpression) entry.getResource(), patientId, encounterId));
                }
            }
        }
        return items;
    }

    private ClinicalImpression.ClinicalImpressionStatus mapToClinicalImpressionStatus(String status) {
        return switch (status) {
            case "in-progress" -> ClinicalImpression.ClinicalImpressionStatus.INPROGRESS;
            case "completed" -> ClinicalImpression.ClinicalImpressionStatus.COMPLETED;
            case "entered-in-error" -> ClinicalImpression.ClinicalImpressionStatus.ENTEREDINERROR;
            default -> ClinicalImpression.ClinicalImpressionStatus.COMPLETED;
        };
    }

    // ========== PDF Helpers ==========

    private static void draw(PDPageContentStream cs, float x, float y, String label, String value) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 12); cs.newLineAtOffset(x, y); cs.showText(label); cs.endText();
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x + 150, y); cs.showText(value != null ? value : "-"); cs.endText();
    }

    private static String nullTo(String v, String fb) { return (v == null || v.isBlank()) ? fb : v; }
    
    private void populateAudit(AssessmentDto dto, Meta meta) {
        AssessmentDto.Audit audit = new AssessmentDto.Audit();
        if (meta.hasLastUpdated()) {
            audit.setLastModifiedDate(meta.getLastUpdated().toInstant().atOffset(ZoneOffset.UTC).toLocalDate().toString());
        }
        if (meta.hasExtension()) {
            meta.getExtension().stream()
                .filter(ext -> ext.getUrl().contains("created"))
                .findFirst()
                .ifPresent(ext -> {
                    if (ext.getValue() instanceof DateTimeType) {
                        audit.setCreatedDate(((DateTimeType) ext.getValue()).getValue().toInstant().atOffset(ZoneOffset.UTC).toLocalDate().toString());
                    }
                });
        }
        if (audit.getCreatedDate() == null && meta.hasLastUpdated()) {
            audit.setCreatedDate(meta.getLastUpdated().toInstant().atOffset(ZoneOffset.UTC).toLocalDate().toString());
        }
        dto.setAudit(audit);
    }
}
