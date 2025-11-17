package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.PracticeDto;
import com.qiaben.ciyex.entity.Practice;
import com.qiaben.ciyex.repository.PracticeRepository;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import com.qiaben.ciyex.dto.integration.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PracticeService {

    private final PracticeRepository repository;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;

    @Autowired
    public PracticeService(PracticeRepository repository, ExternalStorageResolver storageResolver, OrgIntegrationConfigProvider configProvider) {
        this.repository = repository;
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
    }

    @Transactional
    public PracticeDto create(PracticeDto dto) {
        // Validate the required fields
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Practice name is required");
        }

        // Map DTO to Entity
        Practice practice = mapToEntity(dto);

        String externalId = null;

        // Attempt external storage creation first (FHIR)
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            try {
                ExternalStorage<PracticeDto> externalStorage = storageResolver.resolve(PracticeDto.class);
                externalId = externalStorage.create(dto);  // Create in external storage
                log.info("Successfully created practice in external storage with externalId: {}", externalId);
            } catch (Exception e) {
                log.error("Failed to create practice in external storage, error: {}", e.getMessage());
                // Don't throw exception - continue with database save even if external storage fails
                log.warn("Continuing with database save despite external storage failure");
            }
        }

        // Ensure externalId is set
        practice.setExternalId(externalId);

        // Save to database
        practice = repository.save(practice);
        log.info("Created practice with id: {} and externalId: {} in DB", practice.getId(), externalId);

        // Return the DTO of the practice (with externalId)
        return mapToDto(practice);
    }

    @Transactional(readOnly = true)
    public PracticeDto getById(Long id) {
        // Fetch practice from the database
        Practice practice = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Practice not found with id: " + id));
        log.debug("Fetched practice from DB: id={}, externalId={}", practice.getId(), practice.getExternalId());

        // Default mapping from DB data
        PracticeDto resultDto = mapToDto(practice);

        // If external storage is available, attempt to fetch extended practice details
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && practice.getExternalId() != null) {
            log.debug("Attempting to fetch extended details for practice id: {} with externalId: {}", id, practice.getExternalId());
            ExternalStorage<PracticeDto> externalStorage = storageResolver.resolve(PracticeDto.class);

            try {
                PracticeDto extendedPracticeDto = externalStorage.get(practice.getExternalId());

                if (extendedPracticeDto != null) {
                    log.info("Successfully loaded extended details for practice id: {} from external storage", id);
                    log.debug("Extended PracticeDto: id={}, fhirId={}", extendedPracticeDto.getId(), extendedPracticeDto.getFhirId());

                    extendedPracticeDto.setId(practice.getId()); // Preserve DB ID
                    // Manually map fields from Practice to extendedPracticeDto
                    populatePracticeSettings(practice, extendedPracticeDto);
                    populateRegionalSettings(practice, extendedPracticeDto);
                    populateContact(practice, extendedPracticeDto);

                    // Ensure name is set if missing from extended data
                    if (extendedPracticeDto.getName() == null) {
                        extendedPracticeDto.setName(practice.getName());
                    }

                    resultDto = extendedPracticeDto;
                } else {
                    log.warn("No extended details found in external storage for practice id: {} with externalId: {}", id, practice.getExternalId());
                }
            } catch (Exception e) {
                log.error("Failed to fetch extended details from external storage for practice id: {} with externalId: {}, error: {}", id, practice.getExternalId(), e.getMessage());
            }
        }

        log.info("Returning practice dto for id: {}", id);
        log.debug("Returning PracticeDto: id={}, fhirId={}", resultDto.getId(), resultDto.getFhirId());

        return resultDto;
    }

    @Transactional
    public PracticeDto update(Long id, PracticeDto dto) {
        Practice practice = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Practice not found with id: " + id));

        // Update fields
        practice.setName(dto.getName());
        practice.setDescription(dto.getDescription());

        if (dto.getPracticeSettings() != null) {
            practice.setEnablePatientPractice(dto.getPracticeSettings().getEnablePatientPractice());
        }

        if (dto.getRegionalSettings() != null) {
            PracticeDto.RegionalSettings regional = dto.getRegionalSettings();
            practice.setUnitsForVisitForms(regional.getUnitsForVisitForms());
            practice.setDisplayFormatUSWeights(regional.getDisplayFormatUSWeights());
            practice.setTelephoneCountryCode(regional.getTelephoneCountryCode());
            practice.setDateDisplayFormat(regional.getDateDisplayFormat());
            practice.setTimeDisplayFormat(regional.getTimeDisplayFormat());
            practice.setTimeZone(regional.getTimeZone());
            practice.setCurrencyDesignator(regional.getCurrencyDesignator());
        }

        if (dto.getContact() != null) {
            PracticeDto.Contact contact = dto.getContact();
            practice.setEmail(contact.getEmail());
            practice.setPhoneNumber(contact.getPhoneNumber());
            practice.setFaxNumber(contact.getFaxNumber());

            if (contact.getAddress() != null) {
                PracticeDto.Contact.Address address = contact.getAddress();
                practice.setAddressLine1(address.getLine1());
                practice.setAddressLine2(address.getLine2());
                practice.setCity(address.getCity());
                practice.setState(address.getState());
                practice.setPostalCode(address.getPostalCode());
                practice.setCountry(address.getCountry());
            }
        }

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && practice.getExternalId() != null) {
            try {
                ExternalStorage<PracticeDto> externalStorage = storageResolver.resolve(PracticeDto.class);
                externalStorage.update(dto, practice.getExternalId());
                log.info("Successfully updated practice with id: {} and externalId: {} in external storage", practice.getId(), practice.getExternalId());
            } catch (Exception e) {
                log.error("Failed to update practice in external storage, error: {}", e.getMessage());
                // Don't throw exception - continue with database save even if external storage fails
                log.warn("Continuing with database update despite external storage failure");
            }
        }

        Practice updatedPractice = repository.save(practice);
        return mapToDto(updatedPractice);
    }

    @Transactional
    public void delete(Long id) {
        Practice practice = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Practice not found with id: " + id));

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && practice.getExternalId() != null) {
            try {
                ExternalStorage<PracticeDto> externalStorage = storageResolver.resolve(PracticeDto.class);
                externalStorage.delete(practice.getExternalId());
                log.info("Successfully deleted practice with id: {} and externalId: {} from external storage", practice.getId(), practice.getExternalId());
            } catch (Exception e) {
                log.error("Failed to delete practice from external storage, error: {}", e.getMessage());
                // Don't throw exception - continue with database deletion even if external storage fails
                log.warn("Continuing with database deletion despite external storage failure");
            }
        }

        // Delete from database
        repository.delete(practice);
        log.info("Deleted practice with id: {} from DB", id);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<PracticeDto>> getAllPractices() {
        // Fetch all practices directly from the database
        List<Practice> practices = repository.findAll();
        log.info("Retrieved {} practices from DB", practices.size());
        List<PracticeDto> practiceDtos = practices.stream().map(this::mapToDto).collect(Collectors.toList());

        return ApiResponse.<List<PracticeDto>>builder()
                .success(true)
                .message("Practices retrieved successfully")
                .data(practiceDtos)
                .build();
    }

    @Transactional(readOnly = true)
    public long getPracticeCount() {
        return repository.count();
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<PracticeDto>> getPracticesByName(String name) {
        List<Practice> practices = repository.findByNameContaining(name);
        log.info("Retrieved {} practices matching name '{}'", practices.size(), name);
        List<PracticeDto> practiceDtos = practices.stream().map(this::mapToDto).collect(Collectors.toList());

        return ApiResponse.<List<PracticeDto>>builder()
                .success(true)
                .message("Practices retrieved successfully")
                .data(practiceDtos)
                .build();
    }

    // Helper methods to populate nested objects
    private void populatePracticeSettings(Practice practice, PracticeDto extendedPracticeDto) {
        PracticeDto.PracticeSettings practiceSettings = new PracticeDto.PracticeSettings();
        practiceSettings.setEnablePatientPractice(practice.getEnablePatientPractice());
        extendedPracticeDto.setPracticeSettings(practiceSettings);
    }

    private void populateRegionalSettings(Practice practice, PracticeDto extendedPracticeDto) {
        PracticeDto.RegionalSettings regionalSettings = new PracticeDto.RegionalSettings();
        regionalSettings.setUnitsForVisitForms(practice.getUnitsForVisitForms());
        regionalSettings.setDisplayFormatUSWeights(practice.getDisplayFormatUSWeights());
        regionalSettings.setTelephoneCountryCode(practice.getTelephoneCountryCode());
        regionalSettings.setDateDisplayFormat(practice.getDateDisplayFormat());
        regionalSettings.setTimeDisplayFormat(practice.getTimeDisplayFormat());
        regionalSettings.setTimeZone(practice.getTimeZone());
        regionalSettings.setCurrencyDesignator(practice.getCurrencyDesignator());
        extendedPracticeDto.setRegionalSettings(regionalSettings);
    }

    private void populateContact(Practice practice, PracticeDto extendedPracticeDto) {
        PracticeDto.Contact contact = new PracticeDto.Contact();
        contact.setEmail(practice.getEmail());
        contact.setPhoneNumber(practice.getPhoneNumber());
        contact.setFaxNumber(practice.getFaxNumber());

        if (practice.getAddressLine1() != null || practice.getCity() != null) {
            PracticeDto.Contact.Address address = new PracticeDto.Contact.Address();
            address.setLine1(practice.getAddressLine1());
            address.setLine2(practice.getAddressLine2());
            address.setCity(practice.getCity());
            address.setState(practice.getState());
            address.setPostalCode(practice.getPostalCode());
            address.setCountry(practice.getCountry());
            contact.setAddress(address);
        }

        extendedPracticeDto.setContact(contact);
    }

    private Practice mapToEntity(PracticeDto dto) {
        Practice practice = new Practice();
        practice.setName(dto.getName());
        practice.setDescription(dto.getDescription());

        if (dto.getPracticeSettings() != null) {
            practice.setEnablePatientPractice(dto.getPracticeSettings().getEnablePatientPractice());
        }

        if (dto.getRegionalSettings() != null) {
            PracticeDto.RegionalSettings regional = dto.getRegionalSettings();
            practice.setUnitsForVisitForms(regional.getUnitsForVisitForms());
            practice.setDisplayFormatUSWeights(regional.getDisplayFormatUSWeights());
            practice.setTelephoneCountryCode(regional.getTelephoneCountryCode());
            practice.setDateDisplayFormat(regional.getDateDisplayFormat());
            practice.setTimeDisplayFormat(regional.getTimeDisplayFormat());
            practice.setTimeZone(regional.getTimeZone());
            practice.setCurrencyDesignator(regional.getCurrencyDesignator());
        }

        if (dto.getContact() != null) {
            PracticeDto.Contact contact = dto.getContact();
            practice.setEmail(contact.getEmail());
            practice.setPhoneNumber(contact.getPhoneNumber());
            practice.setFaxNumber(contact.getFaxNumber());

            if (contact.getAddress() != null) {
                PracticeDto.Contact.Address address = contact.getAddress();
                practice.setAddressLine1(address.getLine1());
                practice.setAddressLine2(address.getLine2());
                practice.setCity(address.getCity());
                practice.setState(address.getState());
                practice.setPostalCode(address.getPostalCode());
                practice.setCountry(address.getCountry());
            }
        }

        return practice;
    }

    private PracticeDto mapToDto(Practice practice) {
        PracticeDto dto = new PracticeDto();
        dto.setId(practice.getId());
        dto.setName(practice.getName());
        dto.setDescription(practice.getDescription());
        dto.setFhirId(practice.getExternalId());

        // Practice Settings
        if (practice.getEnablePatientPractice() != null) {
            PracticeDto.PracticeSettings practiceSettings = new PracticeDto.PracticeSettings();
            practiceSettings.setEnablePatientPractice(practice.getEnablePatientPractice());
            dto.setPracticeSettings(practiceSettings);
        }

        // Regional Settings
        PracticeDto.RegionalSettings regionalSettings = new PracticeDto.RegionalSettings();
        regionalSettings.setUnitsForVisitForms(practice.getUnitsForVisitForms());
        regionalSettings.setDisplayFormatUSWeights(practice.getDisplayFormatUSWeights());
        regionalSettings.setTelephoneCountryCode(practice.getTelephoneCountryCode());
        regionalSettings.setDateDisplayFormat(practice.getDateDisplayFormat());
        regionalSettings.setTimeDisplayFormat(practice.getTimeDisplayFormat());
        regionalSettings.setTimeZone(practice.getTimeZone());
        regionalSettings.setCurrencyDesignator(practice.getCurrencyDesignator());
        dto.setRegionalSettings(regionalSettings);

        // Contact Information
        if (practice.getEmail() != null || practice.getPhoneNumber() != null || practice.getAddressLine1() != null) {
            PracticeDto.Contact contact = new PracticeDto.Contact();
            contact.setEmail(practice.getEmail());
            contact.setPhoneNumber(practice.getPhoneNumber());
            contact.setFaxNumber(practice.getFaxNumber());

            if (practice.getAddressLine1() != null || practice.getCity() != null) {
                PracticeDto.Contact.Address address = new PracticeDto.Contact.Address();
                address.setLine1(practice.getAddressLine1());
                address.setLine2(practice.getAddressLine2());
                address.setCity(practice.getCity());
                address.setState(practice.getState());
                address.setPostalCode(practice.getPostalCode());
                address.setCountry(practice.getCountry());
                contact.setAddress(address);
            }
            dto.setContact(contact);
        }

        // Audit Information
        if (practice.getCreatedDate() != null || practice.getLastModifiedDate() != null) {
            PracticeDto.Audit audit = new PracticeDto.Audit();
            audit.setCreatedDate(practice.getCreatedDate() != null ? practice.getCreatedDate().toString() : null);
            audit.setLastModifiedDate(practice.getLastModifiedDate() != null ? practice.getLastModifiedDate().toString() : null);
            audit.setCreatedBy(practice.getCreatedBy());
            audit.setLastModifiedBy(practice.getLastModifiedBy());
            dto.setAudit(audit);
        }

        return dto;
    }
}