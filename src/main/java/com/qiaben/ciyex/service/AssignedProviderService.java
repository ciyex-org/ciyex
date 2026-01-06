package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import com.qiaben.ciyex.dto.AssignedProviderDto;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FHIR-only AssignedProvider Service.
 * Uses FHIR CareTeam resource directly via FhirClientService.
 * No local database storage - all data stored in FHIR server.
 * Retains e-signing and PDF rendering business logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssignedProviderService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    // Extension URLs for custom fields
    private static final String EXT_ENCOUNTER = "http://ciyex.com/fhir/StructureDefinition/encounter-reference";
    private static final String EXT_NOTES = "http://ciyex.com/fhir/StructureDefinition/notes";
    private static final String EXT_ESIGNED = "http://ciyex.com/fhir/StructureDefinition/esigned";
    private static final String EXT_SIGNED_AT = "http://ciyex.com/fhir/StructureDefinition/signed-at";
    private static final String EXT_SIGNED_BY = "http://ciyex.com/fhir/StructureDefinition/signed-by";
    private static final String EXT_PRINTED_AT = "http://ciyex.com/fhir/StructureDefinition/printed-at";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public AssignedProviderDto create(Long patientId, Long encounterId, AssignedProviderDto dto) {
        if (patientId == null) {
            throw new IllegalArgumentException("Patient ID is required");
        }
        if (encounterId == null) {
            throw new IllegalArgumentException("Encounter ID is required");
        }
        if (dto.getProviderId() == null) {
            throw new IllegalArgumentException("Provider ID is required");
        }

        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);

        log.debug("Creating FHIR CareTeam for patient: {} encounter: {}", patientId, encounterId);

        CareTeam careTeam = toFhirCareTeam(dto);
        var outcome = fhirClientService.create(careTeam, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setId(Long.parseLong(fhirId));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        
        CareTeam created = (CareTeam) outcome.getResource();
        if (created != null && created.hasMeta()) {
            populateAudit(dto, created.getMeta());
        }
        
        log.info("Created FHIR CareTeam with id: {}", fhirId);

        return dto;
    }

    // GET ONE
    public AssignedProviderDto getOne(Long patientId, Long encounterId, String fhirId) {
        log.debug("Getting FHIR CareTeam: {}", fhirId);
        CareTeam careTeam = fhirClientService.read(CareTeam.class, fhirId, getPracticeId());
        AssignedProviderDto dto = fromFhirCareTeam(careTeam);
        dto.setId(Long.parseLong(fhirId));
        return dto;
    }

    // LIST BY PATIENT
    public List<AssignedProviderDto> getAllByPatient(Long patientId) {
        log.debug("Getting FHIR CareTeams for patient: {}", patientId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(CareTeam.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .returnBundle(Bundle.class)
                .execute();

        List<CareTeam> careTeams = fhirClientService.extractResources(bundle, CareTeam.class);
        return careTeams.stream().map(this::fromFhirCareTeam).collect(Collectors.toList());
    }

    // LIST BY ENCOUNTER
    public List<AssignedProviderDto> list(Long patientId, Long encounterId) {
        log.debug("Getting FHIR CareTeams for patient: {} encounter: {}", patientId, encounterId);

        // Search by patient and filter by encounter extension
        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(CareTeam.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .returnBundle(Bundle.class)
                .execute();

        List<CareTeam> careTeams = fhirClientService.extractResources(bundle, CareTeam.class);
        
        // Filter by encounter
        return careTeams.stream()
                .filter(ct -> hasEncounter(ct, encounterId))
                .map(this::fromFhirCareTeam)
                .collect(Collectors.toList());
    }

    // UPDATE
    public AssignedProviderDto update(Long patientId, Long encounterId, String fhirId, AssignedProviderDto dto) {
        log.debug("Updating FHIR CareTeam: {}", fhirId);

        // Check if signed
        CareTeam existing = fhirClientService.read(CareTeam.class, fhirId, getPracticeId());
        AssignedProviderDto existingDto = fromFhirCareTeam(existing);
        if (Boolean.TRUE.equals(existingDto.getESigned())) {
            throw new IllegalStateException("Signed records are read-only.");
        }

        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);

        CareTeam careTeam = toFhirCareTeam(dto);
        careTeam.setId(fhirId);
        fhirClientService.update(careTeam, getPracticeId());

        return getOne(patientId, encounterId, fhirId);
    }

    // DELETE
    public void delete(Long patientId, Long encounterId, String fhirId) {
        log.debug("Deleting FHIR CareTeam: {}", fhirId);

        // Check if signed
        CareTeam existing = fhirClientService.read(CareTeam.class, fhirId, getPracticeId());
        AssignedProviderDto existingDto = fromFhirCareTeam(existing);
        if (Boolean.TRUE.equals(existingDto.getESigned())) {
            throw new IllegalStateException("Signed records cannot be deleted.");
        }

        fhirClientService.delete(CareTeam.class, fhirId, getPracticeId());
    }

    // E-SIGN
    public AssignedProviderDto eSign(Long patientId, Long encounterId, String fhirId, String signedBy) {
        log.debug("E-signing FHIR CareTeam: {}", fhirId);

        CareTeam careTeam = fhirClientService.read(CareTeam.class, fhirId, getPracticeId());
        AssignedProviderDto dto = fromFhirCareTeam(careTeam);

        if (Boolean.TRUE.equals(dto.getESigned())) {
            return dto; // Already signed
        }

        // Add e-sign extensions
        String now = OffsetDateTime.now(ZoneOffset.UTC).toString();
        careTeam.addExtension(new Extension(EXT_ESIGNED, new BooleanType(true)));
        careTeam.addExtension(new Extension(EXT_SIGNED_AT, new StringType(now)));
        careTeam.addExtension(new Extension(EXT_SIGNED_BY, new StringType(StringUtils.hasText(signedBy) ? signedBy : "system")));

        fhirClientService.update(careTeam, getPracticeId());

        dto.setESigned(true);
        dto.setSignedAt(OffsetDateTime.now(ZoneOffset.UTC));
        dto.setSignedBy(StringUtils.hasText(signedBy) ? signedBy : "system");
        return dto;
    }

    // RENDER PDF
    public byte[] renderPdf(Long patientId, Long encounterId, String fhirId) {
        CareTeam careTeam = fhirClientService.read(CareTeam.class, fhirId, getPracticeId());
        AssignedProviderDto dto = fromFhirCareTeam(careTeam);

        // Update printed timestamp
        String now = OffsetDateTime.now(ZoneOffset.UTC).toString();
        careTeam.addExtension(new Extension(EXT_PRINTED_AT, new StringType(now)));
        fhirClientService.update(careTeam, getPracticeId());

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float x = 64, y = 740;

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.newLineAtOffset(x, y);
                cs.showText("Assigned Provider");
                cs.endText();

                y -= 26;
                draw(cs, x, y, "Patient ID:", String.valueOf(dto.getPatientId())); y -= 16;
                draw(cs, x, y, "Encounter ID:", String.valueOf(dto.getEncounterId())); y -= 16;
                draw(cs, x, y, "FHIR ID:", fhirId); y -= 22;

                draw(cs, x, y, "Provider ID:", String.valueOf(dto.getProviderId())); y -= 16;
                draw(cs, x, y, "Role:", nullTo(dto.getRole(), "-")); y -= 16;
                draw(cs, x, y, "Start Date:", nullTo(dto.getStartDate(), "-")); y -= 16;
                draw(cs, x, y, "End Date:", nullTo(dto.getEndDate(), "-")); y -= 16;
                draw(cs, x, y, "Status:", nullTo(dto.getStatus(), "-")); y -= 22;
                draw(cs, x, y, "Notes:", nullTo(dto.getNotes(), "-")); y -= 22;

                draw(cs, x, y, "eSigned:", Boolean.TRUE.equals(dto.getESigned()) ? "Yes" : "No"); y -= 16;
                if (dto.getSignedAt() != null) { draw(cs, x, y, "Signed At:", dto.getSignedAt().toString()); y -= 16; }
                if (StringUtils.hasText(dto.getSignedBy())) { draw(cs, x, y, "Signed By:", dto.getSignedBy()); }
            }

            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate Assigned Provider PDF", ex);
        }
    }

    // -------- FHIR Mapping --------

    private CareTeam toFhirCareTeam(AssignedProviderDto dto) {
        CareTeam ct = new CareTeam();

        // Status
        if ("inactive".equalsIgnoreCase(dto.getStatus())) {
            ct.setStatus(CareTeam.CareTeamStatus.INACTIVE);
        } else {
            ct.setStatus(CareTeam.CareTeamStatus.ACTIVE);
        }

        // Subject (Patient)
        if (dto.getPatientId() != null) {
            ct.setSubject(new Reference("Patient/" + dto.getPatientId()));
        }

        // Encounter extension
        if (dto.getEncounterId() != null) {
            ct.addExtension(new Extension(EXT_ENCOUNTER, new Reference("Encounter/" + dto.getEncounterId())));
        }

        // Participant (Provider)
        if (dto.getProviderId() != null) {
            CareTeam.CareTeamParticipantComponent participant = ct.addParticipant();
            participant.setMember(new Reference("Practitioner/" + dto.getProviderId()));
            
            if (dto.getRole() != null) {
                participant.addRole().setText(dto.getRole());
            }

            // Period
            Period period = new Period();
            if (dto.getStartDate() != null) {
                period.setStartElement(new DateTimeType(dto.getStartDate()));
            }
            if (dto.getEndDate() != null) {
                period.setEndElement(new DateTimeType(dto.getEndDate()));
            }
            if (period.hasStart() || period.hasEnd()) {
                participant.setPeriod(period);
            }
        }

        // Notes extension
        if (dto.getNotes() != null) {
            ct.addExtension(new Extension(EXT_NOTES, new StringType(dto.getNotes())));
        }

        // E-sign extensions
        if (Boolean.TRUE.equals(dto.getESigned())) {
            ct.addExtension(new Extension(EXT_ESIGNED, new BooleanType(true)));
        }
        if (dto.getSignedAt() != null) {
            ct.addExtension(new Extension(EXT_SIGNED_AT, new StringType(dto.getSignedAt().toString())));
        }
        if (dto.getSignedBy() != null) {
            ct.addExtension(new Extension(EXT_SIGNED_BY, new StringType(dto.getSignedBy())));
        }
        if (dto.getPrintedAt() != null) {
            ct.addExtension(new Extension(EXT_PRINTED_AT, new StringType(dto.getPrintedAt().toString())));
        }

        return ct;
    }

    private AssignedProviderDto fromFhirCareTeam(CareTeam ct) {
        AssignedProviderDto dto = new AssignedProviderDto();
        String fhirId = ct.getIdElement().getIdPart();
        dto.setId(Long.parseLong(fhirId));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);

        // Status
        if (ct.hasStatus()) {
            dto.setStatus(ct.getStatus() == CareTeam.CareTeamStatus.ACTIVE ? "active" : "inactive");
        }

        // Subject -> patientId
        if (ct.hasSubject() && ct.getSubject().hasReference()) {
            String ref = ct.getSubject().getReference();
            if (ref.startsWith("Patient/")) {
                try {
                    dto.setPatientId(Long.parseLong(ref.substring("Patient/".length())));
                } catch (NumberFormatException ignored) {}
            }
        }

        // Encounter extension
        Extension encExt = ct.getExtensionByUrl(EXT_ENCOUNTER);
        if (encExt != null && encExt.getValue() instanceof Reference) {
            String ref = ((Reference) encExt.getValue()).getReference();
            if (ref != null && ref.startsWith("Encounter/")) {
                try {
                    dto.setEncounterId(Long.parseLong(ref.substring("Encounter/".length())));
                } catch (NumberFormatException ignored) {}
            }
        }

        // Participant -> providerId, role, period
        if (ct.hasParticipant()) {
            CareTeam.CareTeamParticipantComponent participant = ct.getParticipantFirstRep();
            if (participant.hasMember() && participant.getMember().hasReference()) {
                String ref = participant.getMember().getReference();
                if (ref.startsWith("Practitioner/")) {
                    try {
                        dto.setProviderId(Long.parseLong(ref.substring("Practitioner/".length())));
                    } catch (NumberFormatException ignored) {}
                }
            }
            if (participant.hasRole()) {
                dto.setRole(participant.getRoleFirstRep().getText());
            }
            if (participant.hasPeriod()) {
                Period p = participant.getPeriod();
                if (p.hasStart()) {
                    dto.setStartDate(p.getStartElement().getValueAsString());
                }
                if (p.hasEnd()) {
                    dto.setEndDate(p.getEndElement().getValueAsString());
                }
            }
        }

        // Notes extension
        Extension notesExt = ct.getExtensionByUrl(EXT_NOTES);
        if (notesExt != null && notesExt.getValue() instanceof StringType) {
            dto.setNotes(((StringType) notesExt.getValue()).getValue());
        }

        // E-sign extensions
        Extension esignedExt = ct.getExtensionByUrl(EXT_ESIGNED);
        if (esignedExt != null && esignedExt.getValue() instanceof BooleanType) {
            dto.setESigned(((BooleanType) esignedExt.getValue()).booleanValue());
        }
        Extension signedAtExt = ct.getExtensionByUrl(EXT_SIGNED_AT);
        if (signedAtExt != null && signedAtExt.getValue() instanceof StringType) {
            try {
                dto.setSignedAt(OffsetDateTime.parse(((StringType) signedAtExt.getValue()).getValue()));
            } catch (Exception ignored) {}
        }
        Extension signedByExt = ct.getExtensionByUrl(EXT_SIGNED_BY);
        if (signedByExt != null && signedByExt.getValue() instanceof StringType) {
            dto.setSignedBy(((StringType) signedByExt.getValue()).getValue());
        }
        Extension printedAtExt = ct.getExtensionByUrl(EXT_PRINTED_AT);
        if (printedAtExt != null && printedAtExt.getValue() instanceof StringType) {
            try {
                dto.setPrintedAt(OffsetDateTime.parse(((StringType) printedAtExt.getValue()).getValue()));
            } catch (Exception ignored) {}
        }
        
        if (ct.hasMeta()) {
            populateAudit(dto, ct.getMeta());
        }

        return dto;
    }

    // -------- Helpers --------

    private boolean hasEncounter(CareTeam ct, Long encounterId) {
        Extension encExt = ct.getExtensionByUrl(EXT_ENCOUNTER);
        if (encExt != null && encExt.getValue() instanceof Reference) {
            String ref = ((Reference) encExt.getValue()).getReference();
            return ref != null && ref.equals("Encounter/" + encounterId);
        }
        return false;
    }

    private static void draw(PDPageContentStream cs, float x, float y, String label, String value) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, 12); cs.newLineAtOffset(x, y); cs.showText(label); cs.endText();
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 12); cs.newLineAtOffset(x + 140, y); cs.showText(value != null ? value : "-"); cs.endText();
    }

    private static String nullTo(String v, String fb) { return (v == null || v.isBlank()) ? fb : v; }
    
    private void populateAudit(AssignedProviderDto dto, Meta meta) {
        AssignedProviderDto.Audit audit = new AssignedProviderDto.Audit();
        if (meta.hasLastUpdated()) {
            audit.setLastModifiedDate(meta.getLastUpdated().toInstant().atOffset(ZoneOffset.UTC).toLocalDate().toString());
            audit.setCreatedDate(meta.getLastUpdated().toInstant().atOffset(ZoneOffset.UTC).toLocalDate().toString());
        }
        dto.setAudit(audit);
    }
}
