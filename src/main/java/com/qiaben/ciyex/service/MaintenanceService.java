package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.MaintenanceDto;
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
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            ExternalStorage<MaintenanceDto> externalStorage = storageResolver.resolve(MaintenanceDto.class);
            externalId = externalStorage.create(dto);
        }
        maintenance.setExternalId(externalId);

        return mapToDto(repository.save(maintenance));
    }

    @Transactional(readOnly = true)
    public MaintenanceDto getById(Long id) {
        Maintenance maintenance = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance not found"));
        return mapToDto(maintenance);
    }

    @Transactional
    public MaintenanceDto update(Long id, MaintenanceDto dto) {
        Maintenance maintenance = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance not found"));

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
                .orElseThrow(() -> new RuntimeException("Maintenance task not found"));
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
        dto.setFhirId(maintenance.getExternalId());
        return dto;
    }
}
