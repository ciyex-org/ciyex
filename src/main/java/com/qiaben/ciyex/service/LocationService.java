package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.qiaben.ciyex.dto.LocationDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Location Service - FHIR Only.
 * All location (room) data is stored in HAPI FHIR server as Location resources.
 */
@Service
@Slf4j
public class LocationService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    @Autowired
    public LocationService(FhirClientService fhirClientService, PracticeContextService practiceContextService) {
        this.fhirClientService = fhirClientService;
        this.practiceContextService = practiceContextService;
    }

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // ✅ Create location in FHIR
    public LocationDto create(LocationDto dto) {
        log.info("Creating location in FHIR: {}", dto.getName());
        
        Location fhirLocation = toFhirLocation(dto);
        fhirLocation.setManagingOrganization(new Reference("Organization/" + getPracticeId()));
        
        MethodOutcome outcome = fhirClientService.create(fhirLocation, getPracticeId());
        
        String fhirId = outcome.getId().getIdPart();
        dto.setExternalId(fhirId);
        
        log.info("Created FHIR Location with ID: {}", fhirId);
        return dto;
    }

    // ✅ Get location by FHIR ID
    public LocationDto getById(Long id) {
        return getByFhirId(String.valueOf(id));
    }

    public LocationDto getByFhirId(String fhirId) {
        log.debug("Reading FHIR Location with ID: {}", fhirId);
        try {
            Location fhirLocation = fhirClientService.read(Location.class, fhirId, getPracticeId());
            return toLocationDto(fhirLocation);
        } catch (ResourceNotFoundException e) {
            throw new RuntimeException("Location not found with FHIR ID: " + fhirId);
        }
    }

    // ✅ Update location in FHIR
    public LocationDto update(Long id, LocationDto dto) {
        return updateByFhirId(String.valueOf(id), dto);
    }

    public LocationDto updateByFhirId(String fhirId, LocationDto dto) {
        log.info("Updating FHIR Location with ID: {}", fhirId);
        
        Location fhirLocation = toFhirLocation(dto);
        fhirLocation.setId(fhirId);
        
        fhirClientService.update(fhirLocation, getPracticeId());
        
        dto.setExternalId(fhirId);
        
        log.info("Updated FHIR Location with ID: {}", fhirId);
        return dto;
    }

    // ✅ Delete location from FHIR
    public void delete(Long id) {
        deleteByFhirId(String.valueOf(id));
    }

    public void deleteByFhirId(String fhirId) {
        log.info("Deleting FHIR Location with ID: {}", fhirId);
        fhirClientService.delete(Location.class, fhirId, getPracticeId());
        log.info("Deleted FHIR Location with ID: {}", fhirId);
    }

    // ✅ Get all locations with pagination
    public Page<LocationDto> getAll(Pageable pageable) {
        log.debug("Getting all FHIR Locations for practice {}", getPracticeId());
        
        Bundle bundle = fhirClientService.search(Location.class, getPracticeId());
        List<LocationDto> allLocations = extractLocations(bundle);
        
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allLocations.size());
        
        List<LocationDto> pageContent = start < allLocations.size() 
                ? allLocations.subList(start, end) 
                : new ArrayList<>();
        
        return new PageImpl<>(pageContent, pageable, allLocations.size());
    }

    // ✅ Search locations by keyword
    public Page<LocationDto> search(String keyword, Pageable pageable) {
        log.debug("Searching FHIR Locations by keyword: {}", keyword);
        
        Bundle bundle = fhirClientService.getClient().search()
                .forResource(Location.class)
                .where(new StringClientParam("name").matches().value(keyword))
                .withAdditionalHeader("X-Request-Tenant-Id", getPracticeId())
                .returnBundle(Bundle.class)
                .execute();
        
        List<LocationDto> allLocations = extractLocations(bundle);
        
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allLocations.size());
        
        List<LocationDto> pageContent = start < allLocations.size() 
                ? allLocations.subList(start, end) 
                : new ArrayList<>();
        
        return new PageImpl<>(pageContent, pageable, allLocations.size());
    }

    // ========== FHIR Mapping Methods ==========

    private Location toFhirLocation(LocationDto dto) {
        Location location = new Location();

        // Name
        location.setName(dto.getName());

        // Status
        location.setStatus(Location.LocationStatus.ACTIVE);

        // Mode - this is a room/physical location
        location.setMode(Location.LocationMode.INSTANCE);

        // Type - room
        location.addType()
                .addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/v3-RoleCode")
                .setCode("RO")
                .setDisplay("Room");

        // Address
        if (dto.getAddress() != null || dto.getCity() != null) {
            Address address = location.getAddress().setUse(Address.AddressUse.WORK);
            if (dto.getAddress() != null) address.addLine(dto.getAddress());
            if (dto.getCity() != null) address.setCity(dto.getCity());
            if (dto.getState() != null) address.setState(dto.getState());
            if (dto.getPostalCode() != null) address.setPostalCode(dto.getPostalCode());
            if (dto.getCountry() != null) address.setCountry(dto.getCountry());
        }

        return location;
    }

    private LocationDto toLocationDto(Location location) {
        LocationDto dto = new LocationDto();

        // FHIR ID
        if (location.hasId()) {
            dto.setExternalId(location.getIdElement().getIdPart());
        }

        // Name
        dto.setName(location.getName());

        // Address
        if (location.hasAddress()) {
            Address address = location.getAddress();
            if (address.hasLine()) dto.setAddress(address.getLine().get(0).getValue());
            dto.setCity(address.getCity());
            dto.setState(address.getState());
            dto.setPostalCode(address.getPostalCode());
            dto.setCountry(address.getCountry());
        }

        return dto;
    }

    private List<LocationDto> extractLocations(Bundle bundle) {
        List<LocationDto> locations = new ArrayList<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Location) {
                    locations.add(toLocationDto((Location) entry.getResource()));
                }
            }
        }
        return locations;
    }
}
 