package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.FacilityDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Facility Service - FHIR Only.
 * All facility data is stored in HAPI FHIR server as Location resources.
 */
@Service
@Slf4j
public class FacilityService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final String IDENTIFIER_SYSTEM_FACILITY = "urn:ciyex:facility:id";
    private static final String IDENTIFIER_SYSTEM_NPI = "http://hl7.org/fhir/sid/us-npi";

    @Autowired
    public FacilityService(FhirClientService fhirClientService, PracticeContextService practiceContextService) {
        this.fhirClientService = fhirClientService;
        this.practiceContextService = practiceContextService;
    }

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // ✅ Create facility in FHIR
    public FacilityDto create(FacilityDto dto) {
        log.info("Creating facility in FHIR: {}", dto.getName());
        
        Location fhirLocation = toFhirLocation(dto);
        fhirLocation.setManagingOrganization(new Reference("Organization/" + getPracticeId()));
        
        // Add audit metadata
        addAuditMetadata(fhirLocation, true);
        
        MethodOutcome outcome = fhirClientService.create(fhirLocation, getPracticeId());
        
        String fhirId = outcome.getId().getIdPart();
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        
        // Populate audit info in DTO
        dto.setAudit(extractAuditFromLocation(fhirLocation));
        
        log.info("Created FHIR Location with ID: {}", fhirId);
        return dto;
    }

    // ✅ Get facility by FHIR ID
    public FacilityDto getById(Long id) {
        return getByFhirId(String.valueOf(id));
    }

    public FacilityDto getByFhirId(String fhirId) {
        log.debug("Reading FHIR Location with ID: {}", fhirId);
        try {
            Location fhirLocation = fhirClientService.read(Location.class, fhirId, getPracticeId());
            return toFacilityDto(fhirLocation);
        } catch (ResourceNotFoundException e) {
            throw new RuntimeException("Facility not found with FHIR ID: " + fhirId);
        }
    }

    // ✅ Update facility in FHIR
    public FacilityDto update(Long id, FacilityDto dto) {
        return updateByFhirId(String.valueOf(id), dto);
    }

    public FacilityDto updateByFhirId(String fhirId, FacilityDto dto) {
        log.info("Updating FHIR Location with ID: {}", fhirId);
        
        Location fhirLocation = toFhirLocation(dto);
        fhirLocation.setId(fhirId);
        
        // Add audit metadata for update
        addAuditMetadata(fhirLocation, false);
        
        fhirClientService.update(fhirLocation, getPracticeId());
        
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setAudit(extractAuditFromLocation(fhirLocation));
        
        log.info("Updated FHIR Location with ID: {}", fhirId);
        return dto;
    }

    // ✅ Delete facility from FHIR
    public void delete(Long id) {
        deleteByFhirId(String.valueOf(id));
    }

    public void deleteByFhirId(String fhirId) {
        log.info("Deleting FHIR Location with ID: {}", fhirId);
        fhirClientService.delete(Location.class, fhirId, getPracticeId());
        log.info("Deleted FHIR Location with ID: {}", fhirId);
    }

    // ✅ Soft delete (set status to inactive)
    public void softDelete(Long id) {
        softDeleteByFhirId(String.valueOf(id));
    }

    public void softDeleteByFhirId(String fhirId) {
        log.info("Soft deleting FHIR Location with ID: {}", fhirId);
        try {
            Location fhirLocation = fhirClientService.read(Location.class, fhirId, getPracticeId());
            fhirLocation.setStatus(Location.LocationStatus.INACTIVE);
            fhirClientService.update(fhirLocation, getPracticeId());
            log.info("Soft deleted FHIR Location with ID: {}", fhirId);
        } catch (ResourceNotFoundException e) {
            throw new RuntimeException("Facility not found with FHIR ID: " + fhirId);
        }
    }

    // ✅ Get all facilities from FHIR
    public ApiResponse<List<FacilityDto>> getAllFacilities() {
        log.debug("Getting all FHIR Locations for practice {}", getPracticeId());
        
        Bundle bundle = fhirClientService.search(Location.class, getPracticeId());
        List<FacilityDto> dtos = extractLocations(bundle);

        return ApiResponse.<List<FacilityDto>>builder()
                .success(true)
                .message("Facilities retrieved successfully from FHIR")
                .data(dtos)
                .build();
    }

    // ✅ Get active facilities
    public ApiResponse<List<FacilityDto>> getActiveFacilities() {
        log.debug("Getting active FHIR Locations");
        
        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Location.class)
                .where(new StringClientParam("status").matches().value("active"))
                
                .returnBundle(Bundle.class)
                .execute();
        
        List<FacilityDto> dtos = extractLocations(bundle);

        return ApiResponse.<List<FacilityDto>>builder()
                .success(true)
                .message("Active facilities retrieved successfully")
                .data(dtos)
                .build();
    }

    // ✅ Get facilities by status
    public ApiResponse<List<FacilityDto>> getFacilitiesByStatus(Boolean isActive) {
        String status = Boolean.TRUE.equals(isActive) ? "active" : "inactive";
        log.debug("Getting FHIR Locations by status: {}", status);
        
        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Location.class)
                .where(new StringClientParam("status").matches().value(status))
                
                .returnBundle(Bundle.class)
                .execute();
        
        List<FacilityDto> dtos = extractLocations(bundle);

        return ApiResponse.<List<FacilityDto>>builder()
                .success(true)
                .message("Facilities retrieved successfully")
                .data(dtos)
                .build();
    }

    // ✅ Search by name
    public ApiResponse<List<FacilityDto>> searchByName(String name) {
        log.debug("Searching FHIR Locations by name: {}", name);
        
        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Location.class)
                .where(new StringClientParam("name").matches().value(name))
                
                .returnBundle(Bundle.class)
                .execute();
        
        List<FacilityDto> dtos = extractLocations(bundle);

        return ApiResponse.<List<FacilityDto>>builder()
                .success(true)
                .message("Facilities search completed")
                .data(dtos)
                .build();
    }

    // ✅ Get paginated
    public Page<FacilityDto> getAllPaginated(Pageable pageable) {
        log.debug("Getting paginated FHIR Locations");
        
        Bundle bundle = fhirClientService.search(Location.class, getPracticeId());
        List<FacilityDto> allFacilities = extractLocations(bundle);
        
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allFacilities.size());
        
        List<FacilityDto> pageContent = start < allFacilities.size() 
                ? allFacilities.subList(start, end) 
                : new ArrayList<>();
        
        return new PageImpl<>(pageContent, pageable, allFacilities.size());
    }

    public long getTotalCount() {
        Bundle bundle = fhirClientService.search(Location.class, getPracticeId());
        return bundle.getTotal();
    }

    public long getActiveCount() {
        return getActiveFacilities().getData().size();
    }

    public long getInactiveCount() {
        return getFacilitiesByStatus(false).getData().size();
    }

    // ========== FHIR Mapping Methods ==========

    private Location toFhirLocation(FacilityDto dto) {
        Location location = new Location();

        // Identifier
        if (dto.getNpi() != null) {
            location.addIdentifier()
                    .setSystem(IDENTIFIER_SYSTEM_NPI)
                    .setValue(dto.getNpi());
        }

        // Name
        location.setName(dto.getName());

        // Status
        location.setStatus(Boolean.FALSE.equals(dto.getIsActive()) || Boolean.TRUE.equals(dto.getFacilityInactive())
                ? Location.LocationStatus.INACTIVE 
                : Location.LocationStatus.ACTIVE);

        // Mode
        location.setMode(Location.LocationMode.INSTANCE);

        // Type
        location.addType()
                .addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/v3-RoleCode")
                .setCode("HOSP")
                .setDisplay("Hospital");

        // Contact Info
        if (dto.getPhone() != null) {
            location.addTelecom()
                    .setSystem(ContactPoint.ContactPointSystem.PHONE)
                    .setValue(dto.getPhone())
                    .setUse(ContactPoint.ContactPointUse.WORK);
        }
        if (dto.getFax() != null) {
            location.addTelecom()
                    .setSystem(ContactPoint.ContactPointSystem.FAX)
                    .setValue(dto.getFax())
                    .setUse(ContactPoint.ContactPointUse.WORK);
        }
        if (dto.getEmail() != null) {
            location.addTelecom()
                    .setSystem(ContactPoint.ContactPointSystem.EMAIL)
                    .setValue(dto.getEmail())
                    .setUse(ContactPoint.ContactPointUse.WORK);
        }

        // Physical Address
        if (dto.getPhysicalAddress() != null || dto.getPhysicalCity() != null) {
            Address address = location.getAddress().setUse(Address.AddressUse.WORK);
            if (dto.getPhysicalAddress() != null) address.addLine(dto.getPhysicalAddress());
            if (dto.getPhysicalCity() != null) address.setCity(dto.getPhysicalCity());
            if (dto.getPhysicalState() != null) address.setState(dto.getPhysicalState());
            if (dto.getPhysicalZipCode() != null) address.setPostalCode(dto.getPhysicalZipCode());
            if (dto.getPhysicalCountry() != null) address.setCountry(dto.getPhysicalCountry());
        }

        return location;
    }

    private FacilityDto toFacilityDto(Location location) {
        FacilityDto dto = FacilityDto.builder().build();

        // FHIR ID (auto-generated)
        if (location.hasId()) {
            dto.setFhirId(location.getIdElement().getIdPart());
            dto.setExternalId(location.getIdElement().getIdPart());
        }

        // Identifiers
        for (Identifier identifier : location.getIdentifier()) {
            if (IDENTIFIER_SYSTEM_NPI.equals(identifier.getSystem())) {
                dto.setNpi(identifier.getValue());
            }
        }

        // Name
        dto.setName(location.getName());

        // Status
        dto.setIsActive(location.getStatus() == Location.LocationStatus.ACTIVE);
        dto.setFacilityInactive(location.getStatus() == Location.LocationStatus.INACTIVE);

        // Contact Info
        for (ContactPoint telecom : location.getTelecom()) {
            switch (telecom.getSystem()) {
                case PHONE -> dto.setPhone(telecom.getValue());
                case FAX -> dto.setFax(telecom.getValue());
                case EMAIL -> dto.setEmail(telecom.getValue());
                default -> {}
            }
        }

        // Address
        if (location.hasAddress()) {
            Address address = location.getAddress();
            if (address.hasLine()) dto.setPhysicalAddress(address.getLine().get(0).getValue());
            dto.setPhysicalCity(address.getCity());
            dto.setPhysicalState(address.getState());
            dto.setPhysicalZipCode(address.getPostalCode());
            dto.setPhysicalCountry(address.getCountry());
        }

        // Extract audit information
        dto.setAudit(extractAuditFromLocation(location));

        return dto;
    }

    private List<FacilityDto> extractLocations(Bundle bundle) {
        List<FacilityDto> facilities = new ArrayList<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Location) {
                    facilities.add(toFacilityDto((Location) entry.getResource()));
                }
            }
        }
        return facilities;
    }

    // ========== Audit Helper Methods ==========

    private void addAuditMetadata(Location location, boolean isCreate) {
        String currentUser = getCurrentUser();
        Date now = new Date();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        String timestamp = LocalDateTime.now().format(formatter);

        if (isCreate) {
            // Add created metadata as extensions
            location.addExtension()
                    .setUrl("http://ciyex.com/fhir/StructureDefinition/created-date")
                    .setValue(new DateTimeType(now));
            location.addExtension()
                    .setUrl("http://ciyex.com/fhir/StructureDefinition/created-by")
                    .setValue(new StringType(currentUser));
        }

        // Always update last modified metadata
        location.addExtension()
                .setUrl("http://ciyex.com/fhir/StructureDefinition/last-modified-date")
                .setValue(new DateTimeType(now));
        location.addExtension()
                .setUrl("http://ciyex.com/fhir/StructureDefinition/last-modified-by")
                .setValue(new StringType(currentUser));
    }

    private FacilityDto.Audit extractAuditFromLocation(Location location) {
        FacilityDto.Audit audit = new FacilityDto.Audit();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        for (Extension ext : location.getExtension()) {
            switch (ext.getUrl()) {
                case "http://ciyex.com/fhir/StructureDefinition/created-date" -> {
                    if (ext.getValue() instanceof DateTimeType) {
                        audit.setCreatedDate(((DateTimeType) ext.getValue()).getValueAsString());
                    }
                }
                case "http://ciyex.com/fhir/StructureDefinition/created-by" -> {
                    if (ext.getValue() instanceof StringType) {
                        audit.setCreatedBy(((StringType) ext.getValue()).getValue());
                    }
                }
                case "http://ciyex.com/fhir/StructureDefinition/last-modified-date" -> {
                    if (ext.getValue() instanceof DateTimeType) {
                        audit.setLastModifiedDate(((DateTimeType) ext.getValue()).getValueAsString());
                    }
                }
                case "http://ciyex.com/fhir/StructureDefinition/last-modified-by" -> {
                    if (ext.getValue() instanceof StringType) {
                        audit.setLastModifiedBy(((StringType) ext.getValue()).getValue());
                    }
                }
            }
        }

        return audit;
    }

    private String getCurrentUser() {
        try {
            RequestContext context = RequestContext.get();
            if (context != null && context.getAuthToken() != null) {
                // Extract username from auth token if available
                return "user-from-token"; // TODO: Parse JWT token to get actual username
            }
        } catch (Exception e) {
            log.debug("Could not extract user from RequestContext: {}", e.getMessage());
        }
        return "system";
    }
}