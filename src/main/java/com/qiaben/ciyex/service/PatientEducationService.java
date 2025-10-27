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

@Service
@Slf4j
public class PatientEducationService {

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
        PatientEducation entity = mapToEntity(dto);


        String externalId = null;
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            ExternalStorage<PatientEducationDto> externalStorage = storageResolver.resolve(PatientEducationDto.class);
            externalId = externalStorage.create(dto);
        }
        entity.setExternalId(externalId);

        return mapToDto(repository.save(entity));
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


        return mapToDto(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
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

        return dto;
    }
}
