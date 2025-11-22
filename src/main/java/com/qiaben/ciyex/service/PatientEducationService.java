package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.PatientEducationDto;
import com.qiaben.ciyex.entity.PatientEducation;
import com.qiaben.ciyex.repository.PatientEducationRepository;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import com.qiaben.ciyex.dto.integration.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class PatientEducationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final PatientEducationRepository repository;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;

    public PatientEducationService(PatientEducationRepository repository,
                                   ExternalStorageResolver storageResolver,
                                   OrgIntegrationConfigProvider configProvider) {
        this.repository = repository;
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
    }

    @Transactional
    public PatientEducationDto create(PatientEducationDto dto) {
        // Validate mandatory fields
        validateMandatoryFields(dto);
        
        PatientEducation entity = mapToEntity(dto);


        String externalId = dto.getFhirId();
        if (externalId == null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null) {
                ExternalStorage<PatientEducationDto> externalStorage = storageResolver.resolve(PatientEducationDto.class);
                externalId = externalStorage.create(dto);
            }
        }
        entity.setExternalId(externalId);

        return mapToDto(repository.save(entity));
    }
    
    private void validateMandatoryFields(PatientEducationDto dto) {
        StringBuilder errors = new StringBuilder();
        
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            errors.append("title, ");
        }
        if (dto.getCategory() == null || dto.getCategory().trim().isEmpty()) {
            errors.append("category, ");
        }
        if (dto.getLanguage() == null || dto.getLanguage().trim().isEmpty()) {
            errors.append("language, ");
        }
        if (dto.getReadingLevel() == null || dto.getReadingLevel().trim().isEmpty()) {
            errors.append("readingLevel, ");
        }
        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            errors.append("content, ");
        }
        
        if (errors.length() > 0) {
            // Remove trailing comma and space
            String missingFields = errors.substring(0, errors.length() - 2);
            throw new IllegalArgumentException("Missing mandatory fields: " + missingFields);
        }
    }

    @Transactional(readOnly = true)
    public PatientEducationDto getById(Long id) {
        PatientEducation entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("PatientEducation not found"));
        return mapToDto(entity);
    }

    @Transactional
    public PatientEducationDto update(Long id, PatientEducationDto dto) {
        PatientEducation entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("PatientEducation not found"));

        entity.setTitle(dto.getTitle());
        entity.setSummary(dto.getSummary());
        entity.setCategory(dto.getCategory());
        entity.setLanguage(dto.getLanguage());
        entity.setReadingLevel(dto.getReadingLevel());
        entity.setContent(dto.getContent());
        if (dto.getFhirId() != null) {
            entity.setExternalId(dto.getFhirId());
        }


        return mapToDto(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        PatientEducation entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient education not found with id: " + id));
        repository.delete(entity);
    }

    @Transactional(readOnly = true)
    public Page<PatientEducationDto> getAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(this::mapToDto);
    }

    private PatientEducation mapToEntity(PatientEducationDto dto) {
        return PatientEducation.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .summary(dto.getSummary())
                .category(dto.getCategory())
                .language(dto.getLanguage())
                .readingLevel(dto.getReadingLevel())
                .content(dto.getContent())
                .externalId(dto.getFhirId())
                .build();
    }

    private PatientEducationDto mapToDto(PatientEducation entity) {
        PatientEducationDto dto = new PatientEducationDto();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setSummary(entity.getSummary());
        dto.setCategory(entity.getCategory());
        dto.setLanguage(entity.getLanguage());
        dto.setReadingLevel(entity.getReadingLevel());
        dto.setContent(entity.getContent());
        dto.setFhirId(entity.getExternalId());

        PatientEducationDto.Audit audit = new PatientEducationDto.Audit();
        if (entity.getCreatedDate() != null) {
            audit.setCreatedDate(entity.getCreatedDate().format(DATE_FORMATTER));
        }
        if (entity.getLastModifiedDate() != null) {
            audit.setLastModifiedDate(entity.getLastModifiedDate().format(DATE_FORMATTER));
        }
        dto.setAudit(audit);

        return dto;
    }
}
