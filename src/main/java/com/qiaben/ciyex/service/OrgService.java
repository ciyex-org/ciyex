package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.OrgDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.entity.Org;
import com.qiaben.ciyex.repository.OrgRepository;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrgService {

    private final OrgRepository repository;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;
    private final TenantSchemaInitializer tenantSchemaInitializer;

    @Autowired
    public OrgService(
            OrgRepository repository,
            ExternalStorageResolver storageResolver,
            OrgIntegrationConfigProvider configProvider,
            TenantSchemaInitializer tenantSchemaInitializer) {
        this.repository = repository;
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
        this.tenantSchemaInitializer = tenantSchemaInitializer;
    }

    @Transactional
    public OrgDto create(OrgDto dto) {
        String externalId = null;
        Org org = mapToEntity(dto);

        // Check if external storage is configured
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            try {
                ExternalStorage<OrgDto> externalStorage = storageResolver.resolve(OrgDto.class);
                externalId = externalStorage.create(dto);
                log.info("Successfully created org in external storage with externalId: {} for orgId: {}", externalId, getCurrentOrgId());
            } catch (Exception e) {
                log.error("Failed to create org in external storage for orgId: {}, error: {}", getCurrentOrgId(), e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e); // This will roll back the transaction
            }
        }

        // Save to database only if external storage succeeded or not configured
        org = repository.save(org);
        if (externalId != null) {
            org.setFhirId(externalId);
            org = repository.save(org); // Update with externalId
            log.info("Created org with id: {} and externalId: {} in DB for orgId: {}", org.getId(), externalId, getCurrentOrgId());
        } else {
            log.info("Created org with id: {} in DB for orgId: {} without external storage", org.getId(), getCurrentOrgId());
        }

        // Initialize tenant schema for the new organization
        try {
            tenantSchemaInitializer.initializeTenantSchema(org.getId());
            log.info("Successfully initialized tenant schema for new org with id: {}", org.getId());
        } catch (Exception e) {
            log.error("Failed to initialize tenant schema for org with id: {}, error: {}", org.getId(), e.getMessage());
            // Note: We don't throw here to avoid rolling back the org creation
            // The tenant schema can be initialized later if needed
        }

        return mapToDto(org);
    }

    @Transactional
    public OrgDto getById(Long id) {
        // Authorization check: Ensure the requested id belongs to the current orgId
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            throw new SecurityException("No orgId available in request context");
        }
        // Assuming the id implicitly belongs to the current orgId (adjust if orgId is a field in Org)
        log.debug("Verifying access for orgId: {} to org with id: {}", currentOrgId, id);
        // If orgId is a field in Org, uncomment and adjust the following:
        // Org org = repository.findById(id).orElseThrow(() -> new RuntimeException("Org not found with id: " + id));

        Org org = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Org not found with id: " + id));
        log.info("Found org in DB with id: {} for orgId: {}", id, currentOrgId);
        if (!currentOrgId.equals(org.getId())) {
            throw new SecurityException("Access denied: Org id " + id + " does not belong to orgId " + currentOrgId);
        }

        // Always load from external storage if fhirId exists and storage is configured
        if (org.getFhirId() != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null) {
                try {
                    ExternalStorage<OrgDto> externalStorage = storageResolver.resolve(OrgDto.class);
                    OrgDto synced = externalStorage.get(org.getFhirId());
                    if (synced != null) {
                        log.info("Loaded org with id: {} from external storage for orgId: {}", id, currentOrgId);
                        // Update database with external data if different
                        if (!isSameOrg(org, synced)) {
                            updateEntityFromSynced(org, synced);
                            org = repository.save(org); // Save changes within the transaction
                            log.info("Updated DB with external storage data for org id: {}", id);
                        }
                        return mapToDto(org); // Return updated org
                    } else {
                        log.warn("No data found in external storage for org id: {} with externalId: {}", id, org.getFhirId());
                    }
                } catch (Exception e) {
                    log.error("Failed to load org from external storage for org id: {}, error: {}", id, e.getMessage());
                    // Fall back to database data
                }
            }
        }

        // Return database data if no external sync or external fetch failed
        return mapToDto(org);
    }

    @Transactional
    public OrgDto update(Long id, OrgDto dto) {
        // Authorization check: Ensure the requested id belongs to the current orgId
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            throw new SecurityException("No orgId available in request context");
        }
        log.debug("Verifying access for orgId: {} to org with id: {}", currentOrgId, id);
        // Assuming the id implicitly belongs to the current orgId (adjust if orgId is a field in Org)
        // If orgId is a field in Org, uncomment and adjust:
        // Org org = repository.findById(id).orElseThrow(() -> new RuntimeException("Org not found with id: " + id));

        Org org = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Org not found with id: " + id));
        if (!currentOrgId.equals(org.getId())) {
            throw new SecurityException("Access denied: Org id " + id + " does not belong to orgId " + currentOrgId);
        }
        updateEntityFromDto(org, dto);

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            ExternalStorage<OrgDto> externalStorage = storageResolver.resolve(OrgDto.class);
            if (org.getFhirId() != null) {
                try {
                    externalStorage.update(dto, org.getFhirId());
                    log.info("Successfully updated org with id: {} and externalId: {} in external storage for orgId: {}", org.getId(), org.getFhirId(), currentOrgId);
                } catch (Exception e) {
                    log.error("Failed to update org in external storage for orgId: {}, error: {}", currentOrgId, e.getMessage());
                    throw new RuntimeException("Failed to sync with external storage", e); // Rollback transaction
                }
            } else {
                try {
                    String externalId = externalStorage.create(dto);
                    org.setFhirId(externalId);
                    log.info("Created new external record with id: {} and externalId: {} for orgId: {}", org.getId(), externalId, currentOrgId);
                } catch (Exception e) {
                    log.error("Failed to create org in external storage for orgId: {}, error: {}", currentOrgId, e.getMessage());
                    throw new RuntimeException("Failed to sync with external storage", e); // Rollback transaction
                }
            }
        }

        // Save to database only if external storage succeeded
        org = repository.save(org);
        return mapToDto(org);
    }

    /**
     * Update only the status of an organization.
     * Accepts status values: "ACTIVE" or "INACTIVE".
     */
    @Transactional
    public OrgDto updateStatus(Long id, String status) {
        Long currentOrgId = getCurrentOrgId();
        String currentRole = RequestContext.get() != null ? RequestContext.get().getRole() : null;

        // If no orgId provided in context, only allow SUPER_ADMIN to proceed
        if (currentOrgId == null && (currentRole == null || !"SUPER_ADMIN".equalsIgnoreCase(currentRole))) {
            throw new SecurityException("No orgId available in request context");
        }

        Org org = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Org not found with id: " + id));

        // If caller is not SUPER_ADMIN, enforce that the org belongs to the caller's orgId
        if (currentRole == null || !"SUPER_ADMIN".equalsIgnoreCase(currentRole)) {
            if (!currentOrgId.equals(org.getId())) {
                throw new SecurityException("Access denied: Org id " + id + " does not belong to orgId " + currentOrgId);
            }
        }

        // Validate and set status
        Org.OrgStatus newStatus;
        try {
            newStatus = Org.OrgStatus.valueOf(status.trim().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid status value: " + status);
        }
        org.setStatus(newStatus);

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            ExternalStorage<OrgDto> externalStorage = storageResolver.resolve(OrgDto.class);
            OrgDto dto = mapToDto(org);
            try {
                if (org.getFhirId() != null) {
                    externalStorage.update(dto, org.getFhirId());
                    log.info("Updated external storage with status for org id: {} and externalId: {}", org.getId(), org.getFhirId());
                } else {
                    // Create external record if missing
                    String externalId = externalStorage.create(dto);
                    org.setFhirId(externalId);
                    log.info("Created external record for org id: {} with externalId: {}", org.getId(), externalId);
                }
            } catch (Exception e) {
                log.error("Failed to sync status change to external storage for orgId: {}, error: {}", currentOrgId, e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e);
            }
        }

        org = repository.save(org);
        return mapToDto(org);
    }

    @Transactional
    public void delete(Long id) {
        // Authorization check: Ensure the requested id belongs to the current orgId
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            throw new SecurityException("No orgId available in request context");
        }
        log.debug("Verifying access for orgId: {} to org with id: {}", currentOrgId, id);

        Org org = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Org not found with id: " + id));
        if (!currentOrgId.equals(org.getId())) {
            throw new SecurityException("Access denied: Org id " + id + " does not belong to orgId " + currentOrgId);
        }

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && org.getFhirId() != null) {
            try {
                ExternalStorage<OrgDto> externalStorage = storageResolver.resolve(OrgDto.class);
                externalStorage.delete(org.getFhirId());
                log.info("Successfully deleted org with id: {} and externalId: {} from external storage for orgId: {}", org.getId(), org.getFhirId(), currentOrgId);
            } catch (Exception e) {
                log.error("Failed to delete org from external storage for orgId: {}, error: {}", currentOrgId, e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e); // Rollback transaction
            }
        }

        // Clean up tenant schema before deleting the organization
        try {
            tenantSchemaInitializer.dropTenantSchema(org.getId());
            log.info("Successfully dropped tenant schema for org with id: {}", org.getId());
        } catch (Exception e) {
            log.warn("Failed to drop tenant schema for org with id: {}, error: {}", org.getId(), e.getMessage());
            // Continue with org deletion even if schema cleanup fails
        }

        // Delete from ciyex database only if external storage succeeded
        repository.delete(org);
        log.info("Deleted org with id: {} from DB for orgId: {}", id, currentOrgId);
    }

    @Transactional(readOnly = true)
    public List<OrgDto> getAll() {
        Long currentOrgId = getCurrentOrgId();
        String currentRole = RequestContext.get() != null ? RequestContext.get().getRole() : null;

        if ("SUPER_ADMIN".equalsIgnoreCase(currentRole)) {
            log.info("SUPER_ADMIN detected, returning all organizations");
            return repository.findAll()
                    .stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
        }

        if (currentOrgId == null) {
            log.info("No orgId in context, returning all orgs (signup/public view)");
            return repository.findAll()
                    .stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
        }

        // Normal user → restrict to their org only
        return repository.findAll().stream()
                .filter(org -> currentOrgId.equals(org.getId()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }







    private Org mapToEntity(OrgDto dto) {
        Org.OrgBuilder builder = Org.builder()
                .orgName(dto.getOrgName())
                .address(dto.getAddress())
                .city(dto.getCity())
                .state(dto.getState())
                .postalCode(dto.getPostalCode())
                .country(dto.getCountry());

        // Only set status on the builder if provided in DTO. If not provided, Lombok's @Builder.Default
        // on the entity will apply and default to ACTIVE.
        if (dto.getStatus() != null) {
            try {
                builder.status(Org.OrgStatus.valueOf(dto.getStatus()));
            } catch (IllegalArgumentException ignored) {
                // Ignore invalid values - let entity default apply
            }
        }

        return builder.build();
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
        dto.setStatus(org.getStatus() != null ? org.getStatus().name() : null);
        return dto;
    }

    private void updateEntityFromDto(Org org, OrgDto dto) {
        if (dto.getOrgName() != null) org.setOrgName(dto.getOrgName());
        if (dto.getAddress() != null) org.setAddress(dto.getAddress());
        if (dto.getCity() != null) org.setCity(dto.getCity());
        if (dto.getState() != null) org.setState(dto.getState());
        if (dto.getPostalCode() != null) org.setPostalCode(dto.getPostalCode());
        if (dto.getCountry() != null) org.setCountry(dto.getCountry());
        if (dto.getStatus() != null) {
            try {
                org.setStatus(Org.OrgStatus.valueOf(dto.getStatus()));
            } catch (IllegalArgumentException e) {
                // ignore invalid status values or could throw a validation exception
            }
        }
    }

    private void updateEntityFromSynced(Org org, OrgDto synced) {
        if (synced.getOrgName() != null) org.setOrgName(synced.getOrgName());
        if (synced.getAddress() != null) org.setAddress(synced.getAddress());
        if (synced.getCity() != null) org.setCity(synced.getCity());
        if (synced.getState() != null) org.setState(synced.getState());
        if (synced.getPostalCode() != null) org.setPostalCode(synced.getPostalCode());
        if (synced.getCountry() != null) org.setCountry(synced.getCountry());
        if (synced.getFhirId() != null) org.setFhirId(synced.getFhirId()); // Update fhirId if changed externally
    }

    private boolean isSameOrg(Org org, OrgDto synced) {
        return org.getOrgName() == null ? synced.getOrgName() == null : org.getOrgName().equals(synced.getOrgName()) &&
                org.getAddress() == null ? synced.getAddress() == null : org.getAddress().equals(synced.getAddress()) &&
                org.getCity() == null ? synced.getCity() == null : org.getCity().equals(synced.getCity()) &&
                org.getState() == null ? synced.getState() == null : org.getState().equals(synced.getState()) &&
                org.getPostalCode() == null ? synced.getPostalCode() == null : org.getPostalCode().equals(synced.getPostalCode()) &&
                org.getCountry() == null ? synced.getCountry() == null : org.getCountry().equals(synced.getCountry());
    }

    private Long getCurrentOrgId() {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        if (orgId == null) {
            log.warn("orgId is null in RequestContext");
        }
        return orgId;
    }
}