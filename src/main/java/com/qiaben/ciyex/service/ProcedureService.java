package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import com.qiaben.ciyex.dto.ProcedureDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
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
    private final PatientBillingService billingService;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    public ProcedureService(FhirClientService fhirClientService, PracticeContextService practiceContextService, PatientBillingService billingService) {
        this.fhirClientService = fhirClientService;
        this.practiceContextService = practiceContextService;
        this.billingService = billingService;
    }

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }


    // ✅ Get all by patient
    public List<ProcedureDto> getAllByPatient(Long patientId) {
        log.debug("Getting FHIR Procedures for patient: {}", patientId);

        Bundle bundle = fhirClientService.getClient().search()
                .forResource(org.hl7.fhir.r4.model.Procedure.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .withAdditionalHeader("X-Request-Tenant-Id", getPracticeId())
                .returnBundle(Bundle.class)
                .execute();

        return extractProcedureDtos(bundle, patientId, null);
    }

    // ✅ Get all by encounter
    public List<ProcedureDto> getAllByEncounter(Long patientId, Long encounterId) {
        log.debug("Getting FHIR Procedures for patient: {}, encounter: {}", patientId, encounterId);

        Bundle bundle = fhirClientService.getClient().search()
                .forResource(org.hl7.fhir.r4.model.Procedure.class)
                .where(new ReferenceClientParam("subject").hasId("Patient/" + patientId))
                .where(new ReferenceClientParam("encounter").hasId("Encounter/" + encounterId))
                .withAdditionalHeader("X-Request-Tenant-Id", getPracticeId())
                .returnBundle(Bundle.class)
                .execute();

        return extractProcedureDtos(bundle, patientId, encounterId);
    }

    // ✅ Create Procedure
    public ProcedureDto create(Long patientId, Long encounterId, ProcedureDto dto) {
        log.info("Creating Procedure in FHIR for patient: {}, encounter: {}", patientId, encounterId);

        org.hl7.fhir.r4.model.Procedure procedure = toFhirProcedure(dto, patientId, encounterId);
        MethodOutcome outcome = fhirClientService.create(procedure, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setPatientId(patientId);
        dto.setEncounterId(encounterId);

        // Create invoice for the procedure
        createInvoiceForProcedure(patientId, dto);

        log.info("Created FHIR Procedure with ID: {}", fhirId);
        return dto;
    }

    // ✅ Create multiple procedures
    public List<ProcedureDto> createMultiple(Long patientId, Long encounterId, ProcedureDto request) {
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
        String fhirId = String.valueOf(id);
        log.debug("Getting FHIR Procedure with ID: {}", fhirId);

        try {
            org.hl7.fhir.r4.model.Procedure procedure = fhirClientService.read(
                    org.hl7.fhir.r4.model.Procedure.class, fhirId, getPracticeId());
            return toProcedureDto(procedure, patientId, encounterId);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Procedure not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id));
        }
    }

    // ✅ Update Procedure
    public ProcedureDto update(Long patientId, Long encounterId, Long id, ProcedureDto dto) {
        String fhirId = String.valueOf(id);
        log.info("Updating FHIR Procedure with ID: {}", fhirId);

        org.hl7.fhir.r4.model.Procedure procedure = toFhirProcedure(dto, patientId, encounterId);
        procedure.setId(fhirId);
        fhirClientService.update(procedure, getPracticeId());

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        return dto;
    }

    // ✅ Delete Procedure
    public void delete(Long patientId, Long encounterId, Long id) {
        String fhirId = String.valueOf(id);
        log.info("Deleting FHIR Procedure with ID: {}", fhirId);

        fhirClientService.delete(org.hl7.fhir.r4.model.Procedure.class, fhirId, getPracticeId());
    }

    // ========== Invoice Helper ==========

    private void createInvoiceForProcedure(Long patientId, ProcedureDto dto) {
        try {
            List<PatientBillingService.ProcedureLineRequest> procedureLines = new ArrayList<>();

            java.math.BigDecimal rateValue;
            try {
                rateValue = dto.getRate() != null ? new java.math.BigDecimal(dto.getRate()) : java.math.BigDecimal.ZERO;
            } catch (Exception ex) {
                rateValue = java.math.BigDecimal.ZERO;
                log.warn("Invalid rate format for procedure. Defaulting to 0.");
            }

            procedureLines.add(new PatientBillingService.ProcedureLineRequest(
                    dto.getCpt4(),
                    dto.getDescription(),
                    rateValue
            ));

            PatientBillingService.CreateInvoiceRequest invoiceRequest = new PatientBillingService.CreateInvoiceRequest(
                    dto.getProvidername(),
                    dto.getHospitalBillingStart(),
                    procedureLines
            );
            billingService.createInvoiceFromProcedure(patientId, invoiceRequest);
            log.info("Invoice automatically created for procedure");
        } catch (Exception e) {
            log.error("Failed to create invoice for procedure", e);
        }
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
            dto.setFhirId(procedure.getIdElement().getIdPart());
            dto.setExternalId(procedure.getIdElement().getIdPart());
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
}
