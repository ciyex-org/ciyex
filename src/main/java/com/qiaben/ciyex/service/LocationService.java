package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.LocationDto;
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
        // orgId deprecated; tenantName isolation handled upstream. orgId assignments removed.

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType == null) {
            throw new RuntimeException("No external storage configured for location creation");
        }

        Location location = mapToEntity(dto);
    // orgId no longer set on entity (deprecated)
        String externalId = null;

        // Attempt external storage creation first
        try {
            ExternalStorage<LocationDto> externalStorage = storageResolver.resolve(LocationDto.class);
            externalId = externalStorage.create(dto);
            log.info("Successfully created location in external storage with externalId: {}", externalId);
        } catch (Exception e) {
            log.error("Failed to create location in external storage, error: {}", e.getMessage());
            throw new RuntimeException("Failed to sync with external storage", e); // Rollback transaction
        }

        // Save to database only if external storage succeeded
        location.setExternalId(externalId);
        location = repository.save(location);
    log.debug("Saved location to DB: id={}, externalId={}", location.getId(), location.getExternalId());
        if (location.getId() == null) {
            log.error("Database save failed to generate id for location with externalId: {}", externalId);
            throw new RuntimeException("Failed to generate id for new location");
        }
        dto.setId(location.getId()); // Set database id in DTO
        dto.setExternalId(externalId); // Set externalId in DTO
    log.info("Created location with id: {} and externalId: {}", location.getId(), externalId);

        return dto;
    }

    @Transactional(readOnly = true)
    public LocationDto getById(Long id) {
        // orgId deprecated; skipping org access verification.

        Location location = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found with id: " + id));
    log.debug("Fetched location from DB: id={}, externalId={}", location.getId(), location.getExternalId());
        // org ownership check removed.

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        LocationDto locationDto = mapToDto(location);
        if (storageType != null && location.getExternalId() != null) {
            ExternalStorage<LocationDto> externalStorage = storageResolver.resolve(LocationDto.class);
            LocationDto syncedDto = externalStorage.get(location.getExternalId());
            if (syncedDto != null) {
                log.debug("Fetched synced LocationDto from external storage: id={}, externalId={}", syncedDto.getId(), syncedDto.getExternalId());
                syncedDto.setId(location.getId()); // Preserve database id
                locationDto = syncedDto;
            } else {
                log.warn("No synced data found in external storage for location id: {} with externalId: {}", id, location.getExternalId());
            }
        }
    log.info("Returning location dto for id: {}", id);
    log.debug("Returning LocationDto: id={}, externalId={}", locationDto.getId(), locationDto.getExternalId());
        return locationDto;
    }

    @Transactional
    public LocationDto update(Long id, LocationDto dto) {
        // orgId deprecated; skipping org access verification.

        Location location = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found with id: " + id));
    log.debug("Fetched location from DB: id={}, externalId={}", location.getId(), location.getExternalId());
        // org ownership check removed.

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && location.getExternalId() != null) {
            try {
                ExternalStorage<LocationDto> externalStorage = storageResolver.resolve(LocationDto.class);
                externalStorage.update(dto, location.getExternalId());
                log.info("Successfully updated location with id: {} and externalId: {} in external storage", id, location.getExternalId());
            } catch (Exception e) {
                log.error("Failed to update location in external storage: error: {}", e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e); // Rollback transaction
            }
        }

        updateEntityFromDto(location, dto);
        location = repository.save(location);
    log.debug("Saved updated location to DB: id={}, externalId={}", location.getId(), location.getExternalId());
        dto.setId(location.getId()); // Set database id in DTO
        dto.setExternalId(location.getExternalId()); // Update externalId in DTO if changed
    log.info("Updated location with id: {} and externalId: {} in DB", id, location.getExternalId());

        return dto;
    }

    @Transactional
    public void delete(Long id) {
        // orgId deprecated; skipping org access verification.

        Location location = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found with id: " + id));
    log.debug("Fetched location from DB: id={}, externalId={}", location.getId(), location.getExternalId());
        // org ownership check removed.

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && location.getExternalId() != null) {
            try {
                ExternalStorage<LocationDto> externalStorage = storageResolver.resolve(LocationDto.class);
                externalStorage.delete(location.getExternalId());
                log.info("Successfully deleted location with id: {} and externalId: {} from external storage", id, location.getExternalId());
            } catch (Exception e) {
                log.error("Failed to delete location from external storage: error: {}", e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e); // Rollback transaction
            }
        }

        repository.delete(location);
    log.info("Deleted location with id: {} from DB", id);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<LocationDto>> getAllLocations() {
    // orgId deprecated; retrieving all locations for tenant schema.

        // Fetch all locations directly from the database
    List<Location> locations = repository.findAll();
    log.info("Retrieved {} locations from DB", locations.size());
        List<LocationDto> locationDtos = locations.stream().map(this::mapToDto).collect(Collectors.toList());

        return ApiResponse.<List<LocationDto>>builder()
                .success(true)
                .message("Locations retrieved successfully (tenant schema isolated)")
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
                // orgId deprecated
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
    // orgId removed from DTO
        return dto;
    }

    private void updateEntityFromDto(Location location, LocationDto dto) {
        if (dto.getName() != null) location.setName(dto.getName());
        if (dto.getAddress() != null) location.setAddress(dto.getAddress());
        if (dto.getCity() != null) location.setCity(dto.getCity());
        if (dto.getState() != null) location.setState(dto.getState());
        if (dto.getPostalCode() != null) location.setPostalCode(dto.getPostalCode());
        if (dto.getCountry() != null) location.setCountry(dto.getCountry());
    // orgId removed from DTO
    }

    // orgId deprecated; method removed.
}