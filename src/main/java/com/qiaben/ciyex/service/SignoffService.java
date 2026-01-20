package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import com.qiaben.ciyex.dto.SignoffDto;
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
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-only Signoff Service.
 * Uses FHIR Task resource directly via FhirClientService.
 * No local database storage - all data stored in FHIR server.
 * Retains PDF rendering and e-signing business logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SignoffService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String STATUS_DRAFT = "Draft";
    private static final String STATUS_SIGNED = "Signed";
    private static final String STATUS_LOCKED = "Locked";

    // Extension URLs
    private static final String EXT_ENCOUNTER = "http://ciyex.com/fhir/StructureDefinition/encounter-reference";
    private static final String EXT_TARGET_TYPE = "http://ciyex.com/fhir/StructureDefinition/target-type";
    private static final String EXT_TARGET_ID = "http://ciyex.com/fhir/StructureDefinition/target-id";
    private static final String EXT_TARGET_VERSION = "http://ciyex.com/fhir/StructureDefinition/target-version";
    private static final String EXT_SIGNED_BY = "http://ciyex.com/fhir/StructureDefinition/signed-by";
    private static final String EXT_SIGNER_ROLE = "http://ciyex.com/fhir/StructureDefinition/signer-role";
    private static final String EXT_SIGNED_AT = "http://ciyex.com/fhir/StructureDefinition/signed-at";
    private static final String EXT_SIGNATURE_TYPE = "http://ciyex.com/fhir/StructureDefinition/signature-type";
    private static final String EXT_SIGNATURE_DATA = "http://ciyex.com/fhir/StructureDefinition/signature-data";
    private static final String EXT_CONTENT_HASH = "http://ciyex.com/fhir/StructureDefinition/content-hash";
    private static final String EXT_ATTESTATION = "http://ciyex.com/fhir/StructureDefinition/attestation-text";
    private static final String EXT_COMMENTS = "http://ciyex.com/fhir/StructureDefinition/comments";
    private static final String EXT_PRINTED_AT = "http://ciyex.com/fhir/StructureDefinition/printed-at";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public SignoffDto create(Long patientId, Long encounterId, SignoffDto dto) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        log.debug("Creating FHIR Task (signoff) for patient: {} encounter: {}", patientId, encounterId);

        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);
        dto.setStatus(STATUS_DRAFT);

        Task task = toFhirTask(dto);
        var outcome = fhirClientService.create(task, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setId(Long.parseLong(fhirId));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        
        Task created = (Task) outcome.getResource();
        if (created != null && created.hasMeta()) {
            populateAudit(dto, created.getMeta());
        }
        
        log.info("Created FHIR Task (signoff) with id: {}", fhirId);

        return dto;
    }

    // LIST BY ENCOUNTER
    public List<SignoffDto> list(Long patientId, Long encounterId) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        log.debug("Getting FHIR Tasks (signoffs) for patient: {} encounter: {}", patientId, encounterId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Task.class)
                .where(new ReferenceClientParam("patient").hasId("Patient/" + patientId))
                .returnBundle(Bundle.class)
                .execute();

        List<Task> tasks = fhirClientService.extractResources(bundle, Task.class);
        return tasks.stream()
                .filter(this::isSignoffTask)
                .filter(t -> hasEncounter(t, encounterId))
                .map(this::fromFhirTask)
                .collect(Collectors.toList());
    }

    // GET ALL BY PATIENT
    public List<SignoffDto> getAllByPatient(Long patientId) {
        validatePathVariable(patientId, "Patient ID");
        validatePatientExists(patientId);
        log.debug("Getting all FHIR Tasks (signoffs) for patient: {}", patientId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Task.class)
                .where(new ReferenceClientParam("patient").hasId("Patient/" + patientId))
                .returnBundle(Bundle.class)
                .execute();

        List<Task> tasks = fhirClientService.extractResources(bundle, Task.class);
        return tasks.stream()
                .filter(this::isSignoffTask)
                .map(this::fromFhirTask)
                .collect(Collectors.toList());
    }

    // GET ONE
    public SignoffDto getOne(Long patientId, Long encounterId, String fhirId) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validateFhirId(fhirId, "Signoff ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        log.debug("Getting FHIR Task (signoff): {}", fhirId);
        try {
            Task task = fhirClientService.read(Task.class, fhirId, getPracticeId());
            SignoffDto dto = fromFhirTask(task);
            dto.setId(Long.parseLong(fhirId));
            return dto;
        } catch (Exception e) {
            throw new IllegalArgumentException("Signoff ID is invalid. Signoff not found: " + fhirId);
        }
    }

    // UPDATE
    public SignoffDto update(Long patientId, Long encounterId, String fhirId, SignoffDto dto) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validateFhirId(fhirId, "Signoff ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        log.debug("Updating FHIR Task (signoff): {}", fhirId);

        Task existing;
        try {
            existing = fhirClientService.read(Task.class, fhirId, getPracticeId());
        } catch (Exception e) {
            throw new IllegalArgumentException("Signoff ID is invalid. Signoff not found: " + fhirId);
        }

        SignoffDto existingDto = fromFhirTask(existing);
        if (isLocked(existingDto.getStatus())) {
            throw new IllegalStateException("Signed/locked sign-offs are read-only.");
        }

        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);

        Task task = toFhirTask(dto);
        task.setId(fhirId);
        fhirClientService.update(task, getPracticeId());

        return getOne(patientId, encounterId, fhirId);
    }

    // DELETE
    public void delete(Long patientId, Long encounterId, String fhirId) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validateFhirId(fhirId, "Signoff ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        log.debug("Deleting FHIR Task (signoff): {}", fhirId);

        Task existing;
        try {
            existing = fhirClientService.read(Task.class, fhirId, getPracticeId());
        } catch (Exception e) {
            throw new IllegalArgumentException("Signoff ID is invalid. Signoff not found: " + fhirId);
        }

        SignoffDto existingDto = fromFhirTask(existing);
        if (isLocked(existingDto.getStatus())) {
            throw new IllegalStateException("Signed/locked sign-offs cannot be deleted.");
        }

        fhirClientService.delete(Task.class, fhirId, getPracticeId());
    }

    // E-SIGN
    public SignoffDto eSign(Long patientId, Long encounterId, String fhirId, String signedBy) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validateFhirId(fhirId, "Signoff ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        log.debug("E-signing FHIR Task (signoff): {}", fhirId);

        Task task;
        try {
            task = fhirClientService.read(Task.class, fhirId, getPracticeId());
        } catch (Exception e) {
            throw new IllegalArgumentException("Signoff ID is invalid. Signoff not found: " + fhirId);
        }
        SignoffDto dto = fromFhirTask(task);

        if (isLocked(dto.getStatus())) {
            return dto; // idempotent
        }

        dto.setStatus(STATUS_LOCKED);
        dto.setSignedBy(StringUtils.hasText(signedBy) ? signedBy : "system");
        dto.setSignedAt(java.time.OffsetDateTime.now().toString());

        // Compute content hash if signature data present
        if (StringUtils.hasText(dto.getSignatureData())) {
            dto.setContentHash(md5(dto.getSignatureData()));
        }

        Task updatedTask = toFhirTask(dto);
        updatedTask.setId(fhirId);
        fhirClientService.update(updatedTask, getPracticeId());

        return dto;
    }

    // RENDER PDF
    public byte[] renderPdf(Long patientId, Long encounterId, String fhirId) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validateFhirId(fhirId, "Signoff ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        
        Task task;
        try {
            task = fhirClientService.read(Task.class, fhirId, getPracticeId());
        } catch (Exception e) {
            throw new IllegalArgumentException("Signoff ID is invalid. Signoff not found: " + fhirId);
        }
        SignoffDto dto = fromFhirTask(task);

        // Update printed timestamp
        dto.setPrintedAt(LocalDateTime.now().toString());
        Task updatedTask = toFhirTask(dto);
        updatedTask.setId(fhirId);
        try {
            fhirClientService.update(updatedTask, getPracticeId());
        } catch (Exception ignore) {}

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float x = 64, y = 740;

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.newLineAtOffset(x, y);
                cs.showText("Encounter Sign-off");
                cs.endText();

                y -= 26;
                draw(cs, x, y, "Patient ID:", String.valueOf(patientId)); y -= 16;
                draw(cs, x, y, "Encounter ID:", String.valueOf(encounterId)); y -= 16;
                draw(cs, x, y, "Sign-off ID:", fhirId); y -= 22;

                draw(cs, x, y, "Status:", nullTo(dto.getStatus())); y -= 16;
                draw(cs, x, y, "Signed By:", nullTo(dto.getSignedBy())); y -= 16;
                draw(cs, x, y, "Signer Role:", nullTo(dto.getSignerRole())); y -= 16;
                draw(cs, x, y, "Signed At:", nullTo(dto.getSignedAt())); y -= 22;
                
                draw(cs, x, y, "Signature Type:", nullTo(dto.getSignatureType())); y -= 16;
                draw(cs, x, y, "Content Hash:", nullTo(dto.getContentHash())); y -= 16;
                draw(cs, x, y, "Target:", dto.getTargetType() != null ? dto.getTargetType() + (dto.getTargetId() != null ? " #" + dto.getTargetId() : "") : "-"); y -= 22;

                if (StringUtils.hasText(dto.getAttestationText())) {
                    String[] lines = dto.getAttestationText().split("\\R");
                    draw(cs, x, y, "Attestation:", lines[0]); y -= 16;
                    for (int i = 1; i < lines.length; i++) {
                        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x + 150, y); cs.showText(lines[i]); cs.endText();
                        y -= 14;
                    }
                    y -= 6;
                }
                
                if (StringUtils.hasText(dto.getComments())) {
                    String[] lines = dto.getComments().split("\\R");
                    draw(cs, x, y, "Comments:", lines[0]); y -= 16;
                    for (int i = 1; i < lines.length; i++) {
                        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x + 150, y); cs.showText(lines[i]); cs.endText();
                        y -= 14;
                    }
                    y -= 6;
                }

                draw(cs, x, y, "Printed:", nullTo(dto.getPrintedAt()));
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate Sign-off PDF", ex);
        }
    }

    // -------- FHIR Mapping --------

    private Task toFhirTask(SignoffDto dto) {
        Task task = new Task();

        // Status mapping
        if (dto.getStatus() != null) {
            switch (dto.getStatus()) {
                case STATUS_DRAFT -> task.setStatus(Task.TaskStatus.DRAFT);
                case STATUS_SIGNED, STATUS_LOCKED -> task.setStatus(Task.TaskStatus.COMPLETED);
                default -> task.setStatus(Task.TaskStatus.DRAFT);
            }
        }

        task.setIntent(Task.TaskIntent.ORDER);

        // Code = signoff
        task.setCode(new CodeableConcept().addCoding(
                new Coding()
                        .setSystem("http://ciyex.com/fhir/CodeSystem/task-type")
                        .setCode("signoff")
                        .setDisplay("Encounter Sign-off")
        ));

        // Patient reference
        if (dto.getPatientId() != null) {
            task.setFor(new Reference("Patient/" + dto.getPatientId()));
        }

        // Encounter extension
        if (dto.getEncounterId() != null) {
            task.addExtension(new Extension(EXT_ENCOUNTER, new Reference("Encounter/" + dto.getEncounterId())));
        }

        // All other fields as extensions
        addStringExtension(task, EXT_TARGET_TYPE, dto.getTargetType());
        if (dto.getTargetId() != null) {
            task.addExtension(new Extension(EXT_TARGET_ID, new StringType(dto.getTargetId().toString())));
        }
        addStringExtension(task, EXT_TARGET_VERSION, dto.getTargetVersion());
        addStringExtension(task, EXT_SIGNED_BY, dto.getSignedBy());
        addStringExtension(task, EXT_SIGNER_ROLE, dto.getSignerRole());
        addStringExtension(task, EXT_SIGNED_AT, dto.getSignedAt());
        addStringExtension(task, EXT_SIGNATURE_TYPE, dto.getSignatureType());
        addStringExtension(task, EXT_SIGNATURE_DATA, dto.getSignatureData());
        addStringExtension(task, EXT_CONTENT_HASH, dto.getContentHash());
        addStringExtension(task, EXT_ATTESTATION, dto.getAttestationText());
        addStringExtension(task, EXT_COMMENTS, dto.getComments());
        addStringExtension(task, EXT_PRINTED_AT, dto.getPrintedAt());

        // Store status in extension too for easy retrieval
        task.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/signoff-status", new StringType(dto.getStatus())));

        return task;
    }

    private SignoffDto fromFhirTask(Task task) {
        SignoffDto dto = new SignoffDto();
        String fhirId = task.getIdElement().getIdPart();
        dto.setId(Long.parseLong(fhirId));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);

        // Patient
        if (task.hasFor() && task.getFor().hasReference()) {
            String ref = task.getFor().getReference();
            if (ref.startsWith("Patient/")) {
                try {
                    dto.setPatientId(Long.parseLong(ref.substring("Patient/".length())));
                } catch (NumberFormatException ignored) {}
            }
        }

        // Encounter
        Extension encExt = task.getExtensionByUrl(EXT_ENCOUNTER);
        if (encExt != null && encExt.getValue() instanceof Reference) {
            String ref = ((Reference) encExt.getValue()).getReference();
            if (ref != null && ref.startsWith("Encounter/")) {
                try {
                    dto.setEncounterId(Long.parseLong(ref.substring("Encounter/".length())));
                } catch (NumberFormatException ignored) {}
            }
        }

        // Status from extension
        Extension statusExt = task.getExtensionByUrl("http://ciyex.com/fhir/StructureDefinition/signoff-status");
        if (statusExt != null && statusExt.getValue() instanceof StringType) {
            dto.setStatus(((StringType) statusExt.getValue()).getValue());
        } else {
            // Fallback to Task status
            if (task.hasStatus()) {
                dto.setStatus(task.getStatus() == Task.TaskStatus.COMPLETED ? STATUS_LOCKED : STATUS_DRAFT);
            }
        }

        // All extensions
        dto.setTargetType(getExtensionString(task, EXT_TARGET_TYPE));
        String targetIdStr = getExtensionString(task, EXT_TARGET_ID);
        if (targetIdStr != null) {
            try { dto.setTargetId(Long.parseLong(targetIdStr)); } catch (NumberFormatException ignored) {}
        }
        dto.setTargetVersion(getExtensionString(task, EXT_TARGET_VERSION));
        dto.setSignedBy(getExtensionString(task, EXT_SIGNED_BY));
        dto.setSignerRole(getExtensionString(task, EXT_SIGNER_ROLE));
        dto.setSignedAt(getExtensionString(task, EXT_SIGNED_AT));
        dto.setSignatureType(getExtensionString(task, EXT_SIGNATURE_TYPE));
        dto.setSignatureData(getExtensionString(task, EXT_SIGNATURE_DATA));
        dto.setContentHash(getExtensionString(task, EXT_CONTENT_HASH));
        dto.setAttestationText(getExtensionString(task, EXT_ATTESTATION));
        dto.setComments(getExtensionString(task, EXT_COMMENTS));
        dto.setPrintedAt(getExtensionString(task, EXT_PRINTED_AT));
        
        if (task.hasMeta()) {
            populateAudit(dto, task.getMeta());
        }

        return dto;
    }

    // -------- Helpers --------

    private boolean isSignoffTask(Task task) {
        if (!task.hasCode()) return false;
        return task.getCode().getCoding().stream()
                .anyMatch(c -> "signoff".equals(c.getCode()));
    }

    private boolean hasEncounter(Task task, Long encounterId) {
        Extension encExt = task.getExtensionByUrl(EXT_ENCOUNTER);
        if (encExt != null && encExt.getValue() instanceof Reference) {
            String ref = ((Reference) encExt.getValue()).getReference();
            return ref != null && ref.equals("Encounter/" + encounterId);
        }
        return false;
    }

    private static boolean isLocked(String status) {
        return STATUS_SIGNED.equalsIgnoreCase(status) || STATUS_LOCKED.equalsIgnoreCase(status) || "finalized".equalsIgnoreCase(status);
    }

    private static String md5(String text) {
        return DigestUtils.md5DigestAsHex(text.getBytes(StandardCharsets.UTF_8));
    }

    private void addStringExtension(Task task, String url, String value) {
        if (value != null) {
            task.addExtension(new Extension(url, new StringType(value)));
        }
    }

    private String getExtensionString(Task task, String url) {
        Extension ext = task.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) {
            return ((StringType) ext.getValue()).getValue();
        }
        return null;
    }

    private static void draw(PDPageContentStream cs, float x, float y, String label, String value) throws IOException {
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
        cs.newLineAtOffset(x, y);
        cs.showText(label);
        cs.endText();
        
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 12);
        cs.newLineAtOffset(x + 150, y);
        cs.showText(value != null ? value : "-");
        cs.endText();
    }
    
    private static String nullTo(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }
    
    private void populateAudit(SignoffDto dto, Meta meta) {
        SignoffDto.Audit audit = new SignoffDto.Audit();
        if (meta.hasLastUpdated()) {
            audit.setLastModifiedDate(meta.getLastUpdated().toInstant().atOffset(java.time.ZoneOffset.UTC).toLocalDate().toString());
            audit.setCreatedDate(meta.getLastUpdated().toInstant().atOffset(java.time.ZoneOffset.UTC).toLocalDate().toString());
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
    
    private void validateFhirId(String fhirId, String fieldName) {
        if (fhirId == null || fhirId.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is invalid. " + fieldName + " cannot be null or empty");
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
