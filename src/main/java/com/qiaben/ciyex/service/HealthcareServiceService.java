package com.qiaben.ciyex.service;



import com.qiaben.ciyex.dto.HealthcareServiceDto;
import com.qiaben.ciyex.entity.HealthcareService;
import com.qiaben.ciyex.repository.HealthcareServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HealthcareServiceService {

    private final HealthcareServiceRepository healthcareServiceRepository;

    public HealthcareServiceService(HealthcareServiceRepository healthcareServiceRepository) {
        this.healthcareServiceRepository = healthcareServiceRepository;
    }

    @Transactional
    public HealthcareServiceDto create(HealthcareServiceDto dto) {
        HealthcareService healthcareService = new HealthcareService();
        healthcareService.setName(dto.getName());
        healthcareService.setDescription(dto.getDescription());
        healthcareService.setType(dto.getType());
        healthcareService.setLocation(dto.getLocation());

        healthcareService = healthcareServiceRepository.save(healthcareService);
        return mapToDto(healthcareService);
    }

    @Transactional(readOnly = true)
    public HealthcareServiceDto getById(Long id) {
        HealthcareService healthcareService = healthcareServiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("HealthcareService not found"));
        return mapToDto(healthcareService);
    }

    @Transactional
    public HealthcareServiceDto update(Long id, HealthcareServiceDto dto) {
        HealthcareService healthcareService = healthcareServiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("HealthcareService not found"));

        healthcareService.setName(dto.getName());
        healthcareService.setDescription(dto.getDescription());
        healthcareService.setType(dto.getType());
        healthcareService.setLocation(dto.getLocation());

        healthcareService = healthcareServiceRepository.save(healthcareService);
        return mapToDto(healthcareService);
    }

    @Transactional
    public void delete(Long id) {
        HealthcareService healthcareService = healthcareServiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("HealthcareService not found"));
        healthcareServiceRepository.delete(healthcareService);
    }

    private HealthcareServiceDto mapToDto(HealthcareService healthcareService) {
        HealthcareServiceDto dto = new HealthcareServiceDto();
        dto.setId(healthcareService.getId());
        dto.setName(healthcareService.getName());
        dto.setDescription(healthcareService.getDescription());
        dto.setType(healthcareService.getType());
        dto.setLocation(healthcareService.getLocation());
        return dto;
    }
}
