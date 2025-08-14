package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.ProviderDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.entity.Provider;
import com.qiaben.ciyex.repository.ProviderRepository;
import com.qiaben.ciyex.storage.ExternalProviderStorage;
import com.qiaben.ciyex.storage.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Practitioner.PractitionerQualificationComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@StorageType("fhir")
@Component("fhirExternalProviderStorage")
@Slf4j
public class FhirExternalProviderStorage implements ExternalProviderStorage {

    private final FhirResourceStorage fhirResourceStorage;
    private final ProviderRepository providerRepository;

    @Autowired
    public FhirExternalProviderStorage(FhirResourceStorage fhirResourceStorage, ProviderRepository providerRepository) {
        this.fhirResourceStorage = fhirResourceStorage;
        this.providerRepository = providerRepository;
        log.info("Initializing FhirExternalProviderStorage with FhirResourceStorage and ProviderRepository");
    }

    @Transactional
    @Override
    public String createProvider(ProviderDto providerDto) {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            throw new SecurityException("No orgId available in request context");
        }
        log.debug("Verifying access for orgId: {} to create new provider", currentOrgId);
        providerDto.setOrgId(currentOrgId); // Set orgId for the new provider

        Practitioner fhirProvider = mapToFhirPractitioner(providerDto);
        String externalId = null;
        try {
            externalId = fhirResourceStorage.create(fhirProvider);
            log.info("Successfully created provider in external storage with externalId: {} for orgId: {}", externalId, currentOrgId);
        } catch (Exception e) {
            log.error("Failed to create provider in external storage for orgId: {}, error: {}, stacktrace: {}", currentOrgId, e.getMessage(), e);
            throw new RuntimeException("Failed to sync with external storage", e); // Rollback transaction
        }

        // Save to database only if external storage succeeded
        Provider provider = new Provider();
        provider.setExternalId(externalId);
        provider.setOrgId(currentOrgId);
        provider = providerRepository.save(provider);
        log.info("Created provider with id: {} and externalId: {} in DB for orgId: {}", provider.getId(), externalId, currentOrgId);

