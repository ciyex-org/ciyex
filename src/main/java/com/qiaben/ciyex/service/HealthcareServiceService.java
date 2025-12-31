package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.HealthcareServiceDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-only HealthcareService Service.
 * Uses FHIR HealthcareService resource directly via FhirClientService.
 * No local database storage - all data stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HealthcareServiceService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public HealthcareServiceDto create(HealthcareServiceDto dto) {
        log.debug("Creating FHIR HealthcareService: {}", dto.getName());

        HealthcareService hs = toFhirHealthcareService(dto);
        var outcome = fhirClientService.create(hs, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        log.info("Created FHIR HealthcareService with id: {}", fhirId);

        return dto;
    }

    // GET ALL
    public List<HealthcareServiceDto> getAll() {
        log.debug("Getting all FHIR HealthcareServices");

        Bundle bundle = fhirClientService.search(HealthcareService.class, getPracticeId());
        List<HealthcareService> services = fhirClientService.extractResources(bundle, HealthcareService.class);

        return services.stream().map(this::fromFhirHealthcareService).collect(Collectors.toList());
    }

    // GET BY ID
    public HealthcareServiceDto getById(String fhirId) {
        log.debug("Getting FHIR HealthcareService: {}", fhirId);
        HealthcareService hs = fhirClientService.read(HealthcareService.class, fhirId, getPracticeId());
        return fromFhirHealthcareService(hs);
    }

    // UPDATE
    public HealthcareServiceDto update(String fhirId, HealthcareServiceDto dto) {
        log.debug("Updating FHIR HealthcareService: {}", fhirId);

        HealthcareService hs = toFhirHealthcareService(dto);
        hs.setId(fhirId);
        fhirClientService.update(hs, getPracticeId());

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        return dto;
    }

    // DELETE
    public void delete(String fhirId) {
        log.debug("Deleting FHIR HealthcareService: {}", fhirId);
        fhirClientService.delete(HealthcareService.class, fhirId, getPracticeId());
    }

    // -------- FHIR Mapping --------

    private HealthcareService toFhirHealthcareService(HealthcareServiceDto dto) {
        HealthcareService hs = new HealthcareService();
        hs.setActive(true);

        // Name
        if (dto.getName() != null) {
            hs.setName(dto.getName());
        }

        // Comment (description)
        if (dto.getDescription() != null) {
            hs.setComment(dto.getDescription());
        }

        // Location reference
        if (dto.getLocation() != null) {
            hs.addLocation(new Reference("Location/" + dto.getLocation()));
        }

        // Type (category)
        if (dto.getType() != null) {
            hs.addType().setText(dto.getType());
        }

        // Hours of operation (stored as extension since AvailableTime doesn't have description)
        if (dto.getHoursOfOperation() != null) {
            hs.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/hours-of-operation", 
                    new StringType(dto.getHoursOfOperation())));
        }

        return hs;
    }

    private HealthcareServiceDto fromFhirHealthcareService(HealthcareService hs) {
        HealthcareServiceDto dto = new HealthcareServiceDto();
        dto.setFhirId(hs.getIdElement().getIdPart());
        dto.setExternalId(hs.getIdElement().getIdPart());

        // Name
        if (hs.hasName()) {
            dto.setName(hs.getName());
        }

        // Description (comment)
        if (hs.hasComment()) {
            dto.setDescription(hs.getComment());
        }

        // Location
        if (hs.hasLocation()) {
            String ref = hs.getLocationFirstRep().getReference();
            if (ref != null && ref.startsWith("Location/")) {
                dto.setLocation(ref.substring("Location/".length()));
            }
        }

        // Type
        if (hs.hasType()) {
            dto.setType(hs.getTypeFirstRep().getText());
        }

        // Hours of operation (from extension)
        Extension hoursExt = hs.getExtensionByUrl("http://ciyex.com/fhir/StructureDefinition/hours-of-operation");
        if (hoursExt != null && hoursExt.getValue() instanceof StringType) {
            dto.setHoursOfOperation(((StringType) hoursExt.getValue()).getValue());
        }

        return dto;
    }
}
