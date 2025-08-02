package com.qiaben.ciyex.service.core;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.core.LocationDto;
import com.qiaben.ciyex.entity.Location;
import com.qiaben.ciyex.storage.ExternalOrgStorage;
import com.qiaben.ciyex.storage.ExternalOrgStorageResolver;
import com.qiaben.ciyex.storage.fhir.FhirExternalOrgStorage;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LocationService {

    private final ExternalOrgStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;

    @Autowired
    public LocationService(ExternalOrgStorageResolver storageResolver, OrgIntegrationConfigProvider configProvider) {
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
    }

    public LocationDto create(LocationDto dto) {
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType == null) {
            throw new RuntimeException("No external storage configured for location creation");
        }

        Location location = mapToEntity(dto);
        ExternalOrgStorage externalStorage = storageResolver.resolve();
        String externalId = externalStorage.createLocation(location);
        location.setExternalId(externalId);
        return mapToDto(location);
    }

    public LocationDto getById(String externalId) {
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType == null) {
            throw new RuntimeException("No external storage configured for location retrieval");
        }

        ExternalOrgStorage externalStorage = storageResolver.resolve();
        Location location = externalStorage.getLocation(externalId);
        if (location == null) {
            throw new RuntimeException("Location not found with externalId: " + externalId);
        }
        return mapToDto(location);
    }

    public LocationDto update(String externalId, LocationDto dto) {
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType == null) {
            throw new RuntimeException("No external storage configured for location update");
        }

        Location location = mapToEntity(dto);
        location.setExternalId(externalId);
        ExternalOrgStorage externalStorage = storageResolver.resolve();
        externalStorage.updateLocation(location, externalId);
        return mapToDto(location);
    }

    public void delete(String externalId) {
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType == null) {
            throw new RuntimeException("No external storage configured for location deletion");
        }

        ExternalOrgStorage externalStorage = storageResolver.resolve();
        externalStorage.deleteLocation(externalId);
    }

    public ApiResponse<List<LocationDto>> getAllLocations() {
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType == null) {
            return ApiResponse.<List<LocationDto>>builder()
                    .success(false)
                    .message("No external storage configured for location retrieval")
                    .build();
        }

        ExternalOrgStorage externalStorage = storageResolver.resolve();
        if (!(externalStorage instanceof FhirExternalOrgStorage)) {
            return ApiResponse.<List<LocationDto>>builder()
                    .success(false)
                    .message("getAllLocations is only supported for FHIR storage")
                    .build();
        }

        FhirExternalOrgStorage fhirStorage = (FhirExternalOrgStorage) externalStorage;
        List<Location> locations = fhirStorage.searchAllLocations();
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
                .build();
    }

    private LocationDto mapToDto(Location location) {
        LocationDto dto = new LocationDto();
        dto.setExternalId(location.getExternalId());
        dto.setName(location.getName());
        dto.setAddress(location.getAddress());
        dto.setCity(location.getCity());
        dto.setState(location.getState());
        dto.setPostalCode(location.getPostalCode());
        dto.setCountry(location.getCountry());
        return dto;
    }
}