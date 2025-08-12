package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.InsuranceCompanyDto;
import com.qiaben.ciyex.entity.InsuranceCompany;
import com.qiaben.ciyex.repository.InsuranceCompanyRepository;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InsuranceCompanyService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final InsuranceCompanyRepository repository;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;

    @Autowired
    public InsuranceCompanyService(InsuranceCompanyRepository repository, ExternalStorageResolver storageResolver, OrgIntegrationConfigProvider configProvider) {
        this.repository = repository;
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
    }

    @Transactional
    public InsuranceCompanyDto create(InsuranceCompanyDto dto) {
        String externalId = null;
        InsuranceCompany insuranceCompany = mapToEntity(dto);

        // Set current timestamp for created and last modified date
        String currentDate = LocalDateTime.now().format(DATE_FORMATTER);
        insuranceCompany.setCreatedDate(currentDate);
        insuranceCompany.setLastModifiedDate(currentDate);

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            try {
                ExternalStorage<InsuranceCompanyDto> externalStorage = storageResolver.resolve(InsuranceCompanyDto.class);
                externalId = externalStorage.create(dto);
                log.info("Successfully created insurance company in external storage with externalId: {}", externalId);
            } catch (Exception e) {
                log.error("Failed to create insurance company in external storage: {}", e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e);
            }
        }

        insuranceCompany = repository.save(insuranceCompany);
        if (externalId != null) {
            insuranceCompany.setFhirId(externalId);
            insuranceCompany = repository.save(insuranceCompany);  // Update with externalId
            log.info("Created insurance company with id: {} and externalId: {}", insuranceCompany.getId(), externalId);
        }

        return mapToDto(insuranceCompany);
    }

    @Transactional(readOnly = true)
    public InsuranceCompanyDto getById(Long id) {
        InsuranceCompany entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Insurance company not found"));
        return mapToDto(entity);
    }

    @Transactional
    public InsuranceCompanyDto update(Long id, InsuranceCompanyDto dto) {
        InsuranceCompany entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Insurance company not found"));
        entity = updateEntityFromDto(entity, dto);

        // Set last modified date to current timestamp
        entity.setLastModifiedDate(LocalDateTime.now().format(DATE_FORMATTER));

        String externalId = entity.getFhirId();
        if (externalId != null) {
            try {
                ExternalStorage<InsuranceCompanyDto> externalStorage = storageResolver.resolve(InsuranceCompanyDto.class);
                externalStorage.update(dto, externalId);
            } catch (Exception e) {
                log.error("Failed to update insurance company in external storage: {}", e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e);
            }
        }

        entity = repository.save(entity);
        return mapToDto(entity);
    }

    @Transactional
    public void delete(Long id) {
        InsuranceCompany entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Insurance company not found"));
        repository.delete(entity);
    }

    @Transactional(readOnly = true)
    public List<InsuranceCompanyDto> getAll() {
        List<InsuranceCompany> entities = repository.findAll();
        return entities.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    private InsuranceCompanyDto mapToDto(InsuranceCompany entity) {
        InsuranceCompanyDto dto = new InsuranceCompanyDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setAddress(entity.getAddress());
        dto.setCity(entity.getCity());
        dto.setState(entity.getState());
        dto.setPostalCode(entity.getPostalCode());
        dto.setCountry(entity.getCountry());
        dto.setFhirId(entity.getFhirId());

        // Initialize and set audit dates
        InsuranceCompanyDto.Audit audit = new InsuranceCompanyDto.Audit();
        audit.setCreatedDate(entity.getCreatedDate());
        audit.setLastModifiedDate(entity.getLastModifiedDate());
        dto.setAudit(audit);

        return dto;
    }

    private InsuranceCompany mapToEntity(InsuranceCompanyDto dto) {
        return InsuranceCompany.builder()
                .name(dto.getName())
                .address(dto.getAddress())
                .city(dto.getCity())
                .state(dto.getState())
                .postalCode(dto.getPostalCode())
                .country(dto.getCountry())
                .build();
    }

    private InsuranceCompany updateEntityFromDto(InsuranceCompany entity, InsuranceCompanyDto dto) {
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getAddress() != null) entity.setAddress(dto.getAddress());
        if (dto.getCity() != null) entity.setCity(dto.getCity());
        if (dto.getState() != null) entity.setState(dto.getState());
        if (dto.getPostalCode() != null) entity.setPostalCode(dto.getPostalCode());
        if (dto.getCountry() != null) entity.setCountry(dto.getCountry());
        return entity;
    }
}