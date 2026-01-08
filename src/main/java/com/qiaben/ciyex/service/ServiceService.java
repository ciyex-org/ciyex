package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ServicebillDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-only Service (billing service definitions).
 * Uses FHIR ActivityDefinition resource directly via FhirClientService.
 * No local database storage - all data stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final String EXT_DEFAULT_PRICE = "http://ciyex.com/fhir/StructureDefinition/default-price";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public ServicebillDto create(ServicebillDto dto) {
        log.debug("Creating FHIR ActivityDefinition (service): {}", dto.getName());

        ActivityDefinition ad = toFhirActivityDefinition(dto);
        var outcome = fhirClientService.create(ad, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setId(Long.parseLong(fhirId.hashCode() + ""));
        
        // Set ID from FHIR ID
        try {
            dto.setId(Long.valueOf(fhirId));
        } catch (NumberFormatException e) {
            dto.setId((long) fhirId.hashCode());
        }
        
        // Add audit information
        ServicebillDto.Audit audit = new ServicebillDto.Audit();
        String currentTime = java.time.Instant.now().toString();
        audit.setCreatedDate(currentTime);
        audit.setLastModifiedDate(currentTime);
        dto.setAudit(audit);
        
        log.info("Created FHIR ActivityDefinition (service) with id: {}", fhirId);

        return dto;
    }

    // GET BY ID
    public ServicebillDto getById(String fhirId) {
        log.debug("Getting FHIR ActivityDefinition (service): {}", fhirId);
        ActivityDefinition ad = fhirClientService.read(ActivityDefinition.class, fhirId, getPracticeId());
        return fromFhirActivityDefinition(ad);
    }

    // GET ALL
    public List<ServicebillDto> getAll() {
        log.debug("Getting all FHIR ActivityDefinitions (services)");

        Bundle bundle = fhirClientService.search(ActivityDefinition.class, getPracticeId());
        List<ActivityDefinition> defs = fhirClientService.extractResources(bundle, ActivityDefinition.class);

        return defs.stream()
                .filter(this::isServiceDefinition)
                .map(this::fromFhirActivityDefinition)
                .collect(Collectors.toList());
    }

    // UPDATE
    public ServicebillDto update(String fhirId, ServicebillDto dto) {
        log.debug("Updating FHIR ActivityDefinition (service): {}", fhirId);

        ActivityDefinition ad = toFhirActivityDefinition(dto);
        ad.setId(fhirId);
        var outcome = fhirClientService.update(ad, getPracticeId());
        
        // Read back the updated resource to get proper audit info
        ActivityDefinition updated = fhirClientService.read(ActivityDefinition.class, fhirId, getPracticeId());
        return fromFhirActivityDefinition(updated);
    }

    // DELETE
    public void delete(String fhirId) {
        log.debug("Deleting FHIR ActivityDefinition (service): {}", fhirId);
        fhirClientService.delete(ActivityDefinition.class, fhirId, getPracticeId());
    }

    // -------- FHIR Mapping --------

    private ActivityDefinition toFhirActivityDefinition(ServicebillDto dto) {
        ActivityDefinition ad = new ActivityDefinition();
        ad.setStatus(Enumerations.PublicationStatus.ACTIVE);

        // Name/Title
        if (dto.getName() != null) {
            ad.setName(dto.getName());
            ad.setTitle(dto.getName());
        }

        // Category = billing-service
        ad.addTopic().addCoding()
                .setSystem("http://ciyex.com/fhir/CodeSystem/activity-type")
                .setCode("billing-service")
                .setDisplay("Billing Service");

        // Default price as extension
        if (dto.getDefaultPrice() != null) {
            ad.addExtension(new Extension(EXT_DEFAULT_PRICE, new StringType(dto.getDefaultPrice())));
        }

        return ad;
    }

    private ServicebillDto fromFhirActivityDefinition(ActivityDefinition ad) {
        ServicebillDto dto = new ServicebillDto();

        // Use FHIR ID hash as Long ID for compatibility
        String fhirId = ad.getIdElement().getIdPart();
        dto.setId((long) Math.abs(fhirId.hashCode()));
        
        // Set ID from FHIR ID
        try {
            dto.setId(Long.valueOf(fhirId));
        } catch (NumberFormatException e) {
            dto.setId((long) fhirId.hashCode());
        }

        // Name
        if (ad.hasName()) {
            dto.setName(ad.getName());
        } else if (ad.hasTitle()) {
            dto.setName(ad.getTitle());
        }

        // Default price
        Extension priceExt = ad.getExtensionByUrl(EXT_DEFAULT_PRICE);
        if (priceExt != null && priceExt.getValue() instanceof StringType) {
            dto.setDefaultPrice(((StringType) priceExt.getValue()).getValue());
        }
        
        // Add audit information
        ServicebillDto.Audit audit = new ServicebillDto.Audit();
        if (ad.getMeta() != null && ad.getMeta().hasLastUpdated()) {
            String timestamp = ad.getMeta().getLastUpdated().toInstant().toString();
            audit.setLastModifiedDate(timestamp);
            audit.setCreatedDate(timestamp);
        } else {
            String currentTime = java.time.Instant.now().toString();
            audit.setCreatedDate(currentTime);
            audit.setLastModifiedDate(currentTime);
        }
        dto.setAudit(audit);

        return dto;
    }

    // -------- Helpers --------

    private boolean isServiceDefinition(ActivityDefinition ad) {
        if (!ad.hasTopic()) return false;
        return ad.getTopic().stream()
                .flatMap(cc -> cc.getCoding().stream())
                .anyMatch(c -> "billing-service".equals(c.getCode()));
    }
}
