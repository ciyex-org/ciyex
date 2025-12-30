package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.ImmunizationDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Immunization Service - FHIR Only.
 * All immunization data is stored in HAPI FHIR server as Immunization resources.
 */
@Service
@Slf4j
public class ImmunizationService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    @Autowired
    public ImmunizationService(FhirClientService fhirClientService, PracticeContextService practiceContextService) {
        this.fhirClientService = fhirClientService;
        this.practiceContextService = practiceContextService;
    }

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // ✅ Create immunization
    public ImmunizationDto create(ImmunizationDto dto) {
        if (dto == null || dto.getImmunizations() == null || dto.getImmunizations().isEmpty()) {
            throw new IllegalArgumentException("No immunization data provided");
        }

        log.info("Creating immunization in FHIR for patient: {}", dto.getPatientId());

        List<ImmunizationDto.ImmunizationItem> createdItems = new ArrayList<>();

        for (ImmunizationDto.ImmunizationItem item : dto.getImmunizations()) {
            validateMandatoryFields(item);

            Immunization fhirImmunization = toFhirImmunization(item, dto.getPatientId());
            MethodOutcome outcome = fhirClientService.create(fhirImmunization, getPracticeId());
            String fhirId = outcome.getId().getIdPart();

            item.setFhirId(fhirId);
            item.setExternalId(fhirId);
            item.setPatientId(dto.getPatientId());
            createdItems.add(item);

            log.info("Created FHIR Immunization with ID: {}", fhirId);
        }

        ImmunizationDto result = new ImmunizationDto();
        result.setPatientId(dto.getPatientId());
        result.setImmunizations(createdItems);
        return result;
    }

    // ✅ Get all immunizations for a patient
    public ImmunizationDto getByPatientId(Long patientId) {
        log.debug("Getting FHIR Immunizations for patient: {}", patientId);

        Bundle bundle = fhirClientService.getClient().search()
                .forResource(Immunization.class)
                .where(new ReferenceClientParam("patient").hasId("Patient/" + patientId))
                .withAdditionalHeader("X-Request-Tenant-Id", getPracticeId())
                .returnBundle(Bundle.class)
                .execute();

        List<ImmunizationDto.ImmunizationItem> items = extractImmunizationItems(bundle);

        ImmunizationDto dto = new ImmunizationDto();
        dto.setPatientId(patientId);
        dto.setImmunizations(items);
        return dto;
    }

    // ✅ Update immunization by patient
    public ImmunizationDto updateByPatientId(Long patientId, ImmunizationDto dto) {
        if (dto.getImmunizations() == null || dto.getImmunizations().isEmpty()) {
            throw new IllegalArgumentException("No immunization data provided");
        }

        ImmunizationDto.ImmunizationItem patch = dto.getImmunizations().get(0);
        String fhirId = patch.getFhirId() != null ? patch.getFhirId() : String.valueOf(patch.getId());

        log.info("Updating FHIR Immunization with ID: {}", fhirId);

        validateMandatoryFields(patch);

        Immunization fhirImmunization = toFhirImmunization(patch, patientId);
        fhirImmunization.setId(fhirId);

        fhirClientService.update(fhirImmunization, getPracticeId());

        patch.setFhirId(fhirId);
        patch.setExternalId(fhirId);

        ImmunizationDto result = new ImmunizationDto();
        result.setPatientId(patientId);
        result.setImmunizations(List.of(patch));
        return result;
    }

    // ✅ Delete all immunizations for a patient
    public void deleteByPatientId(Long patientId) {
        log.info("Deleting all FHIR Immunizations for patient: {}", patientId);

        Bundle bundle = fhirClientService.getClient().search()
                .forResource(Immunization.class)
                .where(new ReferenceClientParam("patient").hasId("Patient/" + patientId))
                .withAdditionalHeader("X-Request-Tenant-Id", getPracticeId())
                .returnBundle(Bundle.class)
                .execute();

        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Immunization) {
                    String fhirId = entry.getResource().getIdElement().getIdPart();
                    fhirClientService.delete(Immunization.class, fhirId, getPracticeId());
                    log.debug("Deleted FHIR Immunization: {}", fhirId);
                }
            }
        }
    }

    // ✅ Get single immunization item
    public ImmunizationDto.ImmunizationItem getItem(Long patientId, Long immunizationId) {
        String fhirId = String.valueOf(immunizationId);
        try {
            Immunization fhirImmunization = fhirClientService.read(Immunization.class, fhirId, getPracticeId());
            return toImmunizationItem(fhirImmunization);
        } catch (ResourceNotFoundException e) {
            throw new RuntimeException("Immunization not found with id: " + immunizationId + " for patientId: " + patientId);
        }
    }

    // ✅ Update single immunization item
    public ImmunizationDto.ImmunizationItem updateItem(Long patientId, Long immunizationId, ImmunizationDto.ImmunizationItem patch) {
        String fhirId = String.valueOf(immunizationId);
        log.info("Updating FHIR Immunization with ID: {}", fhirId);

        validateMandatoryFields(patch);

        Immunization fhirImmunization = toFhirImmunization(patch, patientId);
        fhirImmunization.setId(fhirId);

        fhirClientService.update(fhirImmunization, getPracticeId());

        patch.setFhirId(fhirId);
        patch.setExternalId(fhirId);
        return patch;
    }

    // ✅ Delete single immunization item
    public void deleteItem(Long patientId, Long immunizationId) {
        String fhirId = String.valueOf(immunizationId);
        log.info("Deleting FHIR Immunization with ID: {}", fhirId);
        fhirClientService.delete(Immunization.class, fhirId, getPracticeId());
    }

    // ✅ Search all immunizations
    public ApiResponse<List<ImmunizationDto>> searchAll() {
        log.debug("Searching all FHIR Immunizations");

        Bundle bundle = fhirClientService.search(Immunization.class, getPracticeId());
        List<ImmunizationDto.ImmunizationItem> allItems = extractImmunizationItems(bundle);

        // Group by patient
        Map<Long, List<ImmunizationDto.ImmunizationItem>> byPatient = allItems.stream()
                .filter(item -> item.getPatientId() != null)
                .collect(Collectors.groupingBy(ImmunizationDto.ImmunizationItem::getPatientId));

        List<ImmunizationDto> result = new ArrayList<>();
        for (var entry : byPatient.entrySet()) {
            ImmunizationDto dto = new ImmunizationDto();
            dto.setPatientId(entry.getKey());
            dto.setImmunizations(entry.getValue());
            result.add(dto);
        }

        return ApiResponse.<List<ImmunizationDto>>builder()
                .success(true)
                .message("Immunizations retrieved successfully")
                .data(result)
                .build();
    }

    // ========== FHIR Mapping Methods ==========

    private Immunization toFhirImmunization(ImmunizationDto.ImmunizationItem item, Long patientId) {
        Immunization immunization = new Immunization();

        // Patient reference
        immunization.setPatient(new Reference("Patient/" + patientId));

        // Status
        immunization.setStatus(Immunization.ImmunizationStatus.COMPLETED);

        // Vaccine code (CVX)
        if (item.getCvxCode() != null) {
            immunization.setVaccineCode(new CodeableConcept()
                    .addCoding(new Coding()
                            .setSystem("http://hl7.org/fhir/sid/cvx")
                            .setCode(item.getCvxCode())));
        }

        // Occurrence date
        if (item.getDateTimeAdministered() != null) {
            immunization.setOccurrence(new DateTimeType(item.getDateTimeAdministered()));
        }

        // Manufacturer
        if (item.getManufacturer() != null) {
            immunization.setManufacturer(new Reference().setDisplay(item.getManufacturer()));
        }

        // Lot number
        if (item.getLotNumber() != null) {
            immunization.setLotNumber(item.getLotNumber());
        }

        // Expiration date
        if (item.getExpirationDate() != null) {
            immunization.setExpirationDate(java.sql.Date.valueOf(item.getExpirationDate()));
        }

        // Route
        if (item.getRoute() != null) {
            immunization.setRoute(new CodeableConcept().setText(item.getRoute()));
        }

        // Site
        if (item.getAdministrationSite() != null) {
            immunization.setSite(new CodeableConcept().setText(item.getAdministrationSite()));
        }

        // Dose quantity
        if (item.getAmountAdministered() != null) {
            immunization.setDoseQuantity(new Quantity().setValue(Double.parseDouble(item.getAmountAdministered())));
        }

        // Performer (administrator)
        if (item.getAdministratorName() != null) {
            Immunization.ImmunizationPerformerComponent performer = immunization.addPerformer();
            performer.setActor(new Reference().setDisplay(item.getAdministratorName()));
            if (item.getAdministratorTitle() != null) {
                performer.setFunction(new CodeableConcept().setText(item.getAdministratorTitle()));
            }
        }

        // Notes
        if (item.getNotes() != null) {
            immunization.addNote().setText(item.getNotes());
        }

        // Reason code
        if (item.getReasonCode() != null) {
            immunization.addReasonCode(new CodeableConcept().setText(item.getReasonCode()));
        }

        return immunization;
    }

    private ImmunizationDto.ImmunizationItem toImmunizationItem(Immunization fhirImmunization) {
        ImmunizationDto.ImmunizationItem item = new ImmunizationDto.ImmunizationItem();

        // FHIR ID
        if (fhirImmunization.hasId()) {
            item.setFhirId(fhirImmunization.getIdElement().getIdPart());
            item.setExternalId(fhirImmunization.getIdElement().getIdPart());
        }

        // Patient ID
        if (fhirImmunization.hasPatient() && fhirImmunization.getPatient().hasReference()) {
            String ref = fhirImmunization.getPatient().getReference();
            if (ref.startsWith("Patient/")) {
                try {
                    item.setPatientId(Long.parseLong(ref.substring(8)));
                } catch (NumberFormatException e) {
                    // Non-numeric FHIR ID
                }
            }
        }

        // CVX code
        if (fhirImmunization.hasVaccineCode() && fhirImmunization.getVaccineCode().hasCoding()) {
            item.setCvxCode(fhirImmunization.getVaccineCode().getCodingFirstRep().getCode());
        }

        // Occurrence date
        if (fhirImmunization.hasOccurrenceDateTimeType()) {
            item.setDateTimeAdministered(fhirImmunization.getOccurrenceDateTimeType().getValueAsString());
        }

        // Manufacturer
        if (fhirImmunization.hasManufacturer()) {
            item.setManufacturer(fhirImmunization.getManufacturer().getDisplay());
        }

        // Lot number
        if (fhirImmunization.hasLotNumber()) {
            item.setLotNumber(fhirImmunization.getLotNumber());
        }

        // Expiration date
        if (fhirImmunization.hasExpirationDate()) {
            item.setExpirationDate(fhirImmunization.getExpirationDate().toString());
        }

        // Route
        if (fhirImmunization.hasRoute()) {
            item.setRoute(fhirImmunization.getRoute().getText());
        }

        // Site
        if (fhirImmunization.hasSite()) {
            item.setAdministrationSite(fhirImmunization.getSite().getText());
        }

        // Dose quantity
        if (fhirImmunization.hasDoseQuantity()) {
            item.setAmountAdministered(String.valueOf(fhirImmunization.getDoseQuantity().getValue()));
        }

        // Performer
        if (fhirImmunization.hasPerformer()) {
            Immunization.ImmunizationPerformerComponent performer = fhirImmunization.getPerformerFirstRep();
            if (performer.hasActor()) {
                item.setAdministratorName(performer.getActor().getDisplay());
            }
            if (performer.hasFunction()) {
                item.setAdministratorTitle(performer.getFunction().getText());
            }
        }

        // Notes
        if (fhirImmunization.hasNote()) {
            item.setNotes(fhirImmunization.getNoteFirstRep().getText());
        }

        // Reason code
        if (fhirImmunization.hasReasonCode()) {
            item.setReasonCode(fhirImmunization.getReasonCodeFirstRep().getText());
        }

        return item;
    }

    private List<ImmunizationDto.ImmunizationItem> extractImmunizationItems(Bundle bundle) {
        List<ImmunizationDto.ImmunizationItem> items = new ArrayList<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Immunization) {
                    items.add(toImmunizationItem((Immunization) entry.getResource()));
                }
            }
        }
        return items;
    }

    // ========== Validation Helpers ==========

    private void validateMandatoryFields(ImmunizationDto.ImmunizationItem item) {
        if (item == null) throw new IllegalArgumentException("immunization item is required");
        if (isBlank(item.getCvxCode())) throw new IllegalArgumentException("cvxCode is required");
        if (isBlank(item.getAmountAdministered())) throw new IllegalArgumentException("amountAdministered is required");
        if (isBlank(item.getManufacturer())) throw new IllegalArgumentException("manufacturer is required");
        if (isBlank(item.getAdministratorName())) throw new IllegalArgumentException("administratorName is required");
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
