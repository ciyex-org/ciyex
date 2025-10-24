package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.SupplierDto;
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
        supplier.setCreatedDate(LocalDateTime.now().toString());
        supplier.setLastModifiedDate(LocalDateTime.now().toString());

        String externalId = null;
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            ExternalStorage<SupplierDto> externalStorage = storageResolver.resolve(SupplierDto.class);
            externalId = externalStorage.create(dto);
        }
        supplier.setExternalId(externalId);

        return mapToDto(repository.save(supplier));
    }

    @Transactional(readOnly = true)
    public SupplierDto getById(Long id) {
        Supplier supplier = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));
        return mapToDto(supplier);
    }

    @Transactional
    public SupplierDto update(Long id, SupplierDto dto) {
        Supplier supplier = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        supplier.setName(dto.getName());
        supplier.setContact(dto.getContact());
        supplier.setPhone(dto.getPhone());
        supplier.setEmail(dto.getEmail());
        supplier.setLastModifiedDate(LocalDateTime.now().toString());

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
                .createdDate(dto.getCreatedDate())
                .lastModifiedDate(dto.getLastModifiedDate())
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
        dto.setCreatedDate(supplier.getCreatedDate());
        dto.setLastModifiedDate(supplier.getLastModifiedDate());
        dto.setExternalId(supplier.getExternalId());
        return dto;
    }
}
