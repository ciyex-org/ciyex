package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.LocationDto;
import com.qiaben.ciyex.entity.Location;
import com.qiaben.ciyex.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final LocationRepository repository;

    @Transactional
    public LocationDto create(LocationDto dto) {
        Location location = mapToEntity(dto);
        repository.save(location);
        log.info("Created new location with id: {}", location.getId());
        return mapToDto(location);
    }

    @Transactional(readOnly = true)
    public LocationDto getById(Long id) {
        Location location = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found with id: " + id));
        log.info("Fetched location with id: {}", id);
        return mapToDto(location);
    }

    @Transactional
    public LocationDto update(Long id, LocationDto dto) {
        Location location = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found with id: " + id));

        if (dto.getExternalId() != null) location.setExternalId(dto.getExternalId());
        if (dto.getName() != null) location.setName(dto.getName());
        if (dto.getAddress() != null) location.setAddress(dto.getAddress());
        if (dto.getCity() != null) location.setCity(dto.getCity());
        if (dto.getState() != null) location.setState(dto.getState());
        if (dto.getPostalCode() != null) location.setPostalCode(dto.getPostalCode());
        if (dto.getCountry() != null) location.setCountry(dto.getCountry());

        repository.save(location);
        log.info("Updated location with id: {}", id);
        return mapToDto(location);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Location not found with id: " + id);
        }
        repository.deleteById(id);
        log.info("Deleted location with id: {}", id);
    }

    @Transactional(readOnly = true)
    public Page<LocationDto> getAll(Pageable pageable) {
        Page<Location> locations = repository.findAll(pageable);
        log.info("Retrieved {} locations", locations.getTotalElements());
        return locations.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<LocationDto> search(String keyword, Pageable pageable) {
        Page<Location> results = repository.searchBy(keyword.toLowerCase(), pageable);
        log.info("Found {} matching locations for keyword '{}'", results.getTotalElements(), keyword);
        return results.map(this::mapToDto);
    }

    private Location mapToEntity(LocationDto dto) {
        return Location.builder()
                .externalId(dto.getExternalId())
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
        dto.setId(location.getId());
        dto.setExternalId(location.getExternalId());
        dto.setName(location.getName());
        dto.setAddress(location.getAddress());
        dto.setCity(location.getCity());
        dto.setState(location.getState());
        dto.setPostalCode(location.getPostalCode());
        dto.setCountry(location.getCountry());

        LocationDto.Audit audit = new LocationDto.Audit();
        if (location.getCreatedDate() != null) {
            audit.setCreatedDate(location.getCreatedDate().format(DATE_FORMATTER));
        }
        if (location.getLastModifiedDate() != null) {
            audit.setLastModifiedDate(location.getLastModifiedDate().format(DATE_FORMATTER));
        }
        dto.setAudit(audit);

        return dto;
    }
}
 