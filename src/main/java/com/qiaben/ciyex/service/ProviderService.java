package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.ProviderDto;
import com.qiaben.ciyex.entity.Provider;
import com.qiaben.ciyex.entity.ProviderStatus;
import com.qiaben.ciyex.repository.ProviderRepository;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import com.qiaben.ciyex.dto.integration.RequestContext;
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
        // Validate the required fields with detailed error message
        validateMandatoryFields(dto);

        // Map DTO to Entity
        Provider provider = mapToEntity(dto);


        String externalId = null;

        String tenantName = getTenantNameSafely();

        // Attempt external storage creation first (FHIR)
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            try {
                ExternalStorage<ProviderDto> externalStorage = storageResolver.resolve(ProviderDto.class);
                externalId = externalStorage.create(dto);  // Create in external storage
                log.info("Successfully created provider in external storage with externalId: {} for orgId: {}", externalId, tenantName);
            } catch (Exception e) {
                log.error("Failed to create provider in external storage for orgId: {}, error: {}", tenantName, e.getMessage());
                // Don't throw exception - continue with database save even if external storage fails
                log.warn("Continuing with database save despite external storage failure for tenant: {}", tenantName);
            }
        }

        // Ensure externalId is set
        provider.setExternalId(externalId);

        // Save to database
        provider = repository.save(provider);
        log.info("Created provider with id: {} and externalId: {} in DB for orgId: {}", provider.getId(), externalId, tenantName);

        // Return the DTO of the provider (with externalId)
        return mapToDto(provider);
    }

    @Transactional(readOnly = true)
    public ProviderDto getById(Long id) {
        // Fetch provider from the database
        Provider provider = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found with id: " + id));
        String tenantName = getTenantNameSafely();
        log.debug("Fetched provider from DB: id={}, externalId={}, Tenant={}", provider.getId(), provider.getExternalId(), tenantName);

        // Default mapping from DB data
        ProviderDto resultDto = mapToDto(provider);

        // If external storage is available, attempt to fetch extended provider details
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && provider.getExternalId() != null) {
            log.debug("Attempting to fetch extended details for provider id: {} with externalId: {} for orgId: {}", id, provider.getExternalId(), tenantName);
            ExternalStorage<ProviderDto> externalStorage = storageResolver.resolve(ProviderDto.class);

            try {
                ProviderDto extendedProviderDto = externalStorage.get(provider.getExternalId());

                if (extendedProviderDto != null) {
                    log.info("Successfully loaded extended details for provider id: {} from external storage for orgId: {}", id, tenantName);
                    log.debug("Extended ProviderDto: id={}, fhirId={}={}", extendedProviderDto.getId(), extendedProviderDto.getFhirId(), tenantName);

                    extendedProviderDto.setId(provider.getId()); // Preserve DB ID
                    // Manually map fields from Provider to extendedProviderDto
                    populateIdentification(provider, extendedProviderDto);
                    populateProfessionalDetails(provider, extendedProviderDto);

                    // Ensure NPI is set if missing from extended data
                    if (extendedProviderDto.getNpi() == null) {
                        extendedProviderDto.setNpi(provider.getNpi());
                    }

                    resultDto = extendedProviderDto;
                } else {
                    log.warn("No extended details found in external storage for provider id: {} with externalId: {} for orgId: {}", id, provider.getExternalId(), tenantName);
                }
            } catch (Exception e) {
                log.error("Failed to fetch extended details from external storage for provider id: {} with externalId: {}, error: {}", id, provider.getExternalId(), e.getMessage());
            }
        }

        log.info("Returning provider dto for id: {} and orgId: {}", id, tenantName);
        log.debug("Returning ProviderDto: id={}, fhirId={}={}", resultDto.getId(), resultDto.getFhirId(), tenantName);

        return resultDto;
    }

    // Method to populate identification fields
    private void populateIdentification(Provider provider, ProviderDto extendedProviderDto) {
        ProviderDto.Identification identification = new ProviderDto.Identification();
        identification.setFirstName(provider.getFirstName());
        identification.setLastName(provider.getLastName());
        identification.setMiddleName(provider.getMiddleName());
        identification.setPrefix(provider.getPrefix());
        identification.setSuffix(provider.getSuffix());
        identification.setGender(provider.getGender());
        identification.setDateOfBirth(provider.getDateOfBirth());
        identification.setPhoto(provider.getPhoto());

        extendedProviderDto.setIdentification(identification);
    }

    // Method to populate professional details fields
    private void populateProfessionalDetails(Provider provider, ProviderDto extendedProviderDto) {
        ProviderDto.ProfessionalDetails professionalDetails = new ProviderDto.ProfessionalDetails();
        professionalDetails.setSpecialty(provider.getSpecialty());
        professionalDetails.setProviderType(provider.getProviderType());
        professionalDetails.setLicenseNumber(provider.getLicenseNumber());
        professionalDetails.setLicenseState(provider.getLicenseState());
        professionalDetails.setLicenseExpiry(provider.getLicenseExpiry());

        extendedProviderDto.setProfessionalDetails(professionalDetails);
    }

    @Transactional
    public ProviderDto update(Long id, ProviderDto dto) {
        Provider provider = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found with id: " + id));

        // Update NPI only if provided
        if (dto.getNpi() != null) {
            provider.setNpi(dto.getNpi());
        }

        if (dto.getIdentification() != null) {
            if (dto.getIdentification().getFirstName() != null) provider.setFirstName(dto.getIdentification().getFirstName());
            if (dto.getIdentification().getLastName() != null) provider.setLastName(dto.getIdentification().getLastName());
            if (dto.getIdentification().getMiddleName() != null) provider.setMiddleName(dto.getIdentification().getMiddleName());
            if (dto.getIdentification().getPrefix() != null) provider.setPrefix(dto.getIdentification().getPrefix());
            if (dto.getIdentification().getSuffix() != null) provider.setSuffix(dto.getIdentification().getSuffix());
            if (dto.getIdentification().getGender() != null) provider.setGender(dto.getIdentification().getGender());
            if (dto.getIdentification().getDateOfBirth() != null) provider.setDateOfBirth(dto.getIdentification().getDateOfBirth());
            if (dto.getIdentification().getPhoto() != null) provider.setPhoto(dto.getIdentification().getPhoto());
        }
        if (dto.getContact() != null) {
            if (dto.getContact().getEmail() != null) provider.setEmail(dto.getContact().getEmail());
            if (dto.getContact().getPhoneNumber() != null) provider.setPhoneNumber(dto.getContact().getPhoneNumber());
            if (dto.getContact().getMobileNumber() != null) provider.setMobileNumber(dto.getContact().getMobileNumber());
            if (dto.getContact().getFaxNumber() != null) provider.setFaxNumber(dto.getContact().getFaxNumber());
            if (dto.getContact().getAddress() != null) provider.setAddress(dto.getContact().getAddress().toString());
        }
        if (dto.getProfessionalDetails() != null) {
            if (dto.getProfessionalDetails().getSpecialty() != null) provider.setSpecialty(dto.getProfessionalDetails().getSpecialty());
            if (dto.getProfessionalDetails().getProviderType() != null) provider.setProviderType(dto.getProfessionalDetails().getProviderType());
            if (dto.getProfessionalDetails().getLicenseNumber() != null) provider.setLicenseNumber(dto.getProfessionalDetails().getLicenseNumber());
            if (dto.getProfessionalDetails().getLicenseState() != null) provider.setLicenseState(dto.getProfessionalDetails().getLicenseState());
            if (dto.getProfessionalDetails().getLicenseExpiry() != null) provider.setLicenseExpiry(dto.getProfessionalDetails().getLicenseExpiry());
        }

        // Update SystemAccess status if provided
        if (dto.getSystemAccess() != null && dto.getSystemAccess().getStatus() != null) {
            provider.setStatus(dto.getSystemAccess().getStatus());
        }

        String tenantName = getTenantNameSafely();
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && provider.getExternalId() != null) {
            try {
                ExternalStorage<ProviderDto> externalStorage = storageResolver.resolve(ProviderDto.class);
                externalStorage.update(dto, provider.getExternalId());
                log.info("Successfully updated provider with id: {} and externalId: {} in external storage for orgId: {}", provider.getId(), provider.getExternalId(), tenantName);
            } catch (Exception e) {
                log.error("Failed to update provider in external storage for orgId: {}, error: {}", tenantName, e.getMessage());
                // Don't throw exception - continue with database save even if external storage fails
                log.warn("Continuing with database update despite external storage failure for tenant: {}", tenantName);
            }
        }

        Provider updatedProvider = repository.save(provider);
        return mapToDto(updatedProvider);
    }

    @Transactional
    public void delete(Long id) {
        Provider provider = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found with id: " + id));

        String tenantName = getTenantNameSafely();
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && provider.getExternalId() != null) {
            try {
                ExternalStorage<ProviderDto> externalStorage = storageResolver.resolve(ProviderDto.class);
                externalStorage.delete(provider.getExternalId());
                log.info("Successfully deleted provider with id: {} and externalId: {} from external storage for orgId: {}", provider.getId(), provider.getExternalId(), tenantName);
            } catch (Exception e) {
                log.error("Failed to delete provider from external storage for orgId: {}, error: {}", tenantName, e.getMessage());
                // Don't throw exception - continue with database deletion even if external storage fails
                log.warn("Continuing with database deletion despite external storage failure for tenant: {}", tenantName);
            }
        }

        // Delete from database
        repository.delete(provider);
        log.info("Deleted provider with id: {} from DB for orgId: {}", id, tenantName);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<ProviderDto>> getAllProviders() {
        // Fetch all providers directly from the database
        List<Provider> providers = repository.findAll();
        String tenantName = getTenantNameSafely();
        log.info("Retrieved {} providers from DB for orgId: {}", providers.size(), tenantName);
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
        dto.setId(provider.getId());
        dto.setNpi(provider.getNpi());

        // ✅ Always populate Identification (avoid nulls)
        ProviderDto.Identification identification = new ProviderDto.Identification();
        identification.setFirstName(provider.getFirstName() != null ? provider.getFirstName() : "");
        identification.setLastName(provider.getLastName() != null ? provider.getLastName() : "");
        identification.setMiddleName(provider.getMiddleName());
        identification.setPrefix(provider.getPrefix());
        identification.setSuffix(provider.getSuffix());
        identification.setGender(provider.getGender());
        identification.setDateOfBirth(provider.getDateOfBirth());
        identification.setPhoto(provider.getPhoto());
        dto.setIdentification(identification);

        // ✅ Contact
        if (provider.getEmail() != null || provider.getPhoneNumber() != null) {
            ProviderDto.Contact contact = new ProviderDto.Contact();
            contact.setEmail(provider.getEmail());
            contact.setPhoneNumber(provider.getPhoneNumber());
            contact.setMobileNumber(provider.getMobileNumber());
            contact.setFaxNumber(provider.getFaxNumber());

            if (provider.getAddress() != null) {
                ProviderDto.Contact.Address address = new ProviderDto.Contact.Address();
                // map address fields if available
                contact.setAddress(address);
            }
            dto.setContact(contact);
        }

        // ✅ Professional Details
        if (provider.getSpecialty() != null || provider.getLicenseNumber() != null) {
            ProviderDto.ProfessionalDetails professionalDetails = new ProviderDto.ProfessionalDetails();
            professionalDetails.setSpecialty(provider.getSpecialty());
            professionalDetails.setProviderType(provider.getProviderType());
            professionalDetails.setLicenseNumber(provider.getLicenseNumber());
            professionalDetails.setLicenseState(provider.getLicenseState());
            professionalDetails.setLicenseExpiry(provider.getLicenseExpiry());
            dto.setProfessionalDetails(professionalDetails);
        }

        // ✅ FHIR/External ID
        dto.setFhirId(provider.getExternalId());

        // ✅ Audit
        if (provider.getCreatedDate() != null || provider.getLastModifiedDate() != null) {
            ProviderDto.Audit audit = new ProviderDto.Audit();
            audit.setCreatedDate(provider.getCreatedDate() != null ? provider.getCreatedDate().toString() : null);
            audit.setLastModifiedDate(provider.getLastModifiedDate() != null ? provider.getLastModifiedDate().toString() : null);
            dto.setAudit(audit);
        }

        // ✅ System Access
        ProviderDto.SystemAccess systemAccess = new ProviderDto.SystemAccess();
        systemAccess.setStatus(provider.getStatus());
        dto.setSystemAccess(systemAccess);

        return dto;
    }




    @Transactional(readOnly = true)
    public long getProviderCount() {
        // You might want to add security checks here if needed
        return repository.count();
    }

    @Transactional
    public ProviderDto updateStatus(Long id, ProviderStatus status) {
        Provider provider = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found with id: " + id));
        provider.setStatus(status);
        return mapToDto(repository.save(provider));
    }

    @Transactional
    public boolean resetProviderPassword(Long providerId, String newPassword) {

        Provider provider = repository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider not found with id: " + providerId));
        // Implementation depends on your password storage strategy
        // This is a placeholder - you should implement proper password hashing
        log.info("Password reset requested for provider id: {} in org: {}", providerId, getTenantNameSafely());

        return true; // Return success status
    }

    /**
     * Safely get tenant name from RequestContext, returning a default value if context is null
     */
    private String getTenantNameSafely() {
        try {
            RequestContext context = RequestContext.get();
            return (context != null && context.getTenantName() != null) ? context.getTenantName() : "default";
        } catch (Exception e) {
            log.warn("Failed to get tenant name from RequestContext: {}", e.getMessage());
            return "default";
        }
    }

    /**
     * Validate mandatory fields for Provider creation
     */
    private void validateMandatoryFields(ProviderDto dto) {
        StringBuilder errors = new StringBuilder();

        if (dto.getNpi() == null || dto.getNpi().trim().isEmpty()) {
            errors.append("npi, ");
        }

        if (dto.getIdentification() == null) {
            errors.append("identification, ");
        } else {
            if (dto.getIdentification().getFirstName() == null || dto.getIdentification().getFirstName().trim().isEmpty()) {
                errors.append("firstName, ");
            }
            if (dto.getIdentification().getLastName() == null || dto.getIdentification().getLastName().trim().isEmpty()) {
                errors.append("lastName, ");
            }
        }

        if (dto.getProfessionalDetails() == null) {
            errors.append("professionalDetails, ");
        } else {
            if (dto.getProfessionalDetails().getLicenseNumber() == null || dto.getProfessionalDetails().getLicenseNumber().trim().isEmpty()) {
                errors.append("licenseNumber, ");
            }
        }

        if (errors.length() > 0) {
            // Remove trailing comma and space
            String missingFields = errors.substring(0, errors.length() - 2);
            throw new IllegalArgumentException("Missing mandatory fields: " + missingFields);
        }
    }

}