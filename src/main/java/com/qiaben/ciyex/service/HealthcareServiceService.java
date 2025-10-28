package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.HealthcareServiceDto;
import com.qiaben.ciyex.entity.HealthcareService;
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
        HealthcareService savedEntity = repository.save(entity);
        return mapToDto(savedEntity);
    }


    public List<HealthcareServiceDto> getByOrgId() {
        List<HealthcareService> services = repository.findAll();
        return services.stream().map(this::mapToDto).collect(Collectors.toList());
    }


    public HealthcareServiceDto update(Long id, HealthcareServiceDto dto) {
        // Fetch the healthcare service by ID
        HealthcareService entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Healthcare Service not found"));

        // Update entity with DTO data
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setLocation(dto.getLocation());
        entity.setType(dto.getType());
        entity.setHoursOfOperation(dto.getHoursOfOperation());

        // Save the updated entity
        HealthcareService updatedEntity = repository.save(entity);
        return mapToDto(updatedEntity);
    }

    public void delete(Long id) {
        
        HealthcareService entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Healthcare Service not found"));

        // Delete the entity
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
        return dto;
    }

    private HealthcareService mapToEntity(HealthcareServiceDto dto) {
        HealthcareService entity = new HealthcareService();
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setLocation(dto.getLocation());
        entity.setType(dto.getType());
        entity.setHoursOfOperation(dto.getHoursOfOperation());
        return entity;
    }
}
