package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.RecallDto;
import com.qiaben.ciyex.entity.Recall;
import com.qiaben.ciyex.repository.RecallRepository;
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
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RecallService {

    private final RecallRepository repository;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;

    public RecallService(RecallRepository repository,
                         ExternalStorageResolver storageResolver,
                         OrgIntegrationConfigProvider configProvider) {
        this.repository = repository;
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
    }

    @Transactional
    public RecallDto create(RecallDto dto) {
        Long orgId = getCurrentOrgId();
        dto.setOrgId(orgId);

        Recall recall = mapToEntity(dto);
        recall.setCreatedDate(LocalDateTime.now().toString());
        recall.setLastModifiedDate(LocalDateTime.now().toString());

        String externalId = null;
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            ExternalStorage<RecallDto> externalStorage = storageResolver.resolve(RecallDto.class);
            externalId = externalStorage.create(dto);
        }
        recall.setExternalId(externalId);

        return mapToDto(repository.save(recall));
    }

    @Transactional(readOnly = true)
    public RecallDto getById(Long id) {
        Recall recall = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recall not found"));
        return mapToDto(recall);
    }

    @Transactional
    public RecallDto update(Long id, RecallDto dto) {
        Recall recall = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recall not found"));

        recall.setRecallDate(dto.getRecallDate());
        recall.setRecallReason(dto.getRecallReason());
        recall.setSmsConsent(dto.isSmsConsent());
        recall.setEmailConsent(dto.isEmailConsent());
        recall.setPhone(dto.getPhone());
        recall.setEmail(dto.getEmail());
        recall.setAddress(dto.getAddress());
        recall.setCity(dto.getCity());
        recall.setState(dto.getState());
        recall.setZipCode(dto.getZip());
        recall.setLastModifiedDate(LocalDateTime.now().toString());

        return mapToDto(repository.save(recall));
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<RecallDto> getAll() {
        return repository.findByOrgId(getCurrentOrgId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<RecallDto> getAll(Pageable pageable) {
        Long orgId = getCurrentOrgId();
        return repository.findAllByOrgId(orgId, pageable)
                .map(this::mapToDto);
    }

    private Recall mapToEntity(RecallDto dto) {
        return Recall.builder()
                .id(dto.getId())
                .orgId(dto.getOrgId())
                .patientId(dto.getPatientId())
                .providerId(dto.getProviderId())
                .patientName(dto.getPatientName())
                .dob(dto.getDob())
                .lastVisit(dto.getLastVisit())
                .recallDate(dto.getRecallDate())
                .recallReason(dto.getRecallReason())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .address(dto.getAddress())
                .city(dto.getCity())
                .state(dto.getState())
                .zipCode(dto.getZip())
                .smsConsent(dto.isSmsConsent())
                .emailConsent(dto.isEmailConsent())
                .build();
    }

    private RecallDto mapToDto(Recall recall) {
        RecallDto dto = new RecallDto();
        dto.setId(recall.getId());
        dto.setOrgId(recall.getOrgId());
        dto.setPatientId(recall.getPatientId());
        dto.setProviderId(recall.getProviderId());
        dto.setPatientName(recall.getPatientName());
        dto.setDob(recall.getDob());
        dto.setLastVisit(recall.getLastVisit());
        dto.setRecallDate(recall.getRecallDate());
        dto.setRecallReason(recall.getRecallReason());
        dto.setPhone(recall.getPhone());
        dto.setEmail(recall.getEmail());
        dto.setAddress(recall.getAddress());
        dto.setCity(recall.getCity());
        dto.setState(recall.getState());
        dto.setZip(recall.getZipCode());
        dto.setSmsConsent(recall.isSmsConsent());
        dto.setEmailConsent(recall.isEmailConsent());
        dto.setFhirId(recall.getExternalId());
        return dto;
    }

    private Long getCurrentOrgId() {
        return RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
    }
}
