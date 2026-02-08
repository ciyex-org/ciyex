package org.ciyex.ehr.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import org.ciyex.ehr.dto.ProcedureDto;
import org.ciyex.ehr.fhir.FhirClientService;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Procedure Service - FHIR Only.
 * All procedure data is stored in HAPI FHIR server as Procedure resources.
 */
@Service
@Slf4j
public class ProcedureService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    public ProcedureService(FhirClientService fhirClientService, PracticeContextService practiceContextService) {
        this.fhirClientService = fhirClientService;
        this.practiceContextService = practiceContextService;
    }

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }


    // ✅ Get all by patient
    public List<ProcedureDto> getAllByPatient(Long patientId) {
        validatePathVariable(patientId, "Patient ID");
        validatePatientExists(patientId);
        log.debug("Getting FHIR Procedures for patient: {}", patientId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(org.hl7.fhir.r4.model.Procedure.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                
                .returnBundle(Bundle.class)
                .execute();

        return extractProcedureDtos(bundle, patientId, null);
    }

    // ✅ Get all by encounter
    public List<ProcedureDto> getAllByEncounter(Long patientId, Long encounterId) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        log.debug("Getting FHIR Procedures for patient: {}, encounter: {}", patientId, encounterId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(org.hl7.fhir.r4.model.Procedure.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .where(new ReferenceClientParam("encounter").hasId("Encounter/" + encounterId))
                
                .returnBundle(Bundle.class)
                .execute();

        return extractProcedureDtos(bundle, patientId, encounterId);
    }

    // ✅ Create Procedure
    public ProcedureDto create(Long patientId, Long encounterId, ProcedureDto dto) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        log.info("Creating Procedure in FHIR for patient: {}, encounter: {}", patientId, encounterId);

        org.hl7.fhir.r4.model.Procedure procedure = toFhirProcedure(dto, patientId, encounterId);
        MethodOutcome outcome = fhirClientService.create(procedure, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setId(Long.parseLong(fhirId));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);
        
        org.hl7.fhir.r4.model.Procedure created = (org.hl7.fhir.r4.model.Procedure) outcome.getResource();
        if (created != null && created.hasMeta()) {
            populateAudit(dto, created.getMeta());
        }

        // Create invoice for the procedure
        createInvoiceForProcedure(patientId, dto);

        log.info("Created FHIR Procedure with ID: {}", fhirId);
        return dto;
    }

    // ✅ Create multiple procedures
    public List<ProcedureDto> createMultiple(Long patientId, Long encounterId, ProcedureDto request) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        List<ProcedureDto> createdProcedures = new ArrayList<>();

        if (request.getCodeItems() != null && !request.getCodeItems().isEmpty()) {
            for (ProcedureDto.CodeItem item : request.getCodeItems()) {
                ProcedureDto dto = new ProcedureDto();
                dto.setCpt4(item.getCpt4());
                dto.setDescription(item.getDescription());
                dto.setUnits(item.getUnits());
                dto.setRate(item.getRate());
                dto.setRelatedIcds(item.getRelatedIcds());
                dto.setModifier1(item.getModifier1());
                dto.setHospitalBillingStart(request.getHospitalBillingStart());
                dto.setHospitalBillingEnd(request.getHospitalBillingEnd());
                dto.setNote(request.getNote());
                dto.setPriceLevelTitle(request.getPriceLevelTitle());
                dto.setPriceLevelId(request.getPriceLevelId());
                dto.setProvidername(request.getProvidername());

                createdProcedures.add(create(patientId, encounterId, dto));
            }
        } else {
            createdProcedures.add(create(patientId, encounterId, request));
        }

        return createdProcedures;
    }

    // ✅ Get one Procedure
    public ProcedureDto getOne(Long patientId, Long encounterId, Long id) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validatePathVariable(id, "Procedure ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        String fhirId = String.valueOf(id);
        log.debug("Getting FHIR Procedure with ID: {}", fhirId);

        try {
            org.hl7.fhir.r4.model.Procedure procedure = fhirClientService.read(
                    org.hl7.fhir.r4.model.Procedure.class, fhirId, getPracticeId());
            ProcedureDto dto = toProcedureDto(procedure, patientId, encounterId);
            dto.setId(id);
            return dto;
        } catch (Exception e) {
            throw new IllegalArgumentException("Procedure ID is invalid. Procedure not found: " + id);
        }
    }

    // ✅ Update Procedure
    public ProcedureDto update(Long patientId, Long encounterId, Long id, ProcedureDto dto) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validatePathVariable(id, "Procedure ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        String fhirId = String.valueOf(id);
        log.info("Updating FHIR Procedure with ID: {}", fhirId);

        // Validate resource exists
        try {
            fhirClientService.read(org.hl7.fhir.r4.model.Procedure.class, fhirId, getPracticeId());
        } catch (Exception e) {
            throw new IllegalArgumentException("Procedure ID is invalid. Procedure not found: " + id);
        }

        org.hl7.fhir.r4.model.Procedure procedure = toFhirProcedure(dto, patientId, encounterId);
        procedure.setId(fhirId);
        fhirClientService.update(procedure, getPracticeId());

        return getOne(patientId, encounterId, id);
    }

    // ✅ Delete Procedure
    public void delete(Long patientId, Long encounterId, Long id) {
        validatePathVariable(patientId, "Patient ID");
        validatePathVariable(encounterId, "Encounter ID");
        validatePathVariable(id, "Procedure ID");
        validatePatientExists(patientId);
        validateEncounterExists(encounterId);
        String fhirId = String.valueOf(id);
        log.info("Deleting FHIR Procedure with ID: {}", fhirId);

        // Validate resource exists
        try {
            fhirClientService.read(org.hl7.fhir.r4.model.Procedure.class, fhirId, getPracticeId());
        } catch (Exception e) {
            throw new IllegalArgumentException("Procedure ID is invalid. Procedure not found: " + id);
        }

        fhirClientService.delete(org.hl7.fhir.r4.model.Procedure.class, fhirId, getPracticeId());
    }

    // ========== Invoice Helper ==========
    // Note: Invoice creation removed - PatientBillingService was deleted
    private void createInvoiceForProcedure(Long patientId, ProcedureDto dto) {
        log.debug("Invoice creation skipped - billing service not available");
    }

    // ========== FHIR Mapping Methods ==========

    private org.hl7.fhir.r4.model.Procedure toFhirProcedure(ProcedureDto dto, Long patientId, Long encounterId) {
        org.hl7.fhir.r4.model.Procedure procedure = new org.hl7.fhir.r4.model.Procedure();

        // Patient reference
        procedure.setSubject(new Reference("Patient/" + patientId));

        // Encounter reference
        if (encounterId != null) {
            procedure.setEncounter(new Reference("Encounter/" + encounterId));
        }

        // Status
        procedure.setStatus(org.hl7.fhir.r4.model.Procedure.ProcedureStatus.COMPLETED);

        // Code (CPT4)
        if (dto.getCpt4() != null) {
            CodeableConcept code = new CodeableConcept();
            code.addCoding()
                    .setSystem("http://www.ama-assn.org/go/cpt")
                    .setCode(dto.getCpt4())
                    .setDisplay(dto.getDescription());
            code.setText(dto.getDescription());
            procedure.setCode(code);
        }

        // Note
        if (dto.getNote() != null) {
            procedure.addNote().setText(dto.getNote());
        }

        // Performer (provider)
        if (dto.getProvidername() != null) {
            org.hl7.fhir.r4.model.Procedure.ProcedurePerformerComponent performer = procedure.addPerformer();
            performer.setActor(new Reference().setDisplay(dto.getProvidername()));
        }

        return procedure;
    }

    private ProcedureDto toProcedureDto(org.hl7.fhir.r4.model.Procedure procedure, Long patientId, Long encounterId) {
        ProcedureDto dto = new ProcedureDto();

        if (procedure.hasId()) {
            String fhirId = procedure.getIdElement().getIdPart();
            dto.setId(Long.parseLong(fhirId));
            dto.setFhirId(fhirId);
            dto.setExternalId(fhirId);
        }

        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);

        // Code (CPT4)
        if (procedure.hasCode()) {
            CodeableConcept code = procedure.getCode();
            if (code.hasCoding()) {
                Coding coding = code.getCodingFirstRep();
                dto.setCpt4(coding.getCode());
                dto.setDescription(coding.getDisplay());
            }
            if (code.hasText()) {
                dto.setDescription(code.getText());
            }
        }

        // Note
        if (procedure.hasNote()) {
            dto.setNote(procedure.getNoteFirstRep().getText());
        }

        // Performer (provider)
        if (procedure.hasPerformer()) {
            org.hl7.fhir.r4.model.Procedure.ProcedurePerformerComponent performer = procedure.getPerformerFirstRep();
            if (performer.hasActor() && performer.getActor().hasDisplay()) {
                dto.setProvidername(performer.getActor().getDisplay());
            }
        }
        
        if (procedure.hasMeta()) {
            populateAudit(dto, procedure.getMeta());
        }

        return dto;
    }

    private List<ProcedureDto> extractProcedureDtos(Bundle bundle, Long patientId, Long encounterId) {
        List<ProcedureDto> items = new ArrayList<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof org.hl7.fhir.r4.model.Procedure) {
                    items.add(toProcedureDto((org.hl7.fhir.r4.model.Procedure) entry.getResource(), patientId, encounterId));
                }
            }
        }
        return items;
    }
    
    private void populateAudit(ProcedureDto dto, Meta meta) {
        ProcedureDto.Audit audit = new ProcedureDto.Audit();
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