        return externalId;
    }

    @Transactional(readOnly = true)
    @Override
    public ProviderDto getProvider(String externalId) {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            throw new SecurityException("No orgId available in request context");
        }
        log.debug("Verifying access for orgId: {} to provider with externalId: {}", currentOrgId, externalId);

        // Fetch from database to check orgId
        Provider provider = providerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new RuntimeException("Provider not found with externalId: " + externalId));
        log.debug("Fetched provider from DB: id={}, externalId={}, orgId={}", provider.getId(), provider.getExternalId(), provider.getOrgId());
        if (!currentOrgId.equals(provider.getOrgId())) {
            throw new SecurityException("Access denied: Provider with externalId " + externalId + " does not belong to orgId " + currentOrgId);
        }

        Practitioner fhirProvider = fhirResourceStorage.get(Practitioner.class, externalId);
        if (fhirProvider == null) {
            log.warn("No FHIR Provider found with externalId: {} for orgId: {}", externalId, currentOrgId);
            throw new RuntimeException("Provider not found with externalId: " + externalId);
        }
        log.debug("Fetched FHIR Provider with id: {}, name={}", externalId, fhirProvider.getNameFirstRep());
        ProviderDto dto = mapFromFhirPractitioner(fhirProvider);
        log.info("Retrieved ProviderDto with externalId: {} for orgId: {}", externalId, currentOrgId);
        log.debug("Mapped ProviderDto: id={}, fhirId={}, orgId={}", dto.getId(), dto.getFhirId(), dto.getOrgId());
        return dto;
    }

    @Transactional
    @Override
    public void updateProvider(ProviderDto providerDto, String externalId) {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            throw new SecurityException("No orgId available in request context");
        }
        log.debug("Verifying access for orgId: {} to provider with externalId: {}", currentOrgId, externalId);

        // Fetch from database to check orgId
        Provider provider = providerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new RuntimeException("Provider not found with externalId: " + externalId));
        log.debug("Fetched provider from DB: id={}, externalId={}, orgId={}", provider.getId(), provider.getExternalId(), provider.getOrgId());
        if (!currentOrgId.equals(provider.getOrgId())) {
            throw new SecurityException("Access denied: Provider with externalId " + externalId + " does not belong to orgId " + currentOrgId);
        }

        Practitioner fhirProvider = mapToFhirPractitioner(providerDto);
        try {
            fhirResourceStorage.update(fhirProvider, externalId);
            log.info("Successfully updated provider with externalId: {} in external storage for orgId: {}", externalId, currentOrgId);
        } catch (Exception e) {
            log.error("Failed to update provider in external storage for orgId: {}, error: {}, stacktrace: {}", currentOrgId, e.getMessage(), e);
            throw new RuntimeException("Failed to sync with external storage", e); // Rollback transaction
        }

        // Update database only if external storage succeeded
        provider = providerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new RuntimeException("Provider not found with externalId: " + externalId));
        // Update provider fields if needed (e.g., name from providerDto)
        providerRepository.save(provider);
        log.info("Updated provider with id: {} and externalId: {} in DB for orgId: {}", provider.getId(), externalId, currentOrgId);
    }

    @Transactional
    @Override
    public void deleteProvider(String externalId) {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            throw new SecurityException("No orgId available in request context");
        }
        log.debug("Verifying access for orgId: {} to provider with externalId: {}", currentOrgId, externalId);

        // Fetch from database to check orgId
        Provider provider = providerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new RuntimeException("Provider not found with externalId: " + externalId));
        log.debug("Fetched provider from DB: id={}, externalId={}, orgId={}", provider.getId(), provider.getExternalId(), provider.getOrgId());
        if (!currentOrgId.equals(provider.getOrgId())) {
            throw new SecurityException("Access denied: Provider with externalId " + externalId + " does not belong to orgId " + currentOrgId);
        }

        try {
            fhirResourceStorage.delete("Practitioner", externalId);
            log.info("Successfully deleted provider with externalId: {} from external storage for orgId: {}", externalId, currentOrgId);
        } catch (Exception e) {
            log.error("Failed to delete provider from external storage for orgId: {}, error: {}, stacktrace: {}", currentOrgId, e.getMessage(), e);
            throw new RuntimeException("Failed to sync with external storage", e); // Rollback transaction
        }

        // Delete from database only if external storage succeeded
        providerRepository.delete(provider);
        log.info("Deleted provider with id: {} and externalId: {} from DB for orgId: {}", provider.getId(), externalId, currentOrgId);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ProviderDto> searchAllProviders() {
        Long currentOrgId = getCurrentOrgId();
        if (currentOrgId == null) {
            throw new SecurityException("No orgId available in request context");
        }
        log.debug("Verifying access for orgId: {} to retrieve all providers", currentOrgId);

        List<Practitioner> fhirProviders = fhirResourceStorage.searchAll(Practitioner.class);
        log.debug("Retrieved {} FHIR Providers from external storage for orgId: {}", fhirProviders.size(), currentOrgId);
        return fhirProviders.stream()
                .map(this::mapFromFhirPractitioner)
                .filter(dto -> dto.getOrgId() != null && dto.getOrgId().equals(currentOrgId))
                .collect(Collectors.toList());
    }

    @Override
    public String create(ProviderDto entityDto) {
        return createProvider(entityDto);
    }

    @Override
    public void update(ProviderDto entityDto, String externalId) {
        updateProvider(entityDto, externalId);
    }

    @Override
    public ProviderDto get(String externalId) {
        return getProvider(externalId);
    }

    @Override
    public void delete(String externalId) {
        deleteProvider(externalId);
    }

    @Override
    public List<ProviderDto> searchAll() {
        return searchAllProviders();
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return ProviderDto.class.isAssignableFrom(entityType);
    }

    private Practitioner mapToFhirPractitioner(ProviderDto providerDto) {
        log.debug("Entering mapToFhirPractitioner with ProviderDto: id={}, fhirId={}, orgId={}", providerDto.getId(), providerDto.getFhirId(), providerDto.getOrgId());
        Practitioner fhirProvider = new Practitioner();
        if (providerDto.getIdentification() != null) {
            ProviderDto.Identification identification = providerDto.getIdentification();
            if (identification.getFirstName() != null || identification.getLastName() != null) {
                HumanName name = new HumanName();
                if (identification.getPrefix() != null) name.addPrefix(identification.getPrefix());
                if (identification.getFirstName() != null) name.addGiven(identification.getFirstName());
                if (identification.getMiddleName() != null) name.addGiven(identification.getMiddleName());
                if (identification.getLastName() != null) name.setFamily(identification.getLastName());
                if (identification.getSuffix() != null) name.addSuffix(identification.getSuffix());
                fhirProvider.setName(Collections.singletonList(name));
                log.debug("Added name to Practitioner: {}", name);
            }
        }
        if (providerDto.getContact() != null) {
            ProviderDto.Contact contact = providerDto.getContact();
            if (contact.getEmail() != null) {
                fhirProvider.addTelecom(new ContactPoint().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue(contact.getEmail()));
                log.debug("Added email to Practitioner: {}", contact.getEmail());
            }
            if (contact.getPhoneNumber() != null) {
                fhirProvider.addTelecom(new ContactPoint().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue(contact.getPhoneNumber()));
                log.debug("Added phoneNumber to Practitioner: {}", contact.getPhoneNumber());
            }
            if (contact.getMobileNumber() != null) {
                fhirProvider.addTelecom(new ContactPoint().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue(contact.getMobileNumber()));
                log.debug("Added mobileNumber to Practitioner: {}", contact.getMobileNumber());
            }
            if (contact.getFaxNumber() != null) {
                fhirProvider.addTelecom(new ContactPoint().setSystem(ContactPoint.ContactPointSystem.FAX).setValue(contact.getFaxNumber()));
                log.debug("Added faxNumber to Practitioner: {}", contact.getFaxNumber());
            }
        }
        if (providerDto.getProfessionalDetails() != null) {
            ProviderDto.ProfessionalDetails professionalDetails = providerDto.getProfessionalDetails();
            if (professionalDetails.getSpecialty() != null) {
                fhirProvider.addQualification(new PractitionerQualificationComponent().setCode(new CodeableConcept().setText(professionalDetails.getSpecialty())));
                log.debug("Added specialty to Practitioner: {}", professionalDetails.getSpecialty());
            }
        }
        log.debug("Mapped ProviderDto to Practitioner: id={}, name={}", providerDto.getId(), fhirProvider.getNameFirstRep());
        return fhirProvider;
    }

    private ProviderDto mapFromFhirPractitioner(Practitioner fhirProvider) {
        log.debug("Entering mapFromFhirPractitioner with Practitioner: id={}, name={}", fhirProvider.getIdElement().getIdPart(), fhirProvider.getNameFirstRep());
        ProviderDto dto = new ProviderDto();
        if (!fhirProvider.getName().isEmpty()) {
            HumanName name = fhirProvider.getNameFirstRep();
            ProviderDto.Identification identification = new ProviderDto.Identification();
            identification.setPrefix(name.getPrefixAsSingleString());
            identification.setFirstName(name.getGivenAsSingleString());
            identification.setMiddleName(name.getGiven().size() > 1 ? name.getGiven().get(1).getValue() : null);
            identification.setLastName(name.getFamily());
            identification.setSuffix(name.getSuffixAsSingleString());
            dto.setIdentification(identification);
            log.debug("Mapped name to Identification: {}", identification);
        }
        ProviderDto.Contact contact = new ProviderDto.Contact();
        fhirProvider.getTelecom().forEach(telecom -> {
            switch (telecom.getSystem()) {
                case EMAIL:
                    contact.setEmail(telecom.getValue());
                    log.debug("Mapped email to Contact: {}", telecom.getValue());
                    break;
                case PHONE:
                    if (contact.getPhoneNumber() == null) contact.setPhoneNumber(telecom.getValue());
                    else if (contact.getMobileNumber() == null) contact.setMobileNumber(telecom.getValue());
                    log.debug("Mapped phone to Contact: {}", telecom.getValue());
                    break;
                case FAX:
                    contact.setFaxNumber(telecom.getValue());
                    log.debug("Mapped fax to Contact: {}", telecom.getValue());
                    break;
            }
        });
        dto.setContact(contact);
        if (!fhirProvider.getQualification().isEmpty()) {
            ProviderDto.ProfessionalDetails professionalDetails = new ProviderDto.ProfessionalDetails();
            professionalDetails.setSpecialty(fhirProvider.getQualificationFirstRep().getCode().getText());
            dto.setProfessionalDetails(professionalDetails);
            log.debug("Mapped specialty to ProfessionalDetails: {}", professionalDetails.getSpecialty());
        }
        dto.setFhirId(fhirProvider.getIdElement().getIdPart());
        dto.setOrgId(getCurrentOrgId()); // Set orgId from RequestContext
        log.debug("Mapped ProviderDto: id={}, fhirId={}, orgId={}", dto.getId(), dto.getFhirId(), dto.getOrgId());
        return dto;
    }

    private Long getCurrentOrgId() {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        if (orgId == null) {
            log.warn("orgId is null in RequestContext");
        }
        return orgId;
    }
}