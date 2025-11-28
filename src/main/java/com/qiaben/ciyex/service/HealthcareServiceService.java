package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.HealthcareServiceDto;
import com.qiaben.ciyex.entity.HealthcareService;
import com.qiaben.ciyex.exception.ResourceNotFoundException;
import com.qiaben.ciyex.repository.HealthcareServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HealthcareServiceService {

    private final HealthcareServiceRepository repository;

    @Autowired
    public HealthcareServiceService(HealthcareServiceRepository repository) {
        this.repository = repository;
    }

    public HealthcareServiceDto create(HealthcareServiceDto dto) {
        HealthcareService entity = mapToEntity(dto);
        
        // Auto-generate external ID if not provided
        if (dto.getExternalId() != null && !dto.getExternalId().isBlank()) {
            entity.setExternalId(dto.getExternalId());
        } else if (entity.getExternalId() == null || entity.getExternalId().isBlank()) {
            entity.setExternalId("HCS-" + java.util.UUID.randomUUID().toString());
        }
        
        HealthcareService savedEntity = repository.save(entity);
        return mapToDto(savedEntity);
    }

    public List<HealthcareServiceDto> getAll() {
        return repository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public HealthcareServiceDto getById(Long id) {
        HealthcareService entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HealthcareService", "id", (Object) id));
        return mapToDto(entity);
    }

    public HealthcareServiceDto update(Long id, HealthcareServiceDto dto) {
        HealthcareService entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HealthcareService", "id", (Object) id));

        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setLocation(dto.getLocation());
        entity.setType(dto.getType());
        entity.setHoursOfOperation(dto.getHoursOfOperation());

        HealthcareService updatedEntity = repository.save(entity);
        return mapToDto(updatedEntity);
    }

    public void delete(Long id) {
        HealthcareService entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HealthcareService", "id", (Object) id));
        repository.delete(entity);
    }

    private HealthcareServiceDto mapToDto(HealthcareService entity) {
        HealthcareServiceDto dto = new HealthcareServiceDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setLocation(entity.getLocation());
        dto.setType(entity.getType());
        dto.setHoursOfOperation(entity.getHoursOfOperation());
        dto.setExternalId(entity.getExternalId());
        dto.setFhirId(entity.getExternalId());
        
        // Always create audit object to avoid null
        HealthcareServiceDto.Audit audit = new HealthcareServiceDto.Audit();
        audit.setCreatedDate(entity.getCreatedDate() != null ? entity.getCreatedDate().toString() : null);
        audit.setLastModifiedDate(entity.getLastModifiedDate() != null ? entity.getLastModifiedDate().toString() : null);
        dto.setAudit(audit);
        
        return dto;
    }

    private HealthcareService mapToEntity(HealthcareServiceDto dto) {
        HealthcareService entity = new HealthcareService();
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setLocation(dto.getLocation());
        entity.setType(dto.getType());
        entity.setHoursOfOperation(dto.getHoursOfOperation());
        if (dto.getExternalId() != null && !dto.getExternalId().isBlank()) {
            entity.setExternalId(dto.getExternalId());
        }
        return entity;
    }
}
