package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.InsuranceCompanyDto;
import com.qiaben.ciyex.entity.InsuranceCompany;
import com.qiaben.ciyex.entity.InsuranceStatus;
import com.qiaben.ciyex.exception.ResourceNotFoundException;
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
        // Validate mandatory fields
        validateMandatoryFields(dto);

        InsuranceCompany insuranceCompany = mapToEntity(dto);

        String externalId = dto.getExternalId(); // Start with DTO's externalId
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            try {
                ExternalStorage<InsuranceCompanyDto> externalStorage = storageResolver.resolve(InsuranceCompanyDto.class);
                externalId = externalStorage.create(dto); // Override with external storage ID if available
                log.info("Successfully created insurance company in external storage with externalId: {}", externalId);
            } catch (Exception e) {
                log.warn("Failed to sync with external storage, falling back to local generation: {}", e.getMessage());
                // Fall back to auto-generation if external storage fails
                externalId = null;
            }
        }

        // Auto-generate externalId if not provided, no external storage, or external storage failed
        if (externalId == null) {
            externalId = "INS-" + System.currentTimeMillis();
            log.info("Auto-generated externalId: {}", externalId);
        }

        insuranceCompany.setFhirId(externalId);
        insuranceCompany = repository.save(insuranceCompany);
        log.info("Created insurance company with id: {} and fhirId: {}", insuranceCompany.getId(), insuranceCompany.getFhirId());

        return mapToDto(insuranceCompany);
    }

    @Transactional
    public InsuranceCompanyDto updateStatus(Long id, InsuranceStatus status) {
        InsuranceCompany entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance company not found with id: " + id));
        entity.setStatus(status);
        entity = repository.save(entity);
        return mapToDto(entity);
    }


    @Transactional(readOnly = true)
    public InsuranceCompanyDto getById(Long id) {
        InsuranceCompany entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance company not found with id: " + id));
        return mapToDto(entity);
    }

    @Transactional
    public InsuranceCompanyDto update(Long id, InsuranceCompanyDto dto) {
        InsuranceCompany entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance company not found with id: " + id));
        entity = updateEntityFromDto(entity, dto);
        // Determine externalId priority: DTO.externalId > existing entity.fhirId
        String externalId = dto.getExternalId() != null ? dto.getExternalId() : entity.getFhirId();
        String storageType = configProvider.getStorageTypeForCurrentOrg();

        if (storageType != null) {
            try {
                ExternalStorage<InsuranceCompanyDto> externalStorage = storageResolver.resolve(InsuranceCompanyDto.class);
                if (externalId != null) {
                    // Try update in external storage
                    externalStorage.update(dto, externalId);
                    log.info("Updated insurance company in external storage with externalId={}", externalId);
                } else {
                    // No external id yet - try creating in external storage
                    String created = externalStorage.create(dto);
                    if (created != null) {
                        externalId = created;
                        log.info("Created insurance company in external storage with externalId={}", externalId);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to sync with external storage during update, falling back to local state: {}", e.getMessage());
                // fall back to local handling below
                externalId = null;
            }
        }

        // Ensure we have an external/fhir id; prefer DTO value, then existing entity, else generate
        if (externalId == null) {
            if (dto.getExternalId() != null) externalId = dto.getExternalId();
            else if (entity.getFhirId() != null) externalId = entity.getFhirId();
            else externalId = "INS-" + System.currentTimeMillis();
            log.info("Using local externalId for insurance company id={} externalId={}", entity.getId(), externalId);
        }

        entity.setFhirId(externalId);
        entity = repository.save(entity);
        return mapToDto(entity);
    }

    @Transactional
    public void delete(Long id) {
        InsuranceCompany entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance company not found with id: " + id));
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
        dto.setExternalId(entity.getFhirId()); // externalId is an alias for fhirId
        dto.setPayerId(entity.getPayerId());
        dto.setStatus(entity.getStatus().name());

        // Initialize and set audit dates
        InsuranceCompanyDto.Audit audit = new InsuranceCompanyDto.Audit();
        if (entity.getCreatedDate() != null) {
            audit.setCreatedDate(entity.getCreatedDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        if (entity.getLastModifiedDate() != null) {
            audit.setLastModifiedDate(entity.getLastModifiedDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
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
                .payerId(dto.getPayerId())
                .status(InsuranceStatus.ACTIVE)
                .build();
    }

    private InsuranceCompany updateEntityFromDto(InsuranceCompany entity, InsuranceCompanyDto dto) {
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getAddress() != null) entity.setAddress(dto.getAddress());
        if (dto.getCity() != null) entity.setCity(dto.getCity());
        if (dto.getState() != null) entity.setState(dto.getState());
        if (dto.getPostalCode() != null) entity.setPostalCode(dto.getPostalCode());
        if (dto.getCountry() != null) entity.setCountry(dto.getCountry());
        if (dto.getPayerId() != null) entity.setPayerId(dto.getPayerId()); //
        return entity;
    }

    private void validateMandatoryFields(InsuranceCompanyDto dto) {
        StringBuilder errors = new StringBuilder();

        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            errors.append("Name is mandatory. ");
        }
        if (dto.getAddress() == null || dto.getAddress().trim().isEmpty()) {
            errors.append("Address is mandatory. ");
        }
        if (dto.getCity() == null || dto.getCity().trim().isEmpty()) {
            errors.append("City is mandatory. ");
        }
        if (dto.getState() == null || dto.getState().trim().isEmpty()) {
            errors.append("State is mandatory. ");
        }
        if (dto.getPostalCode() == null || dto.getPostalCode().trim().isEmpty()) {
            errors.append("Postal code is mandatory. ");
        }
        if (dto.getPayerId() == null || dto.getPayerId().trim().isEmpty()) {
            errors.append("Payer ID is mandatory. ");
        }
        if (dto.getCountry() == null || dto.getCountry().trim().isEmpty()) {
            errors.append("Country is mandatory. ");
        }

        if (errors.length() > 0) {
            throw new IllegalArgumentException(errors.toString().trim());
        }
    }
}