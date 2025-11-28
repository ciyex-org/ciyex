package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.SupplierDto;
import com.qiaben.ciyex.exception.ResourceNotFoundException;
import com.qiaben.ciyex.entity.Supplier;
import com.qiaben.ciyex.repository.SupplierRepository;
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
public class SupplierService {

    private final SupplierRepository repository;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;

    public SupplierService(SupplierRepository repository,
                           ExternalStorageResolver storageResolver,
                           OrgIntegrationConfigProvider configProvider) {
        this.repository = repository;
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
    }

    @Transactional
    public SupplierDto create(SupplierDto dto) {
        Supplier supplier = mapToEntity(dto);

        String externalId = null;
        try {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null && !storageType.isBlank()) {
                ExternalStorage<SupplierDto> externalStorage = storageResolver.resolve(SupplierDto.class);
                if (externalStorage != null) {
                    externalId = externalStorage.create(dto);
                }
            }
        } catch (Exception e) {
            log.error("Error creating external storage record for Supplier", e);
        }

        // Set externalId priority: 1) External storage, 2) Client provided, 3) Auto-generate
        if (externalId != null) {
            supplier.setExternalId(externalId);
        } else if (dto.getExternalId() != null && !dto.getExternalId().isBlank()) {
            supplier.setExternalId(dto.getExternalId());
        } else {
            // Auto-generate if no external storage and no client-provided ID
            supplier.setExternalId("SP-" + java.util.UUID.randomUUID().toString());
            log.info("Auto-generated external ID for supplier: {}", supplier.getExternalId());
        }

        return mapToDto(repository.save(supplier));
    }

    @Transactional(readOnly = true)
    public SupplierDto getById(Long id) {
        Supplier supplier = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", (Object) id));
        return mapToDto(supplier);
    }

    @Transactional
    public SupplierDto update(Long id, SupplierDto dto) {
        Supplier supplier = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", (Object) id));

        supplier.setName(dto.getName());
        supplier.setContact(dto.getContact());
        supplier.setPhone(dto.getPhone());
        supplier.setEmail(dto.getEmail());


        return mapToDto(repository.save(supplier));
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<SupplierDto> getAll() {
        return repository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<SupplierDto> getAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Long countByOrg() {
        return repository.count();
    }


    private Supplier mapToEntity(SupplierDto dto) {
        return Supplier.builder()
                .id(dto.getId())
                .name(dto.getName())
                .contact(dto.getContact())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .externalId(dto.getExternalId())
                .build();
    }

    private SupplierDto mapToDto(Supplier supplier) {
        SupplierDto dto = new SupplierDto();
        dto.setId(supplier.getId());
        dto.setName(supplier.getName());
        dto.setContact(supplier.getContact());
        dto.setPhone(supplier.getPhone());
        dto.setEmail(supplier.getEmail());
        dto.setExternalId(supplier.getExternalId());
        dto.setFhirId(supplier.getExternalId());

        SupplierDto.Audit audit = new SupplierDto.Audit();
        audit.setCreatedDate(supplier.getCreatedDate() != null ? supplier.getCreatedDate().toString() : null);
        audit.setLastModifiedDate(supplier.getLastModifiedDate() != null ? supplier.getLastModifiedDate().toString() : null);
        dto.setAudit(audit);
        return dto;
    }
}
