package com.qiaben.ciyex.service.core;

import com.qiaben.ciyex.dto.core.OrgDto;
import com.qiaben.ciyex.entity.Org;
import com.qiaben.ciyex.repository.OrgRepository;
import com.qiaben.ciyex.storage.ExternalOrgStorage;
import com.qiaben.ciyex.storage.ExternalOrgStorageResolver;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrgService {

    private final OrgRepository repository;
    private final ExternalOrgStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;

    @Autowired
    public OrgService(
            OrgRepository repository,
            ExternalOrgStorageResolver storageResolver,
            OrgIntegrationConfigProvider configProvider) {
        this.repository = repository;
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
    }

    @Transactional
    public OrgDto create(OrgDto dto) {
        // Save to ciyex database (master)
        Org org = mapToEntity(dto);
        org = repository.save(org);

        // Sync with external storage if configured
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            ExternalOrgStorage externalStorage = storageResolver.resolve();
            String externalId = externalStorage.create(org);
            org.setFhirId(externalId); // Consider renaming to externalId
            org = repository.save(org);
        }

        return mapToDto(org);
    }

    @Transactional(readOnly = true)
    public OrgDto getById(Long id) {
        Org org = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Org not found with id: " + id));
        // Optional: Sync from external storage if configured and externalId exists
        if (org.getFhirId() != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null) {
                ExternalOrgStorage externalStorage = storageResolver.resolve();
                Org synced = externalStorage.get(org.getFhirId());
                // Merge synced data if needed (e.g., update local fields)
                updateEntityFromSynced(org, synced);
                repository.save(org);
            }
        }
        return mapToDto(org);
    }

    @Transactional
    public OrgDto update(Long id, OrgDto dto) {
        Org org = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Org not found with id: " + id));
        updateEntityFromDto(org, dto);
        org = repository.save(org);

        // Sync with external storage if configured
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && org.getFhirId() != null) {
            ExternalOrgStorage externalStorage = storageResolver.resolve();
            externalStorage.update(org, org.getFhirId());
        } else if (storageType != null) {
            // Create new external record if none exists
            ExternalOrgStorage externalStorage = storageResolver.resolve();
            String externalId = externalStorage.create(org);
            org.setFhirId(externalId);
            org = repository.save(org);
        }

        return mapToDto(org);
    }

    @Transactional
    public void delete(Long id) {
        Org org = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Org not found with id: " + id));

        // Delete from external storage if configured
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && org.getFhirId() != null) {
            ExternalOrgStorage externalStorage = storageResolver.resolve();
            externalStorage.delete(org.getFhirId());
        }

        // Delete from ciyex database
        repository.delete(org);
    }

    private Org mapToEntity(OrgDto dto) {
        return Org.builder()
                .orgName(dto.getOrgName())
                .address(dto.getAddress())
                .city(dto.getCity())
                .state(dto.getState())
                .postalCode(dto.getPostalCode())
                .country(dto.getCountry())
                .build();
    }

    private OrgDto mapToDto(Org org) {
        OrgDto dto = new OrgDto();
        dto.setId(org.getId());
        dto.setOrgName(org.getOrgName());
        dto.setAddress(org.getAddress());
        dto.setCity(org.getCity());
        dto.setState(org.getState());
        dto.setPostalCode(org.getPostalCode());
        dto.setCountry(org.getCountry());
        dto.setFhirId(org.getFhirId());
        return dto;
    }

    private void updateEntityFromDto(Org org, OrgDto dto) {
        if (dto.getOrgName() != null) org.setOrgName(dto.getOrgName());
        if (dto.getAddress() != null) org.setAddress(dto.getAddress());
        if (dto.getCity() != null) org.setCity(dto.getCity());
        if (dto.getState() != null) org.setState(dto.getState());
        if (dto.getPostalCode() != null) org.setPostalCode(dto.getPostalCode());
        if (dto.getCountry() != null) org.setCountry(dto.getCountry());
    }

    private void updateEntityFromSynced(Org org, Org synced) {
        // Merge fields from synced external data if needed
        if (synced.getOrgName() != null) org.setOrgName(synced.getOrgName());
        if (synced.getAddress() != null) org.setAddress(synced.getAddress());
        if (synced.getCity() != null) org.setCity(synced.getCity());
        if (synced.getState() != null) org.setState(synced.getState());
        if (synced.getPostalCode() != null) org.setPostalCode(synced.getPostalCode());
        if (synced.getCountry() != null) org.setCountry(synced.getCountry());
    }
}