package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.MaintenanceDto;
import com.qiaben.ciyex.exception.ResourceNotFoundException;
import com.qiaben.ciyex.entity.Maintenance;
import com.qiaben.ciyex.repository.MaintenanceRepository;
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
public class MaintenanceService {

    private final MaintenanceRepository repository;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;

    public MaintenanceService(MaintenanceRepository repository,
                              ExternalStorageResolver storageResolver,
                              OrgIntegrationConfigProvider configProvider) {
        this.repository = repository;
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
    }

    @Transactional
    public MaintenanceDto create(MaintenanceDto dto) {
        Maintenance maintenance = mapToEntity(dto);
        String externalId = null;

        log.debug("Incoming DTO externalId={}", dto.getExternalId());

        try {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            log.info("Storage type for current org: {}", storageType);

            if (storageType != null && !storageType.isBlank()) {
                ExternalStorage<MaintenanceDto> externalStorage = storageResolver.resolve(MaintenanceDto.class);
                if (externalStorage != null) {
                    externalId = externalStorage.create(dto);
                    log.info("Generated external ID from external storage: {}", externalId);
                } else {
                    log.warn("No external storage found for MaintenanceDto");
                }
            } else {
                log.info("No storage type configured, skipping external storage");
            }
        } catch (Exception e) {
            log.error("Error creating external storage record", e);
        }

        // Set externalId priority: 1) External storage, 2) Client provided, 3) Auto-generate
        if (externalId != null) {
            maintenance.setExternalId(externalId);
        } else if (dto.getExternalId() != null && !dto.getExternalId().isBlank()) {
            maintenance.setExternalId(dto.getExternalId());
        } else {
            // Auto-generate if no external storage and no client-provided ID
            maintenance.setExternalId("MT-" + java.util.UUID.randomUUID().toString());
            log.info("Auto-generated external ID: {}", maintenance.getExternalId());
        }

        log.debug("Entity before save externalId={}, createdDate={}, lastModifiedDate={}", maintenance.getExternalId(), maintenance.getCreatedDate(), maintenance.getLastModifiedDate());

        Maintenance saved = repository.save(maintenance);

        log.info("Saved maintenance with ID: {} and external ID: {}", saved.getId(), saved.getExternalId());
        log.debug("Saved entity audit: createdDate={}, lastModifiedDate={}", saved.getCreatedDate(), saved.getLastModifiedDate());

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public MaintenanceDto getById(Long id) {
        Maintenance maintenance = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance", "id", (Object) id));
        return mapToDto(maintenance);
    }

    @Transactional
    public MaintenanceDto update(Long id, MaintenanceDto dto) {
        Maintenance maintenance = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance", "id", (Object) id));

        maintenance.setEquipment(dto.getEquipment());
        maintenance.setCategory(dto.getCategory());
        maintenance.setLocation(dto.getLocation());
        maintenance.setDueDate(dto.getDueDate());
        maintenance.setLastServiceDate(dto.getLastServiceDate());
        maintenance.setAssignee(dto.getAssignee());
        maintenance.setVendor(dto.getVendor());
        maintenance.setPriority(dto.getPriority());
        maintenance.setStatus(dto.getStatus());
        maintenance.setNotes(dto.getNotes());

        return mapToDto(repository.save(maintenance));
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceDto> getAll() {
        return repository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<MaintenanceDto> getAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(this::mapToDto);
    }

    @Transactional
    public MaintenanceDto updateStatus(Long id, String status) {
        Maintenance entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance", "id", (Object) id));
        entity.setStatus(status);
        return mapToDto(repository.save(entity));
    }


    private Maintenance mapToEntity(MaintenanceDto dto) {
        return Maintenance.builder()
                .id(dto.getId())
                .equipment(dto.getEquipment())
                .category(dto.getCategory())
                .location(dto.getLocation())
                .dueDate(dto.getDueDate())
                .lastServiceDate(dto.getLastServiceDate())
                .assignee(dto.getAssignee())
                .vendor(dto.getVendor())
                .priority(dto.getPriority())
                .status(dto.getStatus())
                .notes(dto.getNotes())
                .externalId(dto.getExternalId())
                .build();
    }

    private MaintenanceDto mapToDto(Maintenance maintenance) {
        MaintenanceDto dto = new MaintenanceDto();
        dto.setId(maintenance.getId());
        dto.setEquipment(maintenance.getEquipment());
        dto.setCategory(maintenance.getCategory());
        dto.setLocation(maintenance.getLocation());
        dto.setDueDate(maintenance.getDueDate());
        dto.setLastServiceDate(maintenance.getLastServiceDate());
        dto.setAssignee(maintenance.getAssignee());
        dto.setVendor(maintenance.getVendor());
        dto.setPriority(maintenance.getPriority());
        dto.setStatus(maintenance.getStatus());
        dto.setNotes(maintenance.getNotes());
        dto.setExternalId(maintenance.getExternalId());
        dto.setFhirId(maintenance.getExternalId());
        // Map audit information
        MaintenanceDto.Audit audit = new MaintenanceDto.Audit();
        audit.setCreatedDate(maintenance.getCreatedDate()!=null ? maintenance.getCreatedDate().toString() : null);
        audit.setLastModifiedDate(maintenance.getLastModifiedDate()!=null ? maintenance.getLastModifiedDate().toString() : null);
        dto.setAudit(audit);
        return dto;
    }
}
