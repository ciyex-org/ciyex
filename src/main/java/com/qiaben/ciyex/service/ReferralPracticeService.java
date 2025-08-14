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
        String externalId = null;
        ReferralPractice referralPractice = mapToEntity(dto);

        // Set current timestamp for created and last modified date
        String currentDate = LocalDateTime.now().format(DATE_FORMATTER);
        referralPractice.setCreatedDate(currentDate);
        referralPractice.setLastModifiedDate(currentDate);

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            try {
                ExternalStorage<ReferralPracticeDto> externalStorage = storageResolver.resolve(ReferralPracticeDto.class);
                externalId = externalStorage.create(dto);
                log.info("Successfully created referral practice in external storage with externalId: {}", externalId);
            } catch (Exception e) {
                log.error("Failed to create referral practice in external storage: {}", e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e);
            }
        }

        referralPractice = repository.save(referralPractice);
        if (externalId != null) {
            referralPractice.setFhirId(externalId);
            referralPractice = repository.save(referralPractice);
            log.info("Created referral practice with id: {} and externalId: {}", referralPractice.getId(), externalId);
        }

        return mapToDto(referralPractice);
    }

    @Transactional(readOnly = true)
    public ReferralPracticeDto getById(Long id) {
        ReferralPractice entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Referral practice not found"));
        return mapToDto(entity);
    }

    @Transactional
    public ReferralPracticeDto update(Long id, ReferralPracticeDto dto) {
        ReferralPractice entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Referral practice not found"));
        entity = updateEntityFromDto(entity, dto);
        entity.setLastModifiedDate(LocalDateTime.now().format(DATE_FORMATTER));

        String externalId = entity.getFhirId();
        if (externalId != null) {
            try {
                ExternalStorage<ReferralPracticeDto> externalStorage = storageResolver.resolve(ReferralPracticeDto.class);
                externalStorage.update(dto, externalId);
            } catch (Exception e) {
                log.error("Failed to update referral practice in external storage: {}", e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e);
            }
        }

        entity = repository.save(entity);
        return mapToDto(entity);
    }

    @Transactional
    public void delete(Long id) {
        ReferralPractice entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Referral practice not found"));
        repository.delete(entity);
    }

    @Transactional(readOnly = true)
    public List<ReferralPracticeDto> getAll() {
        List<ReferralPractice> entities = repository.findAll();
        return entities.stream().map(this::mapToDto).collect(Collectors.toList());
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

        // Initialize and set audit dates
        ReferralPracticeDto.Audit audit = new ReferralPracticeDto.Audit();
        audit.setCreatedDate(entity.getCreatedDate());
        audit.setLastModifiedDate(entity.getLastModifiedDate());
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