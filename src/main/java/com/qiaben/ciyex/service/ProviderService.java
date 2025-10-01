
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.qiaben.ciyex.entity.User;
import com.qiaben.ciyex.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Service
@Slf4j
public class ProviderService {

    private final ProviderRepository repository;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;

    // === NEW: user+password support ===
    @Autowired
    private UserRepository userRepository;
    @Autowired(required = false)
    private PasswordEncoder passwordEncoder;

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

        // Validate the required fields
        if (dto.getNpi() == null || dto.getIdentification() == null || dto.getIdentification().getFirstName() == null ||
                dto.getIdentification().getLastName() == null || dto.getProfessionalDetails() == null ||
                dto.getProfessionalDetails().getLicenseNumber() == null) {
            throw new IllegalArgumentException("NPI, first name, last name, and license number are required");
        }

        // Map DTO to Entity
        Provider provider = mapToEntity(dto);
        provider.setOrgId(currentOrgId);
        provider.setCreatedDate(LocalDateTime.now().toString());
        provider.setLastModifiedDate(LocalDateTime.now().toString());

        String externalId = null;

        // Attempt external storage creation first (FHIR)
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            try {
                ExternalStorage<ProviderDto> externalStorage = storageResolver.resolve(ProviderDto.class);
                externalId = externalStorage.create(dto);  // Create in external storage
                log.info("Successfully created provider in external storage with externalId: {} for orgId: {}", externalId, currentOrgId);
            } catch (Exception e) {
                log.error("Failed to create provider in external storage for orgId: {}, error: {}", currentOrgId, e.getMessage());
                throw new RuntimeException("Failed to sync with external storage", e);  // Rollback transaction
            }
        }

        // Ensure externalId is set
        provider.setExternalId(externalId);

        // Save to database **only after external storage is successful**
        provider = repository.save(provider);
        log.info("Created provider with id: {} and externalId: {} in DB for orgId: {}", provider.getId(), externalId, currentOrgId);

        // Return the DTO of the provider (with externalId)
        return mapToDto(provider);
    }

    @Transactional(readOnly = true)
    public ProviderDto getById(Long id) {
        log.debug("Entering getById with id: {}", id);

        // Fetch the current organization ID
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            log.error("No orgId found in RequestContext during getById for id: {}", id);
            throw new SecurityException("No orgId available in request context");
        }

        log.debug("Verifying access for orgId: {} to provider with id: {}", currentOrgId, id);

        // Fetch provider from the database
        Provider provider = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found with id: " + id));
        log.debug("Fetched provider from DB: id={}, externalId={}, orgId={}", provider.getId(), provider.getExternalId(), provider.getOrgId());

        // Check if the provider belongs to the current organization
        if (!currentOrgId.equals(provider.getOrgId())) {
            throw new SecurityException("Access denied: Provider id " + id + " does not belong to orgId " + currentOrgId);
        }
        log.info("Found provider in DB with id: {} for orgId: {}", id, currentOrgId);

        // Default mapping from DB data
        ProviderDto resultDto = mapToDto(provider);

        // If external storage is available, attempt to fetch extended provider details
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && provider.getExternalId() != null) {
            log.debug("Attempting to fetch extended details for provider id: {} with externalId: {} for orgId: {}", id, provider.getExternalId(), currentOrgId);
            ExternalStorage<ProviderDto> externalStorage = storageResolver.resolve(ProviderDto.class);

            try {
                ProviderDto extendedProviderDto = externalStorage.get(provider.getExternalId());

                if (extendedProviderDto != null) {
                    log.info("Successfully loaded extended details for provider id: {} from external storage for orgId: {}", id, currentOrgId);
                    log.debug("Extended ProviderDto: id={}, fhirId={}, orgId={}", extendedProviderDto.getId(), extendedProviderDto.getFhirId(), extendedProviderDto.getOrgId());

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
    dto.setId(provider.getId());
    dto.setNpi(provider.getNpi());
    dto.setOrgId(provider.getOrgId());

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
        audit.setCreatedDate(provider.getCreatedDate());
        audit.setLastModifiedDate(provider.getLastModifiedDate());
        dto.setAudit(audit);
    }

    // ✅ System Access
    ProviderDto.SystemAccess systemAccess = new ProviderDto.SystemAccess();
    systemAccess.setStatus(provider.getStatus());
    dto.setSystemAccess(systemAccess);

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

    @Transactional(readOnly = true)
    public long getProviderCountByOrgId() {
        // You might want to add security checks here if needed
        return repository.countByOrgId(getCurrentOrgId());
    }

    @Transactional
    public ProviderDto updateStatus(Long id, ProviderStatus status) {
        Provider provider = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found"));
        provider.setStatus(status);
        provider.setLastModifiedDate(LocalDateTime.now().toString());
        return mapToDto(repository.save(provider));
    }

    // =========================
    // NEW: Reset password by provider id
    // =========================
    @Transactional
    public void resetProviderPassword(Long providerId, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New password is required");
        }

        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            throw new SecurityException("No orgId available in request context");
        }

        Provider provider = repository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found with id: " + providerId));

        if (provider.getOrgId() == null || !provider.getOrgId().equals(currentOrgId)) {
            throw new SecurityException("Access denied to this provider for the current org");
        }

        String email = provider.getEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Provider does not have an email set");
        }

        // Create (if missing) or fetch the user by email
        Optional<User> existing = userRepository.findByEmail(email);
        User user = existing.orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setFirstName(provider.getFirstName());
            u.setLastName(provider.getLastName());
            if (u.getUuid() == null) {
                u.setUuid(java.util.UUID.randomUUID());
            }
            return u;
        });

        // ✅ FIX: use injected encoder if available, otherwise fallback to BCrypt
        PasswordEncoder encoder = (this.passwordEncoder != null)
                ? this.passwordEncoder
                : new BCryptPasswordEncoder();

        user.setPassword(encoder.encode(newPassword));

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            log.error("Failed to save user for provider {} ({}): {}", providerId, email, ex.getMostSpecificCause().getMessage());
            throw new IllegalArgumentException("Could not save user for this provider: " + ex.getMostSpecificCause().getMessage());
        }

        log.info("Password set for user linked to provider {} (email={})", providerId, email);
    }

}