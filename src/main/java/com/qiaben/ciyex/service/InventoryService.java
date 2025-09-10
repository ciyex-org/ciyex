package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.InventoryDto;
import com.qiaben.ciyex.entity.Inventory;
import com.qiaben.ciyex.repository.InventoryRepository;
import com.qiaben.ciyex.storage.ExternalInventoryStorage;
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
public class InventoryService {

    private final InventoryRepository repository;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;

    public InventoryService(InventoryRepository repository,
                            ExternalStorageResolver storageResolver,
                            OrgIntegrationConfigProvider configProvider) {
        this.repository = repository;
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
    }

    @Transactional
    public InventoryDto create(InventoryDto dto) {
        Long orgId = getCurrentOrgId();
        dto.setOrgId(orgId);

        Inventory entity = mapToEntity(dto);
        entity.setCreatedDate(now());
        entity.setLastModifiedDate(now());

        // External sync (optional, based on org configuration)
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            ExternalStorage<InventoryDto> storage = storageResolver.resolve(InventoryDto.class);
            String externalId = storage.create(dto);
            entity.setExternalId(externalId);
        }

        return mapToDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public InventoryDto getById(Long id) {
        Inventory entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory item not found"));
        return mapToDto(entity);
    }

    @Transactional
    public InventoryDto update(Long id, InventoryDto dto) {
        Inventory entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory item not found"));

        entity.setName(dto.getName());
        entity.setCategory(dto.getCategory());
        entity.setLot(dto.getLot());
        entity.setExpiry(dto.getExpiry());
        entity.setSku(dto.getSku());
        entity.setStock(dto.getStock());
        entity.setUnit(dto.getUnit());
        entity.setMinStock(dto.getMinStock());
        entity.setLocation(dto.getLocation());
        entity.setStatus(dto.getStatus());
        entity.setLastModifiedDate(now());

        // External sync (if configured & item linked)
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && entity.getExternalId() != null) {
            ExternalStorage<InventoryDto> storage = storageResolver.resolve(InventoryDto.class);
            storage.update(mapToDto(entity), entity.getExternalId());
        }

        return mapToDto(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        Inventory entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory item not found"));

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && entity.getExternalId() != null) {
            ExternalStorage<InventoryDto> storage = storageResolver.resolve(InventoryDto.class);
            storage.delete(entity.getExternalId());
        }

        repository.delete(entity);
    }

    @Transactional(readOnly = true)
    public List<InventoryDto> getAll() {
        return repository.findByOrgId(getCurrentOrgId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<InventoryDto> getAll(Pageable pageable) {
        Long orgId = getCurrentOrgId();
        return repository.findAllByOrgId(orgId, pageable).map(this::mapToDto);
    }

    private Inventory mapToEntity(InventoryDto dto) {
        return Inventory.builder()
                .id(dto.getId())
                .orgId(dto.getOrgId())
                .name(dto.getName())
                .category(dto.getCategory())
                .lot(dto.getLot())
                .expiry(dto.getExpiry())
                .sku(dto.getSku())
                .stock(dto.getStock())
                .unit(dto.getUnit())
                .minStock(dto.getMinStock())
                .location(dto.getLocation())
                .status(dto.getStatus())
                .build();
    }

    private InventoryDto mapToDto(Inventory e) {
        InventoryDto dto = new InventoryDto();
        dto.setId(e.getId());
        dto.setOrgId(e.getOrgId());
        dto.setName(e.getName());
        dto.setCategory(e.getCategory());
        dto.setLot(e.getLot());
        dto.setExpiry(e.getExpiry());
        dto.setSku(e.getSku());
        dto.setStock(e.getStock());
        dto.setUnit(e.getUnit());
        dto.setMinStock(e.getMinStock());
        dto.setLocation(e.getLocation());
        dto.setStatus(e.getStatus());
        dto.setFhirId(e.getExternalId());

        InventoryDto.Audit audit = new InventoryDto.Audit();
        audit.setCreatedDate(e.getCreatedDate());
        audit.setLastModifiedDate(e.getLastModifiedDate());
        dto.setAudit(audit);

        return dto;
    }

    private Long getCurrentOrgId() {
        return RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
    }

    private String now() {
        return LocalDateTime.now().toString();
    }
}
