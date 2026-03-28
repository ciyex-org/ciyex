package org.ciyex.ehr.service;

import org.ciyex.ehr.dto.PatientBillingNoteDto;
import org.ciyex.ehr.dto.NoteTargetType;
import org.ciyex.ehr.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-backed Patient Billing Note Service
 * Uses FHIR Observation resources to store patient billing notes
 * All business logic from original implementation preserved
 * No local database storage - all data stored in FHIR server
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientBillingNoteService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;
    private final PatientInvoiceService invoiceService;

    // FHIR Extensions for Billing Notes
    private static final String EXT_PATIENT = "http://ciyex.com/fhir/StructureDefinition/patient-reference";
    private static final String EXT_INVOICE = "http://ciyex.com/fhir/StructureDefinition/invoice-reference";
    private static final String EXT_TARGET_TYPE = "http://ciyex.com/fhir/StructureDefinition/note-target-type";
    private static final String EXT_TARGET_ID = "http://ciyex.com/fhir/StructureDefinition/note-target-id";
    private static final String EXT_CREATED_BY = "http://ciyex.com/fhir/StructureDefinition/note-created-by";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    /* ===================== Notes (FHIR-backed) ===================== */

    /**
     * List notes for a specific invoice (targetType=INVOICE, targetId=invoiceId)
     * Verifies invoice exists and belongs to patient before returning notes
     */
    public List<PatientBillingNoteDto> listInvoiceNotes(Long patientId, Long invoiceId) {
        log.debug("Listing notes for patient {} invoice {}", patientId, invoiceId);
        // Verify invoice exists and belongs to patient
        getInvoiceOrThrow(patientId, invoiceId);

        List<Observation> allObs = new ArrayList<>();
        Bundle bundle = fhirClientService.search(Observation.class, getPracticeId());
        
        // Handle pagination
        while (bundle != null) {
            List<Observation> pageObs = fhirClientService.extractResources(bundle, Observation.class);
            allObs.addAll(pageObs);
            
            String nextLink = bundle.getLink(Bundle.LINK_NEXT) != null 
                ? bundle.getLink(Bundle.LINK_NEXT).getUrl() 
                : null;
            
            if (nextLink != null) {
                bundle = fhirClientService.loadPage(nextLink, getPracticeId());
            } else {
                break;
            }
        }
        
        log.info("Total observations loaded: {}, filtering for patient {} invoice {}", 
            allObs.size(), patientId, invoiceId);
        
        return allObs.stream()
                .filter(obs -> {
                    boolean isBilling = isBillingNote(obs);
                    Long pid = getPatientIdFromNote(obs);
                    Long iid = getInvoiceIdFromNote(obs);
                    String targetType = getTargetTypeFromNote(obs);
                    
                    boolean matches = isBilling && patientId.equals(pid) && 
                                    invoiceId.equals(iid) && "INVOICE".equals(targetType);
                    
                    if (isBilling) {
                        log.debug("Note {}: patientId={}, invoiceId={}, targetType={}, matches={}",
                            obs.getIdElement().getIdPart(), pid, iid, targetType, matches);
                    }
                    
                    return matches;
                })
                .sorted((a, b) -> {
                    Date da = a.getIssued();
                    Date db = b.getIssued();
                    if (da == null && db == null) return 0;
                    if (da == null) return 1;
                    if (db == null) return -1;
                    return db.compareTo(da); // Newest first
                })
                .map(this::fromFhirObservation)
                .collect(Collectors.toList());
    }

    /**
     * Create a note for a specific invoice
     * Validates invoice exists and belonging to patient
     * Sets targetType=INVOICE and targetId=invoiceId from path (immutable)
     */
    public PatientBillingNoteDto createInvoiceNote(Long patientId, Long invoiceId, PatientBillingNoteDto dto) {
        log.debug("Creating note for patient {} invoice {}", patientId, invoiceId);
        // Verify invoice exists and belongs to patient
        getInvoiceOrThrow(patientId, invoiceId);

        if (dto == null || dto.text == null) {
            throw new IllegalArgumentException("Required fields missing for note creation: text");
        }

        // If provided in body, validate against path (optional: can remove if not needed)
        if (dto.patientId != null && !dto.patientId.equals(patientId)) {
            throw new IllegalArgumentException("Patient ID mismatch");
        }
        if (dto.invoiceId != null && !dto.invoiceId.equals(invoiceId)) {
            throw new IllegalArgumentException("Invoice ID mismatch");
        }

        // Create FHIR Observation for note
        Observation obs = new Observation();
        obs.setStatus(Observation.ObservationStatus.FINAL);
        obs.setCode(new CodeableConcept(new Coding("http://ciyex.com", "billing-note", "Billing Note")));
        obs.setSubject(new Reference("Patient/" + patientId));
        obs.setEffective(new DateTimeType(java.util.Calendar.getInstance()));
        obs.setIssued(new Date());

        // Add note text
        obs.addNote().setText(dto.text);

        // Add extensions - always from path, never from body
        addExtension(obs, EXT_PATIENT, new StringType(String.valueOf(patientId)));
        addExtension(obs, EXT_INVOICE, new StringType(String.valueOf(invoiceId)));
        addExtension(obs, EXT_TARGET_TYPE, new StringType("INVOICE"));
        addExtension(obs, EXT_TARGET_ID, new StringType(String.valueOf(invoiceId)));

        // Add createdBy if provided
        if (dto.createdBy != null) {
            addExtension(obs, EXT_CREATED_BY, new StringType(dto.createdBy));
        }

        var outcome = fhirClientService.create(obs, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        log.info("Created FHIR note with id: {} for patient {} invoice {}", fhirId, patientId, invoiceId);
        Observation created = fhirClientService.read(Observation.class, fhirId, getPracticeId());
        return fromFhirObservation(created);
    }

    /**
     * Update a note for an invoice
     * Validates note belongs to patient and invoice
     * targetType and targetId are immutable
     */
    public PatientBillingNoteDto updateInvoiceNote(Long patientId, Long invoiceId, Long noteId, PatientBillingNoteDto dto) {
        log.debug("Updating note {} for patient {} invoice {}", noteId, patientId, invoiceId);

        String fhirId = String.valueOf(noteId);
        Observation obs = fhirClientService.read(Observation.class, fhirId, getPracticeId());

        if (obs == null) {
            throw new IllegalArgumentException(
                String.format("Note not found with ID: %d. Please provide a valid Note ID.", noteId)
            );
        }

        Long obsPatientId = getPatientIdFromNote(obs);
        Long obsInvoiceId = getInvoiceIdFromNote(obs);

        if (!patientId.equals(obsPatientId)) {
            throw new IllegalArgumentException("Note does not belong to this patient");
        }
        if (!"INVOICE".equals(getTargetTypeFromNote(obs)) || !invoiceId.equals(obsInvoiceId)) {
            throw new IllegalArgumentException("Note does not belong to this invoice");
        }

        // Update text if provided
        if (dto.text != null) {
            obs.getNote().clear();
            obs.addNote().setText(dto.text);
        }

        // Update createdBy if provided
        if (dto.createdBy != null) {
            updateExtension(obs, EXT_CREATED_BY, new StringType(dto.createdBy));
        }

        // targetType and targetId are immutable - do not update

        fhirClientService.update(obs, getPracticeId());
        log.info("Updated FHIR note {} for patient {} invoice {}", noteId, patientId, invoiceId);

        Observation updated = fhirClientService.read(Observation.class, fhirId, getPracticeId());
        return fromFhirObservation(updated);
    }

    /**
     * Delete a note for an invoice
     * Validates note belongs to patient and invoice before deletion
     */
    public void deleteInvoiceNote(Long patientId, Long invoiceId, Long noteId) {
        log.debug("Deleting note {} for patient {} invoice {}", noteId, patientId, invoiceId);

        String fhirId = String.valueOf(noteId);
        Observation obs = fhirClientService.read(Observation.class, fhirId, getPracticeId());

        if (obs == null) {
            throw new IllegalArgumentException(
                String.format("Note not found with ID: %d. Please provide a valid Note ID.", noteId)
            );
        }

        Long obsPatientId = getPatientIdFromNote(obs);
        Long obsInvoiceId = getInvoiceIdFromNote(obs);

        if (!patientId.equals(obsPatientId)) {
            throw new IllegalArgumentException("Note does not belong to this patient");
        }
        if (!"INVOICE".equals(getTargetTypeFromNote(obs)) || !invoiceId.equals(obsInvoiceId)) {
            throw new IllegalArgumentException("Note does not belong to this invoice");
        }

        fhirClientService.delete(Observation.class, fhirId, getPracticeId());
        log.info("Deleted FHIR note {} for patient {} invoice {}", noteId, patientId, invoiceId);
    }

    /* ===================== FHIR Mapping ===================== */

    /**
     * Convert FHIR Observation to PatientBillingNoteDto
     */
    private PatientBillingNoteDto fromFhirObservation(Observation obs) {
        PatientBillingNoteDto dto = new PatientBillingNoteDto();

        String fhirId = obs.getIdElement().getIdPart();
        try {
            dto.id = Long.parseLong(fhirId);
        } catch (NumberFormatException e) {
            dto.id = Long.valueOf(Math.abs(fhirId.hashCode()));
        }

        dto.patientId = getPatientIdFromNote(obs);
        dto.invoiceId = getInvoiceIdFromNote(obs);

        String targetTypeStr = getTargetTypeFromNote(obs);
        try {
            dto.type = NoteTargetType.valueOf(targetTypeStr != null ? targetTypeStr : "INVOICE");
        } catch (Exception ex) {
            dto.type = NoteTargetType.INVOICE;
        }

        Long targetId = getLongExtValue(obs, EXT_TARGET_ID);
        if (targetId != null) {
            dto.targetId = targetId;
        }

        if (obs.hasNote() && !obs.getNote().isEmpty()) {
            dto.text = obs.getNoteFirstRep().getText();
        }

        dto.createdBy = optStringExt(obs, EXT_CREATED_BY);

        Date issued = obs.getIssued();
        if (issued != null) {
            dto.createdAt = OffsetDateTime.ofInstant(issued.toInstant(), ZoneOffset.UTC);
        }

        return dto;
    }

    /* ===================== Helpers ===================== */

    /**
     * Check if observation is a billing note
     */
    private boolean isBillingNote(Observation obs) {
        if (obs == null || !obs.hasCode() || !obs.getCode().hasCoding()) {
            return false;
        }
        return obs.getCode().getCoding().stream()
                .anyMatch(c -> "billing-note".equals(c.getCode()));
    }

    /**
     * Validate invoice exists and belongs to patient
     */
    private void getInvoiceOrThrow(Long patientId, Long invoiceId) {
        invoiceService.getPatientInvoice(patientId, invoiceId);
        // If no exception thrown, invoice exists and belongs to patient
    }

    /**
     * Extract patient ID from note observation
     */
    private Long getPatientIdFromNote(Observation obs) {
        return getLongExtValue(obs, EXT_PATIENT);
    }

    /**
     * Extract invoice ID from note observation
     */
    private Long getInvoiceIdFromNote(Observation obs) {
        return getLongExtValue(obs, EXT_INVOICE);
    }

    /**
     * Extract target type from note observation
     */
    private String getTargetTypeFromNote(Observation obs) {
        return optStringExt(obs, EXT_TARGET_TYPE);
    }

    /**
     * Add extension to FHIR resource
     */
    private void addExtension(DomainResource resource, String url, Type value) {
        Extension ext = new Extension();
        ext.setUrl(url);
        ext.setValue(value);
        resource.getExtension().add(ext);
    }

    /**
     * Update extension on FHIR resource
     */
    private void updateExtension(DomainResource resource, String url, Type value) {
        resource.getExtension().stream()
                .filter(e -> url.equals(e.getUrl()))
                .findFirst()
                .ifPresentOrElse(
                        e -> e.setValue(value),
                        () -> addExtension(resource, url, value)
                );
    }

    /**
     * Safely extract Long value from extension
     */
    private Long getLongExtValue(Observation obs, String url) {
        String val = optStringExt(obs, url);
        if (val != null && !val.isEmpty()) {
            try {
                return Long.parseLong(val);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse Long from extension {}: {}", url, val);
            }
        }
        return null;
    }

    /**
     * Safely extract String value from extension
     */
    private String optStringExt(Observation obs, String url) {
        return obs.getExtension().stream()
                .filter(e -> url.equals(e.getUrl()))
                .findFirst()
                .map(e -> {
                    if (e.getValue() instanceof StringType) {
                        return ((StringType) e.getValue()).getValue();
                    }
                    return e.getValue() != null ? e.getValue().toString() : null;
                })
                .orElse(null);
    }
}
