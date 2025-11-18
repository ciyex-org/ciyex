package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ReferralPracticeDto;
import com.qiaben.ciyex.entity.ReferralPractice;
import com.qiaben.ciyex.repository.ReferralPracticeRepository;
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
public class ReferralPracticeService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReferralPracticeRepository repository;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;

    @Autowired
    public ReferralPracticeService(ReferralPracticeRepository repository,
                                   ExternalStorageResolver storageResolver,
                                   OrgIntegrationConfigProvider configProvider) {
        this.repository = repository;
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
    }

    @Transactional
    public ReferralPracticeDto create(ReferralPracticeDto dto) {
        // Validate mandatory fields
        validateMandatoryFields(dto);

        ReferralPractice entity = mapToEntity(dto);
        // Save locally first
        entity = repository.save(entity);

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            try {
                ExternalStorage<ReferralPracticeDto> external = storageResolver.resolve(ReferralPracticeDto.class);
                String externalId = external.create(mapToDto(entity));
                entity.setFhirId(externalId);
                entity = repository.save(entity);
                log.info("Practice external sync (create) OK, externalId={}", externalId);
            } catch (Exception e) {
                log.warn("Practice external sync (create) failed and will be skipped: {}", e.toString());
            }
        }

        return mapToDto(entity);
    }

    private void validateMandatoryFields(ReferralPracticeDto dto) {
        StringBuilder errors = new StringBuilder();

        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            errors.append("name, ");
        }

        if (errors.length() > 0) {
            // Remove trailing comma and space
            String missingFields = errors.substring(0, errors.length() - 2);
            throw new IllegalArgumentException("Missing mandatory fields: " + missingFields);
        }
    }

    @Transactional(readOnly = true)
    public ReferralPracticeDto getById(Long id) {
        ReferralPractice entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Referral practice not found with id: " + id));
        return mapToDto(entity);
    }

    @Transactional
    public ReferralPracticeDto update(Long id, ReferralPracticeDto dto) {
        ReferralPractice entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Referral practice not found with id: " + id));

        entity = updateEntityFromDto(entity, dto);
        entity = repository.save(entity); // save first

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            try {
                ExternalStorage<ReferralPracticeDto> external = storageResolver.resolve(ReferralPracticeDto.class);
                if (entity.getFhirId() == null) {
                    String externalId = external.create(mapToDto(entity));
                    entity.setFhirId(externalId);
                    entity = repository.save(entity);
                    log.info("Practice external sync (create-on-update) OK, externalId={}", externalId);
                } else {
                    external.update(mapToDto(entity), entity.getFhirId());
                    log.info("Practice external sync (update) OK, externalId={}", entity.getFhirId());
                }
            } catch (Exception e) {
                log.warn("Practice external sync (update) failed and will be skipped: {}", e.toString());
            }
        }

        return mapToDto(entity);
    }

    @Transactional
    public void delete(Long id) {
        ReferralPractice entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Referral practice not found with id: " + id));
        repository.delete(entity);
    }

    @Transactional(readOnly = true)
    public List<ReferralPracticeDto> getAll() {
        return repository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    private ReferralPracticeDto mapToDto(ReferralPractice entity) {
        ReferralPracticeDto dto = new ReferralPracticeDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setAddress(entity.getAddress());
        dto.setCity(entity.getCity());
        dto.setState(entity.getState());
        dto.setPostalCode(entity.getPostalCode());
        dto.setCountry(entity.getCountry());
        dto.setPhoneNumber(entity.getPhoneNumber());
        dto.setEmail(entity.getEmail());
        dto.setFhirId(entity.getFhirId());

        ReferralPracticeDto.Audit audit = new ReferralPracticeDto.Audit();
        dto.setAudit(audit);

        return dto;
    }

    private ReferralPractice mapToEntity(ReferralPracticeDto dto) {
        return ReferralPractice.builder()
                .name(dto.getName())
                .address(dto.getAddress())
                .city(dto.getCity())
                .state(dto.getState())
                .postalCode(dto.getPostalCode())
                .country(dto.getCountry())
                .phoneNumber(dto.getPhoneNumber())
                .email(dto.getEmail())
                .build();
    }

    private ReferralPractice updateEntityFromDto(ReferralPractice entity, ReferralPracticeDto dto) {
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getAddress() != null) entity.setAddress(dto.getAddress());
        if (dto.getCity() != null) entity.setCity(dto.getCity());
        if (dto.getState() != null) entity.setState(dto.getState());
        if (dto.getPostalCode() != null) entity.setPostalCode(dto.getPostalCode());
        if (dto.getCountry() != null) entity.setCountry(dto.getCountry());
        if (dto.getPhoneNumber() != null) entity.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getEmail() != null) entity.setEmail(dto.getEmail());
        return entity;
    }
}