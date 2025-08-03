package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.LocationDto;
import com.qiaben.ciyex.dto.core.integration.RequestContext;
import com.qiaben.ciyex.entity.Location;
import com.qiaben.ciyex.repository.LocationRepository;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LocationService {

    private final LocationRepository repository;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;

    @Autowired
    public LocationService(LocationRepository repository, ExternalStorageResolver storageResolver, OrgIntegrationConfigProvider configProvider) {
        this.repository = repository;
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
    }

    @Transactional
    public LocationDto create(LocationDto dto) {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            throw new SecurityException("No orgId available in request context");
        }
        log.debug("Verifying access for orgId: {} to create new location", currentOrgId);
        dto.setOrgId(currentOrgId); // Set orgId for the new location

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType == null) {
            throw new RuntimeException("No external storage configured for location creation");
        }

        Location location = mapToEntity(dto);
        location.setOrgId(currentOrgId); // Ensure orgId is set in entity
        String externalId = null;

        // Attempt external storage creation first
        try {
            ExternalStorage<LocationDto> externalStorage = storageResolver.resolve(LocationDto.class);
            externalId = externalStorage.create(dto);
            log.info("Successfully created location in external storage with externalId: {} for orgId: {}", externalId, currentOrgId);
        } catch (Exception e) {
            log.error("Failed to create location in external storage for orgId: {}, error: {}", currentOrgId, e.getMessage());
            throw new RuntimeException("Failed to sync with external storage", e); // Rollback transaction
        }

        // Save to database only if external storage succeeded
        location.setExternalId(externalId);
        location = repository.save(location);
        log.debug("Saved location to DB: id={}, externalId={}, orgId={}", location.getId(), location.getExternalId(), location.getOrgId());
        if (location.getId() == null) {
            log.error("Database save failed to generate id for location with externalId: {} and orgId: {}", externalId, currentOrgId);
            throw new RuntimeException("Failed to generate id for new location");
        }
        dto.setId(location.getId()); // Set database id in DTO
        dto.setExternalId(externalId); // Set externalId in DTO
        log.info("Created location with id: {} and externalId: {} in DB for orgId: {}", location.getId(), externalId, currentOrgId);

        return dto;
    }

    @Transactional(readOnly = true)
    public LocationDto getById(Long id) {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            throw new SecurityException("No orgId available in request context");
        }
        log.debug("Verifying access for orgId: {} to location with id: {}", currentOrgId, id);

        Location location = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found with id: " + id));
        log.debug("Fetched location from DB: id={}, externalId={}, orgId={}", location.getId(), location.getExternalId(), location.getOrgId());
        if (!currentOrgId.equals(location.getOrgId())) {
            throw new SecurityException("Access denied: Location with id " + id + " does not belong to orgId " + currentOrgId);
        }

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        LocationDto locationDto = mapToDto(location);
        if (storageType != null && location.getExternalId() != null) {
            ExternalStorage<LocationDto> externalStorage = storageResolver.resolve(LocationDto.class);
            LocationDto syncedDto = externalStorage.get(location.getExternalId());
            if (syncedDto != null) {
                log.debug("Fetched synced LocationDto from external storage: id={}, externalId={}, orgId={}", syncedDto.getId(), syncedDto.getExternalId(), syncedDto.getOrgId());
                syncedDto.setId(location.getId()); // Preserve database id
                locationDto = syncedDto;
            } else {
                log.warn("No synced data found in external storage for location id: {} with externalId: {} for orgId: {}", id, location.getExternalId(), currentOrgId);
            }
        }
        log.info("Returning location dto for id: {} and orgId: {}", id, currentOrgId);
        log.debug("Returning LocationDto: id={}, externalId={}, orgId={}", locationDto.getId(), locationDto.getExternalId(), locationDto.getOrgId());
        return locationDto;
    }

    @Transactional
    public LocationDto update(Long id, LocationDto dto) {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            throw new SecurityException("No orgId available in request context");
        }
        log.debug("Verifying access for orgId: {} to location with id: {}", currentOrgId, id);

        Location location = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found with id: " + id));
        log.debug("Fetched location from DB: id={}, externalId={}, orgId={}", location.getId(), location.getExternalId(), location.getOrgId());
        if (!currentOrgId.equals(location.getOrgId())) {
            throw new SecurityException("Access denied: Location with id " + id + " does not belong to orgId " + currentOrgId);
        }

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && location.getExternalId() != null) {
            try {
                ExternalStorage<LocationDto> externalStorage = storageResolver.resolve(LocationDto.class);
                externalStorage.update(dto, location.getExternalId());
                log.info("Successfully updated location with id: {} and externalId: {} in external storage for orgId: {}", id, location.getExternalId(), currentOrgId);
            } catch (Exception e) {
                log.error("Failed to update location in external storage for orgId: {}, error: {}", currentOrgId, e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e); // Rollback transaction
            }
        }

        updateEntityFromDto(location, dto);
        location = repository.save(location);
        log.debug("Saved updated location to DB: id={}, externalId={}, orgId={}", location.getId(), location.getExternalId(), location.getOrgId());
        dto.setId(location.getId()); // Set database id in DTO
        dto.setExternalId(location.getExternalId()); // Update externalId in DTO if changed
        log.info("Updated location with id: {} and externalId: {} in DB for orgId: {}", id, location.getExternalId(), currentOrgId);

        return dto;
    }

    @Transactional
    public void delete(Long id) {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            throw new SecurityException("No orgId available in request context");
        }
        log.debug("Verifying access for orgId: {} to location with id: {}", currentOrgId, id);

        Location location = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found with id: " + id));
        log.debug("Fetched location from DB: id={}, externalId={}, orgId={}", location.getId(), location.getExternalId(), location.getOrgId());
        if (!currentOrgId.equals(location.getOrgId())) {
            throw new SecurityException("Access denied: Location with id " + id + " does not belong to orgId " + currentOrgId);
        }

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && location.getExternalId() != null) {
            try {
                ExternalStorage<LocationDto> externalStorage = storageResolver.resolve(LocationDto.class);
                externalStorage.delete(location.getExternalId());
                log.info("Successfully deleted location with id: {} and externalId: {} from external storage for orgId: {}", id, location.getExternalId(), currentOrgId);
            } catch (Exception e) {
                log.error("Failed to delete location from external storage for orgId: {}, error: {}", currentOrgId, e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e); // Rollback transaction
            }
        }

        repository.delete(location);
        log.info("Deleted location with id: {} from DB for orgId: {}", id, currentOrgId);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<LocationDto>> getAllLocations() {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            return ApiResponse.<List<LocationDto>>builder()
                    .success(false)
                    .message("No orgId available in request context")
                    .build();
        }
        log.debug("Verifying access for orgId: {} to retrieve all locations", currentOrgId);

        // Fetch all locations directly from the database
        List<Location> locations = repository.findAllByOrgId(currentOrgId);
        log.info("Retrieved {} locations from DB for orgId: {}", locations.size(), currentOrgId);
        List<LocationDto> locationDtos = locations.stream().map(this::mapToDto).collect(Collectors.toList());

        return ApiResponse.<List<LocationDto>>builder()
                .success(true)
                .message("Locations retrieved successfully")
                .data(locationDtos)
                .build();
    }

    private Location mapToEntity(LocationDto dto) {
        return Location.builder()
                .name(dto.getName())
                .address(dto.getAddress())
                .city(dto.getCity())
                .state(dto.getState())
                .postalCode(dto.getPostalCode())
                .country(dto.getCountry())
                .orgId(dto.getOrgId()) // Set orgId from DTO
                .build();
    }

    private LocationDto mapToDto(Location location) {
        LocationDto dto = new LocationDto();
        dto.setId(location.getId());
        dto.setExternalId(location.getExternalId());
        dto.setName(location.getName());
        dto.setAddress(location.getAddress());
        dto.setCity(location.getCity());
        dto.setState(location.getState());
        dto.setPostalCode(location.getPostalCode());
        dto.setCountry(location.getCountry());
        dto.setOrgId(location.getOrgId()); // Include orgId in DTO
        return dto;
    }

    private void updateEntityFromDto(Location location, LocationDto dto) {
        if (dto.getName() != null) location.setName(dto.getName());
        if (dto.getAddress() != null) location.setAddress(dto.getAddress());
        if (dto.getCity() != null) location.setCity(dto.getCity());
        if (dto.getState() != null) location.setState(dto.getState());
        if (dto.getPostalCode() != null) location.setPostalCode(dto.getPostalCode());
        if (dto.getCountry() != null) location.setCountry(dto.getCountry());
        if (dto.getOrgId() != null) location.setOrgId(dto.getOrgId()); // Update orgId if provided
    }

    private Long getCurrentOrgId() {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        if (orgId == null) {
            log.warn("orgId is null in RequestContext");
        }
        return orgId;
    }
}