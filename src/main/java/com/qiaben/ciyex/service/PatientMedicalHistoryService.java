package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import com.qiaben.ciyex.dto.PatientMedicalHistoryDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-only PatientMedicalHistory Service.
 * Uses FHIR Condition resource directly via FhirClientService.
 * No local database storage - all data stored in FHIR server.
 * Retains PDF rendering and e-signing business logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatientMedicalHistoryService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Extension URLs
    private static final String EXT_ENCOUNTER = "http://ciyex.com/fhir/StructureDefinition/encounter-reference";
    private static final String EXT_CONDITION_NAME = "http://ciyex.com/fhir/StructureDefinition/condition-name";
    private static final String EXT_STATUS = "http://ciyex.com/fhir/StructureDefinition/status";
    private static final String EXT_IS_CHRONIC = "http://ciyex.com/fhir/StructureDefinition/is-chronic";
    private static final String EXT_DIAGNOSIS_DATE = "http://ciyex.com/fhir/StructureDefinition/diagnosis-date";
    private static final String EXT_ONSET_DATE = "http://ciyex.com/fhir/StructureDefinition/onset-date";
    private static final String EXT_RESOLVED_DATE = "http://ciyex.com/fhir/StructureDefinition/resolved-date";
    private static final String EXT_TREATMENT = "http://ciyex.com/fhir/StructureDefinition/treatment-details";
    private static final String EXT_DIAGNOSIS_DETAILS = "http://ciyex.com/fhir/StructureDefinition/diagnosis-details";
    private static final String EXT_DESCRIPTION = "http://ciyex.com/fhir/StructureDefinition/description";
    private static final String EXT_E_SIGNED = "http://ciyex.com/fhir/StructureDefinition/e-signed";
    private static final String EXT_SIGNED_AT = "http://ciyex.com/fhir/StructureDefinition/signed-at";
    private static final String EXT_SIGNED_BY = "http://ciyex.com/fhir/StructureDefinition/signed-by";
    private static final String EXT_PRINTED_AT = "http://ciyex.com/fhir/StructureDefinition/printed-at";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public PatientMedicalHistoryDto create(Long patientId, Long encounterId, PatientMedicalHistoryDto dto) {
        log.debug("Creating FHIR Condition (PMH) for patient: {} encounter: {}", patientId, encounterId);

        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);

        Condition condition = toFhirCondition(dto);
        var outcome = fhirClientService.create(condition, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setId(Long.parseLong(fhirId));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        
        Condition created = (Condition) outcome.getResource();
        if (created != null && created.hasMeta()) {
            populateAudit(dto, created.getMeta());
        }
        
        log.info("Created FHIR Condition (PMH) with id: {}", fhirId);

        return dto;
    }

    // GET ONE
    public PatientMedicalHistoryDto getOne(Long patientId, Long encounterId, String fhirId) {
        log.debug("Getting FHIR Condition (PMH): {}", fhirId);
        Condition condition = fhirClientService.read(Condition.class, fhirId, getPracticeId());
        PatientMedicalHistoryDto dto = fromFhirCondition(condition);
        dto.setId(Long.parseLong(fhirId));
        return dto;
    }

    // LIST BY ENCOUNTER
    public List<PatientMedicalHistoryDto> list(Long patientId, Long encounterId) {
        log.debug("Getting FHIR Conditions (PMH) for patient: {} encounter: {}", patientId, encounterId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Condition.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .returnBundle(Bundle.class)
                .execute();

        List<Condition> conditions = fhirClientService.extractResources(bundle, Condition.class);
        return conditions.stream()
                .filter(c -> hasEncounter(c, encounterId))
                .map(this::fromFhirCondition)
                .collect(Collectors.toList());
    }

    // GET ALL BY PATIENT
    public List<PatientMedicalHistoryDto> getAllByPatient(Long patientId) {
        log.debug("Getting all FHIR Conditions (PMH) for patient: {}", patientId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Condition.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .returnBundle(Bundle.class)
                .execute();

        List<Condition> conditions = fhirClientService.extractResources(bundle, Condition.class);
        return conditions.stream()
                .map(this::fromFhirCondition)
                .collect(Collectors.toList());
    }

    // UPDATE
    public PatientMedicalHistoryDto update(Long patientId, Long encounterId, String fhirId, PatientMedicalHistoryDto dto) {
        log.debug("Updating FHIR Condition (PMH): {}", fhirId);

        // Check if signed
        Condition existing = fhirClientService.read(Condition.class, fhirId, getPracticeId());
        PatientMedicalHistoryDto existingDto = fromFhirCondition(existing);
        if (Boolean.TRUE.equals(existingDto.getESigned())) {
            throw new IllegalStateException("Signed entries are read-only.");
        }

        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);

        Condition condition = toFhirCondition(dto);
        condition.setId(fhirId);
        fhirClientService.update(condition, getPracticeId());

        return getOne(patientId, encounterId, fhirId);
    }

    // DELETE
    public void delete(Long patientId, Long encounterId, String fhirId) {
        log.debug("Deleting FHIR Condition (PMH): {}", fhirId);

        // Check if signed
        Condition existing = fhirClientService.read(Condition.class, fhirId, getPracticeId());
        PatientMedicalHistoryDto existingDto = fromFhirCondition(existing);
        if (Boolean.TRUE.equals(existingDto.getESigned())) {
            throw new IllegalStateException("Signed entries cannot be deleted.");
        }

        fhirClientService.delete(Condition.class, fhirId, getPracticeId());
    }

    // E-SIGN
    public PatientMedicalHistoryDto eSign(Long patientId, Long encounterId, String fhirId, String signedBy) {
        log.debug("E-signing FHIR Condition (PMH): {}", fhirId);

        Condition condition = fhirClientService.read(Condition.class, fhirId, getPracticeId());
        PatientMedicalHistoryDto dto = fromFhirCondition(condition);

        if (Boolean.TRUE.equals(dto.getESigned())) {
            return dto; // idempotent
        }

        dto.setESigned(true);
        dto.setSignedBy(StringUtils.hasText(signedBy) ? signedBy : "system");
        dto.setSignedAt(OffsetDateTime.now(ZoneOffset.UTC));

        Condition updatedCondition = toFhirCondition(dto);
        updatedCondition.setId(fhirId);
        fhirClientService.update(updatedCondition, getPracticeId());

        return dto;
    }

    // RENDER PDF
    public byte[] renderPdf(Long patientId, Long encounterId, String fhirId) {
        Condition condition = fhirClientService.read(Condition.class, fhirId, getPracticeId());
        PatientMedicalHistoryDto dto = fromFhirCondition(condition);

        // Update printed timestamp
        dto.setPrintedAt(OffsetDateTime.now(ZoneOffset.UTC));
        Condition updatedCondition = toFhirCondition(dto);
        updatedCondition.setId(fhirId);
        try {
            fhirClientService.update(updatedCondition, getPracticeId());
        } catch (Exception ignore) {}

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
                draw(cs, x, y, "ID:", fhirId); y -= 22;

                if (dto.getConditionName() != null) { draw(cs, x, y, "Condition:", dto.getConditionName()); y -= 16; }
                if (dto.getStatus() != null) { draw(cs, x, y, "Status:", dto.getStatus()); y -= 16; }
                if (dto.getIsChronic() != null) { draw(cs, x, y, "Chronic:", dto.getIsChronic() ? "Yes" : "No"); y -= 16; }
                if (dto.getDiagnosisDate() != null) { draw(cs, x, y, "Diagnosis Date:", dto.getDiagnosisDate().toString()); y -= 16; }
                if (dto.getOnsetDate() != null) { draw(cs, x, y, "Onset Date:", dto.getOnsetDate().toString()); y -= 16; }
                if (dto.getResolvedDate() != null) { draw(cs, x, y, "Resolved Date:", dto.getResolvedDate().toString()); y -= 16; }

                if (has(dto.getDescription())) { draw(cs, x, y, "Description:", dto.getDescription()); y -= 16; }
                if (has(dto.getTreatmentDetails())) { draw(cs, x, y, "Treatment:", dto.getTreatmentDetails()); y -= 16; }
                if (has(dto.getDiagnosisDetails())) { draw(cs, x, y, "Diagnosis Details:", dto.getDiagnosisDetails()); y -= 16; }
                if (has(dto.getNotes())) { draw(cs, x, y, "Notes:", dto.getNotes()); y -= 16; }

                y -= 8;
                draw(cs, x, y, "eSigned:", Boolean.TRUE.equals(dto.getESigned()) ? "Yes" : "No"); y -= 16;
                if (dto.getSignedAt() != null) { draw(cs, x, y, "Signed At:", dto.getSignedAt().toString()); y -= 16; }
                if (has(dto.getSignedBy())) { draw(cs, x, y, "Signed By:", dto.getSignedBy()); y -= 16; }
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate Patient Medical History PDF", ex);
        }
    }

    // -------- FHIR Mapping --------

    private Condition toFhirCondition(PatientMedicalHistoryDto dto) {
        Condition c = new Condition();

        // Subject (Patient)
        if (dto.getPatientId() != null) {
            c.setSubject(new Reference("Patient/" + dto.getPatientId()));
        }

        // Encounter extension
        if (dto.getEncounterId() != null) {
            c.addExtension(new Extension(EXT_ENCOUNTER, new Reference("Encounter/" + dto.getEncounterId())));
        }

        // Code (medical condition)
        if (dto.getMedicalCondition() != null) {
            c.getCode().setText(dto.getMedicalCondition());
        }

        // Notes
        if (dto.getNotes() != null) {
            c.addNote().setText(dto.getNotes());
        }

        // Extensions
        addStringExtension(c, EXT_CONDITION_NAME, dto.getConditionName());
        addStringExtension(c, EXT_STATUS, dto.getStatus());
        addStringExtension(c, EXT_TREATMENT, dto.getTreatmentDetails());
        addStringExtension(c, EXT_DIAGNOSIS_DETAILS, dto.getDiagnosisDetails());
        addStringExtension(c, EXT_DESCRIPTION, dto.getDescription());
        addStringExtension(c, EXT_SIGNED_BY, dto.getSignedBy());

        if (dto.getIsChronic() != null) {
            c.addExtension(new Extension(EXT_IS_CHRONIC, new BooleanType(dto.getIsChronic())));
        }
        if (dto.getDiagnosisDate() != null) {
            c.addExtension(new Extension(EXT_DIAGNOSIS_DATE, new StringType(dto.getDiagnosisDate().toString())));
        }
        if (dto.getOnsetDate() != null) {
            c.addExtension(new Extension(EXT_ONSET_DATE, new StringType(dto.getOnsetDate().toString())));
        }
        if (dto.getResolvedDate() != null) {
            c.addExtension(new Extension(EXT_RESOLVED_DATE, new StringType(dto.getResolvedDate().toString())));
        }
        if (dto.getESigned() != null) {
            c.addExtension(new Extension(EXT_E_SIGNED, new BooleanType(dto.getESigned())));
        }
        if (dto.getSignedAt() != null) {
            c.addExtension(new Extension(EXT_SIGNED_AT, new StringType(dto.getSignedAt().toString())));
        }
        if (dto.getPrintedAt() != null) {
            c.addExtension(new Extension(EXT_PRINTED_AT, new StringType(dto.getPrintedAt().toString())));
        }

        return c;
    }

    private PatientMedicalHistoryDto fromFhirCondition(Condition c) {
        PatientMedicalHistoryDto dto = new PatientMedicalHistoryDto();
        String fhirId = c.getIdElement().getIdPart();
        dto.setId(Long.parseLong(fhirId));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);

        // Subject -> patientId
        if (c.hasSubject() && c.getSubject().hasReference()) {
            String ref = c.getSubject().getReference();
            if (ref.startsWith("Patient/")) {
                try {
                    dto.setPatientId(Long.parseLong(ref.substring("Patient/".length())));
                } catch (NumberFormatException ignored) {}
            }
        }

        // Encounter extension
        Extension encExt = c.getExtensionByUrl(EXT_ENCOUNTER);
        if (encExt != null && encExt.getValue() instanceof Reference) {
            String ref = ((Reference) encExt.getValue()).getReference();
            if (ref != null && ref.startsWith("Encounter/")) {
                try {
                    dto.setEncounterId(Long.parseLong(ref.substring("Encounter/".length())));
                } catch (NumberFormatException ignored) {}
            }
        }

        // Code -> medicalCondition
        if (c.hasCode() && c.getCode().hasText()) {
            dto.setMedicalCondition(c.getCode().getText());
        }

        // Notes
        if (c.hasNote()) {
            dto.setNotes(c.getNoteFirstRep().getText());
        }

        // Extensions
        dto.setConditionName(getExtensionString(c, EXT_CONDITION_NAME));
        dto.setStatus(getExtensionString(c, EXT_STATUS));
        dto.setTreatmentDetails(getExtensionString(c, EXT_TREATMENT));
        dto.setDiagnosisDetails(getExtensionString(c, EXT_DIAGNOSIS_DETAILS));
        dto.setDescription(getExtensionString(c, EXT_DESCRIPTION));
        dto.setSignedBy(getExtensionString(c, EXT_SIGNED_BY));

        Extension chronicExt = c.getExtensionByUrl(EXT_IS_CHRONIC);
        if (chronicExt != null && chronicExt.getValue() instanceof BooleanType) {
            dto.setIsChronic(((BooleanType) chronicExt.getValue()).booleanValue());
        }

        Extension eSignedExt = c.getExtensionByUrl(EXT_E_SIGNED);
        if (eSignedExt != null && eSignedExt.getValue() instanceof BooleanType) {
            dto.setESigned(((BooleanType) eSignedExt.getValue()).booleanValue());
        }

        String diagDateStr = getExtensionString(c, EXT_DIAGNOSIS_DATE);
        if (diagDateStr != null) {
            try { dto.setDiagnosisDate(LocalDateTime.parse(diagDateStr)); } catch (Exception ignored) {}
        }

        String onsetDateStr = getExtensionString(c, EXT_ONSET_DATE);
        if (onsetDateStr != null) {
            try { dto.setOnsetDate(LocalDate.parse(onsetDateStr)); } catch (Exception ignored) {}
        }

        String resolvedDateStr = getExtensionString(c, EXT_RESOLVED_DATE);
        if (resolvedDateStr != null) {
            try { dto.setResolvedDate(LocalDate.parse(resolvedDateStr)); } catch (Exception ignored) {}
        }

        String signedAtStr = getExtensionString(c, EXT_SIGNED_AT);
        if (signedAtStr != null) {
            try { dto.setSignedAt(OffsetDateTime.parse(signedAtStr)); } catch (Exception ignored) {}
        }

        String printedAtStr = getExtensionString(c, EXT_PRINTED_AT);
        if (printedAtStr != null) {
            try { dto.setPrintedAt(OffsetDateTime.parse(printedAtStr)); } catch (Exception ignored) {}
        }
        
        if (c.hasMeta()) {
            populateAudit(dto, c.getMeta());
        }

        return dto;
    }

    // -------- Helpers --------

    private boolean hasEncounter(Condition c, Long encounterId) {
        Extension encExt = c.getExtensionByUrl(EXT_ENCOUNTER);
        if (encExt != null && encExt.getValue() instanceof Reference) {
            String ref = ((Reference) encExt.getValue()).getReference();
            return ref != null && ref.equals("Encounter/" + encounterId);
        }
        return false;
    }

    private static boolean has(String s) { return s != null && !s.isBlank(); }

    private void addStringExtension(Condition c, String url, String value) {
        if (value != null) {
            c.addExtension(new Extension(url, new StringType(value)));
        }
    }

    private String getExtensionString(Condition c, String url) {
        Extension ext = c.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) {
            return ((StringType) ext.getValue()).getValue();
        }
        return null;
    }

    private static void draw(PDPageContentStream cs, float x, float y, String label, String value) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 12); cs.newLineAtOffset(x, y); cs.showText(label); cs.endText();
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x + 140, y); cs.showText(value != null ? value : "-"); cs.endText();
    }
    
    private void populateAudit(PatientMedicalHistoryDto dto, Meta meta) {
        PatientMedicalHistoryDto.Audit audit = new PatientMedicalHistoryDto.Audit();
        if (meta.hasLastUpdated()) {
            audit.setLastModifiedDate(meta.getLastUpdated().toInstant().atOffset(ZoneOffset.UTC).toLocalDate().toString());
            audit.setCreatedDate(meta.getLastUpdated().toInstant().atOffset(ZoneOffset.UTC).toLocalDate().toString());
        }
        dto.setAudit(audit);
    }
}
