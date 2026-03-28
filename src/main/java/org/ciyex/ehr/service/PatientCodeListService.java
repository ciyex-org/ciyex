package org.ciyex.ehr.service;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import org.ciyex.ehr.dto.PatientCodeListDto;
import org.ciyex.ehr.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-only PatientCodeList Service.
 * Uses FHIR List resource directly via FhirClientService.
 * No local database storage - all data stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatientCodeListService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    // Extension URLs
    private static final String EXT_ORDER = "http://ciyex.com/fhir/StructureDefinition/order-index";
    private static final String EXT_IS_DEFAULT = "http://ciyex.com/fhir/StructureDefinition/is-default";
    private static final String EXT_ACTIVE = "http://ciyex.com/fhir/StructureDefinition/active";
    private static final String EXT_CODES = "http://ciyex.com/fhir/StructureDefinition/codes";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // FIND ALL
    public List<PatientCodeListDto> findAll() {
        log.debug("Getting all FHIR Lists (patient code lists)");

        Bundle bundle = fhirClientService.search(ListResource.class, getPracticeId());
        List<ListResource> lists = fhirClientService.extractResources(bundle, ListResource.class);

        return lists.stream()
                .filter(this::isPatientCodeList)
                .map(this::fromFhirList)
                .collect(Collectors.toList());
    }

    // GET BY ID
    public PatientCodeListDto getById(String fhirId) {
        log.debug("Getting FHIR List (patient code list): {}", fhirId);
        try {
            ListResource list = fhirClientService.read(ListResource.class, fhirId, getPracticeId());
            PatientCodeListDto dto = fromFhirList(list);
            dto.id = Long.parseLong(fhirId);
            return dto;
        } catch (BaseServerResponseException e) {
            log.error("Failed to retrieve patient code list {}: {} {}", fhirId, e.getStatusCode(), e.getMessage());
            throw new org.ciyex.ehr.exception.ResourceNotFoundException(
                String.format("Patient code list not found with id: %s", fhirId)
            );
        }
    }

    // CREATE
    public PatientCodeListDto create(PatientCodeListDto dto) {
        log.debug("Creating FHIR List (patient code list): {}", dto.title);

        ListResource list = toFhirList(dto);
        var outcome = fhirClientService.create(list, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.id = Long.parseLong(fhirId);
        dto.fhirId = fhirId;
        dto.externalId = fhirId;
        
        ListResource created = (ListResource) outcome.getResource();
        if (created != null && created.hasMeta()) {
            populateAudit(dto, created.getMeta());
        }

        // If this is default, clear other defaults
        if (dto.isDefault) {
            clearOtherDefaults(fhirId);
        }

        log.info("Created FHIR List (patient code list) with id: {}", fhirId);
        return dto;
    }

    // UPDATE
    public PatientCodeListDto update(String fhirId, PatientCodeListDto dto) {
        log.debug("Updating FHIR List (patient code list): {}", fhirId);
        try {
            ListResource list = toFhirList(dto);
            list.setId(fhirId);
            fhirClientService.update(list, getPracticeId());

            dto.fhirId = fhirId;
            dto.externalId = fhirId;

            // If this is default, clear other defaults
            if (dto.isDefault) {
                clearOtherDefaults(fhirId);
            }

            return getById(fhirId);
        } catch (BaseServerResponseException e) {
            log.error("Failed to update patient code list {}: {} {}", fhirId, e.getStatusCode(), e.getMessage());
            throw new org.ciyex.ehr.exception.ResourceNotFoundException(
                String.format("Patient code list not found with id: %s", fhirId)
            );
        }
    }

    // DELETE
    public boolean delete(String fhirId) {
        log.debug("Deleting FHIR List (patient code list): {}", fhirId);
        try {
            fhirClientService.deleteByResourceName("List", fhirId, getPracticeId());
            return true;
        } catch (BaseServerResponseException e) {
            log.error("Failed to delete patient code list {}: {} {}", fhirId, e.getStatusCode(), e.getMessage());
            throw new org.ciyex.ehr.exception.ResourceNotFoundException(
                String.format("Patient code list not found with id: %s", fhirId)
            );
        }
    }

    // SAVE BULK
    public List<PatientCodeListDto> saveBulk(List<PatientCodeListDto> rows) {
        boolean seenDefault = false;
        for (PatientCodeListDto r : rows) {
            if (r.isDefault) {
                if (seenDefault) r.isDefault = false;
                else seenDefault = true;
            }
        }

        return rows.stream()
                .sorted(Comparator.comparingInt(r -> r.order != null ? r.order : 0))
                .map(r -> {
                    if (r.fhirId != null && !r.fhirId.isEmpty()) {
                        return update(r.fhirId, r);
                    } else {
                        return create(r);
                    }
                })
                .collect(Collectors.toList());
    }

    // SET DEFAULT
    public PatientCodeListDto setDefault(String fhirId) {
        try {
            PatientCodeListDto dto = getById(fhirId);
            if (dto == null) return null;
            
            dto.isDefault = true;
            update(fhirId, dto);
            clearOtherDefaults(fhirId);
            
            return dto;
        } catch (BaseServerResponseException e) {
            log.error("Failed to set default for patient code list {}: {} {}", fhirId, e.getStatusCode(), e.getMessage());
            throw new org.ciyex.ehr.exception.ResourceNotFoundException(
                String.format("Patient code list not found with id: %s", fhirId)
            );
        }
    }

    // -------- FHIR Mapping --------

    private ListResource toFhirList(PatientCodeListDto dto) {
        ListResource list = new ListResource();
        list.setStatus(ListResource.ListStatus.CURRENT);
        list.setMode(ListResource.ListMode.WORKING);

        // Code to identify as patient code list
        list.setCode(new CodeableConcept().addCoding(
                new Coding()
                        .setSystem("http://ciyex.com/fhir/CodeSystem/list-type")
                        .setCode("patient-code-list")
                        .setDisplay("Patient Code List")
        ));

        // Title
        if (dto.title != null) {
            list.setTitle(dto.title);
        }

        // Notes
        if (dto.notes != null) {
            list.addNote().setText(dto.notes);
        }

        // Extensions
        if (dto.order != null) {
            list.addExtension(new Extension(EXT_ORDER, new IntegerType(dto.order)));
        }
        list.addExtension(new Extension(EXT_IS_DEFAULT, new BooleanType(dto.isDefault)));
        list.addExtension(new Extension(EXT_ACTIVE, new BooleanType(dto.active)));
        
        if (dto.codes != null) {
            list.addExtension(new Extension(EXT_CODES, new StringType(dto.codes)));
        }

        return list;
    }

    private PatientCodeListDto fromFhirList(ListResource list) {
        PatientCodeListDto dto = new PatientCodeListDto();
        String fhirId = list.getIdElement().getIdPart();
        dto.id = Long.parseLong(fhirId);
        dto.fhirId = fhirId;
        dto.externalId = fhirId;

        // Title
        if (list.hasTitle()) {
            dto.title = list.getTitle();
        }

        // Notes
        if (list.hasNote()) {
            dto.notes = list.getNoteFirstRep().getText();
        }

        // Extensions
        Extension orderExt = list.getExtensionByUrl(EXT_ORDER);
        if (orderExt != null && orderExt.getValue() instanceof IntegerType) {
            dto.order = ((IntegerType) orderExt.getValue()).getValue();
        }

        Extension defaultExt = list.getExtensionByUrl(EXT_IS_DEFAULT);
        if (defaultExt != null && defaultExt.getValue() instanceof BooleanType) {
            dto.isDefault = ((BooleanType) defaultExt.getValue()).booleanValue();
        }

        Extension activeExt = list.getExtensionByUrl(EXT_ACTIVE);
        if (activeExt != null && activeExt.getValue() instanceof BooleanType) {
            dto.active = ((BooleanType) activeExt.getValue()).booleanValue();
        }

        Extension codesExt = list.getExtensionByUrl(EXT_CODES);
        if (codesExt != null && codesExt.getValue() instanceof StringType) {
            dto.codes = ((StringType) codesExt.getValue()).getValue();
        }
        
        if (list.hasMeta()) {
            populateAudit(dto, list.getMeta());
        }

        return dto;
    }

    // -------- Helpers --------

    private boolean isPatientCodeList(ListResource list) {
        if (!list.hasCode()) return false;
        return list.getCode().getCoding().stream()
                .anyMatch(c -> "patient-code-list".equals(c.getCode()));
    }

    private void clearOtherDefaults(String exceptFhirId) {
        List<PatientCodeListDto> all = findAll();
        for (PatientCodeListDto item : all) {
            if (!item.fhirId.equals(exceptFhirId) && item.isDefault) {
                item.isDefault = false;
                update(item.fhirId, item);
            }
        }
    }
    
    private void populateAudit(PatientCodeListDto dto, Meta meta) {
        PatientCodeListDto.Audit audit = new PatientCodeListDto.Audit();
        if (meta.hasLastUpdated()) {
            audit.lastModifiedDate = meta.getLastUpdated().toInstant().atOffset(ZoneOffset.UTC).toLocalDate().toString();
            audit.createdDate = meta.getLastUpdated().toInstant().atOffset(ZoneOffset.UTC).toLocalDate().toString();
        }
        dto.audit = audit;
    }
}
