package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.ProviderDto;
import com.qiaben.ciyex.entity.Provider;
import com.qiaben.ciyex.repository.ProviderRepository;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import com.qiaben.ciyex.dto.core.integration.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProviderService {

    private final ProviderRepository repository;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;

    @Autowired
    public ProviderService(ProviderRepository repository, ExternalStorageResolver storageResolver, OrgIntegrationConfigProvider configProvider) {
        this.repository = repository;
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
    }

    @Transactional
    public ProviderDto create(ProviderDto dto) {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            log.error("No orgId found in RequestContext during create");
            throw new SecurityException("No orgId available in request context");
        }
        log.debug("Verifying access for orgId: {} to create new provider", currentOrgId);
        dto.setOrgId(currentOrgId); // Set orgId for the new provider

        if (dto.getNpi() == null || dto.getIdentification() == null || dto.getIdentification().getFirstName() == null ||
                dto.getIdentification().getLastName() == null || dto.getProfessionalDetails() == null ||
                dto.getProfessionalDetails().getLicenseNumber() == null) {
            throw new IllegalArgumentException("NPI, first name, last name, and license number are required");
        }

        Provider provider = mapToEntity(dto);
        provider.setOrgId(currentOrgId);
        provider.setCreatedDate(LocalDateTime.now().toString());
        provider.setLastModifiedDate(LocalDateTime.now().toString());
        String externalId = null;

        // Attempt external storage creation first
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            try {
                ExternalStorage<ProviderDto> externalStorage = storageResolver.resolve(ProviderDto.class);
                externalId = externalStorage.create(dto);
                log.info("Successfully created provider in external storage with externalId: {} for orgId: {}", externalId, currentOrgId);
            } catch (Exception e) {
                log.error("Failed to create provider in external storage for orgId: {}, error: {}", currentOrgId, e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e); // Rollback transaction
            }
        }

        // Save to database only if external storage succeeded or not configured
        provider.setExternalId(externalId);
        provider = repository.save(provider);
        log.info("Created provider with id: {} and externalId: {} in DB for orgId: {}", provider.getId(), externalId, currentOrgId);

        return mapToDto(provider);
    }

    @Transactional(readOnly = true)
    public ProviderDto getById(Long id) {
        log.debug("Entering getById with id: {}", id);
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            log.error("No orgId found in RequestContext during getById for id: {}", id);
            throw new SecurityException("No orgId available in request context");
        }
        log.debug("Verifying access for orgId: {} to provider with id: {}", currentOrgId, id);

        // Fetch provider from database
        Provider provider = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found with id: " + id));
        log.debug("Fetched provider from DB: id={}, externalId={}, orgId={}", provider.getId(), provider.getExternalId(), provider.getOrgId());
        if (!currentOrgId.equals(provider.getOrgId())) {
            throw new SecurityException("Access denied: Provider id " + id + " does not belong to orgId " + currentOrgId);
        }
        log.info("Found provider in DB with id: {} for orgId: {}", id, currentOrgId);

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        ProviderDto resultDto = mapToDto(provider); // Default to DB data
        if (storageType != null && provider.getExternalId() != null) {
            log.debug("Attempting to fetch extended details for provider id: {} with externalId: {} for orgId: {}", id, provider.getExternalId(), currentOrgId);
            ExternalStorage<ProviderDto> externalStorage = storageResolver.resolve(ProviderDto.class);
            try {
                ProviderDto extendedProviderDto = externalStorage.get(provider.getExternalId());
                if (extendedProviderDto != null) {
                    log.info("Successfully loaded extended details for provider id: {} from external storage for orgId: {}", id, currentOrgId);
                    log.debug("Extended ProviderDto: id={}, fhirId={}, orgId={}", extendedProviderDto.getId(), extendedProviderDto.getFhirId(), extendedProviderDto.getOrgId());
                    extendedProviderDto.setId(provider.getId()); // Preserve DB ID
                    resultDto = extendedProviderDto;
                } else {
                    log.warn("No extended details found in external storage for provider id: {} with externalId: {} for orgId: {}", id, provider.getExternalId(), currentOrgId);
                }
            } catch (Exception e) {
                log.error("Failed to fetch extended details from external storage for provider id: {} with externalId: {}, error: {}", id, provider.getExternalId(), e.getMessage());
            }
        }
        log.info("Returning provider dto for id: {} and orgId: {}", id, currentOrgId);
        log.debug("Returning ProviderDto: id={}, fhirId={}, orgId={}", resultDto.getId(), resultDto.getFhirId(), resultDto.getOrgId());
        return resultDto;
    }

    @Transactional
    public ProviderDto update(Long id, ProviderDto dto) {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            log.error("No orgId found in RequestContext during update for id: {}", id);
            throw new SecurityException("No orgId available in request context");
        }
        log.debug("Verifying access for orgId: {} to provider with id: {}", currentOrgId, id);

        Provider provider = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found with id: " + id));
        if (!currentOrgId.equals(provider.getOrgId())) {
            throw new SecurityException("Access denied: Provider id " + id + " does not belong to orgId " + currentOrgId);
        }

        provider.setNpi(dto.getNpi());
        if (dto.getIdentification() != null) {
            provider.setFirstName(dto.getIdentification().getFirstName());
            provider.setLastName(dto.getIdentification().getLastName());
            provider.setMiddleName(dto.getIdentification().getMiddleName());
            provider.setPrefix(dto.getIdentification().getPrefix());
            provider.setSuffix(dto.getIdentification().getSuffix());
            provider.setGender(dto.getIdentification().getGender());
            provider.setDateOfBirth(dto.getIdentification().getDateOfBirth());
            provider.setPhoto(dto.getIdentification().getPhoto());
        }
        if (dto.getContact() != null) {
            provider.setEmail(dto.getContact().getEmail());
            provider.setPhoneNumber(dto.getContact().getPhoneNumber());
            provider.setMobileNumber(dto.getContact().getMobileNumber());
            provider.setFaxNumber(dto.getContact().getFaxNumber());
            provider.setAddress(dto.getContact().getAddress() != null ? dto.getContact().getAddress().toString() : null);
        }
        if (dto.getProfessionalDetails() != null) {
            provider.setSpecialty(dto.getProfessionalDetails().getSpecialty());
            provider.setProviderType(dto.getProfessionalDetails().getProviderType());
            provider.setLicenseNumber(dto.getProfessionalDetails().getLicenseNumber());
            provider.setLicenseState(dto.getProfessionalDetails().getLicenseState());
            provider.setLicenseExpiry(dto.getProfessionalDetails().getLicenseExpiry());
        }
        provider.setLastModifiedDate(LocalDateTime.now().toString());

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && provider.getExternalId() != null) {
            try {
                ExternalStorage<ProviderDto> externalStorage = storageResolver.resolve(ProviderDto.class);
                externalStorage.update(dto, provider.getExternalId());
                log.info("Successfully updated provider with id: {} and externalId: {} in external storage for orgId: {}", provider.getId(), provider.getExternalId(), currentOrgId);
            } catch (Exception e) {
                log.error("Failed to update provider in external storage for orgId: {}, error: {}", currentOrgId, e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e); // Rollback transaction
            }
        }

        Provider updatedProvider = repository.save(provider);
        return mapToDto(updatedProvider);
    }

    @Transactional
    public void delete(Long id) {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            log.error("No orgId found in RequestContext during delete for id: {}", id);
            throw new SecurityException("No orgId available in request context");
        }
        log.debug("Verifying access for orgId: {} to provider with id: {}", currentOrgId, id);

        Provider provider = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found with id: " + id));
        if (!currentOrgId.equals(provider.getOrgId())) {
            throw new SecurityException("Access denied: Provider id " + id + " does not belong to orgId " + currentOrgId);
        }

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && provider.getExternalId() != null) {
            try {
                ExternalStorage<ProviderDto> externalStorage = storageResolver.resolve(ProviderDto.class);
                externalStorage.delete(provider.getExternalId());
                log.info("Successfully deleted provider with id: {} and externalId: {} from external storage for orgId: {}", provider.getId(), provider.getExternalId(), currentOrgId);
            } catch (Exception e) {
                log.error("Failed to delete provider from external storage for orgId: {}, error: {}", currentOrgId, e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e); // Rollback transaction
            }
        }

        // Delete from database only if external storage succeeded
        repository.delete(provider);
        log.info("Deleted provider with id: {} from DB for orgId: {}", id, currentOrgId);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<ProviderDto>> getAllProviders() {
        log.debug("Entering getAllProviders");
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            log.error("No orgId found in RequestContext");
            return ApiResponse.<List<ProviderDto>>builder()
                    .success(false)
                    .message("No orgId configured for provider retrieval")
                    .data(null)
                    .build();
        }
        log.debug("Verifying access for orgId: {} to retrieve all providers", currentOrgId);

        // Fetch all providers directly from the database
        List<Provider> providers = repository.findAllByOrgId(currentOrgId);
        log.info("Retrieved {} providers from DB for orgId: {}", providers.size(), currentOrgId);
        List<ProviderDto> providerDtos = providers.stream().map(this::mapToDto).collect(Collectors.toList());

        return ApiResponse.<List<ProviderDto>>builder()
                .success(true)
                .message("Providers retrieved successfully")
                .data(providerDtos)
                .build();
    }

    private Provider mapToEntity(ProviderDto dto) {
        Provider provider = new Provider();
        provider.setNpi(dto.getNpi());
        if (dto.getIdentification() != null) {
            provider.setFirstName(dto.getIdentification().getFirstName());
            provider.setLastName(dto.getIdentification().getLastName());
            provider.setMiddleName(dto.getIdentification().getMiddleName());
            provider.setPrefix(dto.getIdentification().getPrefix());
            provider.setSuffix(dto.getIdentification().getSuffix());
            provider.setGender(dto.getIdentification().getGender());
            provider.setDateOfBirth(dto.getIdentification().getDateOfBirth());
            provider.setPhoto(dto.getIdentification().getPhoto());
        }
        if (dto.getContact() != null) {
            provider.setEmail(dto.getContact().getEmail());
            provider.setPhoneNumber(dto.getContact().getPhoneNumber());
            provider.setMobileNumber(dto.getContact().getMobileNumber());
            provider.setFaxNumber(dto.getContact().getFaxNumber());
            provider.setAddress(dto.getContact().getAddress() != null ? dto.getContact().getAddress().toString() : null);
        }
        if (dto.getProfessionalDetails() != null) {
            provider.setSpecialty(dto.getProfessionalDetails().getSpecialty());
            provider.setProviderType(dto.getProfessionalDetails().getProviderType());
            provider.setLicenseNumber(dto.getProfessionalDetails().getLicenseNumber());
            provider.setLicenseState(dto.getProfessionalDetails().getLicenseState());
            provider.setLicenseExpiry(dto.getProfessionalDetails().getLicenseExpiry());
        }
        return provider;
    }

    private ProviderDto mapToDto(Provider provider) {
        ProviderDto dto = new ProviderDto();
        dto.setId(provider.getId()); // Always set id
        dto.setNpi(provider.getNpi());
        if (provider.getFirstName() != null || provider.getLastName() != null) {
            ProviderDto.Identification identification = new ProviderDto.Identification();
            identification.setFirstName(provider.getFirstName());
            identification.setLastName(provider.getLastName());
            identification.setMiddleName(provider.getMiddleName());
            identification.setPrefix(provider.getPrefix());
            identification.setSuffix(provider.getSuffix());
            identification.setGender(provider.getGender());
            identification.setDateOfBirth(provider.getDateOfBirth());
            identification.setPhoto(provider.getPhoto());
            dto.setIdentification(identification);
        }
        if (provider.getEmail() != null || provider.getPhoneNumber() != null) {
            ProviderDto.Contact contact = new ProviderDto.Contact();
            contact.setEmail(provider.getEmail());
            contact.setPhoneNumber(provider.getPhoneNumber());
            contact.setMobileNumber(provider.getMobileNumber());
            contact.setFaxNumber(provider.getFaxNumber());
            if (provider.getAddress() != null) {
                ProviderDto.Contact.Address address = new ProviderDto.Contact.Address();
                contact.setAddress(address);
            }
            dto.setContact(contact);
        }
        if (provider.getSpecialty() != null || provider.getLicenseNumber() != null) {
            ProviderDto.ProfessionalDetails professionalDetails = new ProviderDto.ProfessionalDetails();
            professionalDetails.setSpecialty(provider.getSpecialty());
            professionalDetails.setProviderType(provider.getProviderType());
            professionalDetails.setLicenseNumber(provider.getLicenseNumber());
            professionalDetails.setLicenseState(provider.getLicenseState());
            professionalDetails.setLicenseExpiry(provider.getLicenseExpiry());
            dto.setProfessionalDetails(professionalDetails);
        }
        dto.setFhirId(provider.getExternalId()); // Map externalId to fhirId
        if (provider.getCreatedDate() != null || provider.getLastModifiedDate() != null) {
            ProviderDto.Audit audit = new ProviderDto.Audit();
            audit.setCreatedDate(provider.getCreatedDate());
            audit.setLastModifiedDate(provider.getLastModifiedDate());
            dto.setAudit(audit);
        }
        return dto;
    }

    private Long getCurrentOrgId() {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        if (orgId == null) {
            log.warn("orgId is null in RequestContext, attempting to populate from X-Org-Id header");
            // This is a fallback; ideally, the filter should handle this
            // For now, assume a manual check or filter is in place
        }
        return orgId;
    }
}