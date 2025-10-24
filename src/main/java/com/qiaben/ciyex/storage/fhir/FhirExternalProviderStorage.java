package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.ProviderDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
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
    private final ProviderRepository providerRepository; // retained for potential future DB sync

    @Autowired
    public FhirExternalProviderStorage(FhirResourceStorage fhirResourceStorage, ProviderRepository providerRepository) {
        this.fhirResourceStorage = fhirResourceStorage;
        this.providerRepository = providerRepository;
        log.info("Initializing FhirExternalProviderStorage with FhirResourceStorage and ProviderRepository");
    }

    @Transactional
    @Override
    public String createProvider(ProviderDto providerDto) {
        String tenantName = getCurrentTenantName();
        log.debug("Creating provider in external storage for tenantName={}", tenantName);

        Practitioner fhirProvider = mapToFhirPractitioner(providerDto);
        String externalId = null;
        try {
            // Creating the provider in the external FHIR system and returning externalId
            externalId = fhirResourceStorage.create(fhirProvider);
            log.info("Successfully created provider in external storage with externalId: {} for tenantName: {}", externalId, tenantName);
        } catch (Exception e) {
            log.error("Failed to create provider in external storage for tenantName: {}, error: {}", tenantName, e.getMessage());
            throw new RuntimeException("Failed to sync with external storage", e);  // Rollback transaction
        }

        // Only return the externalId without inserting into the database
        return externalId;
    }

    @Transactional(readOnly = true)
    @Override
    public ProviderDto getProvider(String externalId) {
        String tenantName = getCurrentTenantName();
        log.debug("Fetching provider externalId={} for tenantName={}", externalId, tenantName);

        Practitioner fhirProvider = fhirResourceStorage.get(Practitioner.class, externalId);
        if (fhirProvider == null) {
            log.warn("No FHIR Provider found with externalId: {} for tenantName: {}", externalId, tenantName);
            throw new RuntimeException("Provider not found with externalId: " + externalId);
        }
        log.debug("Fetched FHIR Provider with id: {}, name={}", externalId, fhirProvider.getNameFirstRep());
        ProviderDto dto = mapFromFhirPractitioner(fhirProvider);
        log.info("Retrieved ProviderDto with externalId: {} for tenantName: {}", externalId, tenantName);
        return dto;
    }

    @Transactional
    @Override
    public void updateProvider(ProviderDto providerDto, String externalId) {
        String tenantName = getCurrentTenantName();
        log.debug("Updating provider externalId={} for tenantName={}", externalId, tenantName);

        Practitioner fhirProvider = mapToFhirPractitioner(providerDto);
        try {
            fhirResourceStorage.update(fhirProvider, externalId);
            log.info("Successfully updated provider with externalId: {} in external storage for tenantName: {}", externalId, tenantName);
        } catch (Exception e) {
            log.error("Failed to update provider in external storage for tenantName: {}, error: {}, stacktrace: {}", tenantName, e.getMessage(), e);
            throw new RuntimeException("Failed to sync with external storage", e); // Rollback transaction
        }

        // Update database only if external storage succeeded
    // DB synchronization skipped (orgId deprecation phase). Implement if needed.
    }

    @Transactional
    @Override
    public void deleteProvider(String externalId) {
        String tenantName = getCurrentTenantName();
        log.debug("Deleting provider externalId={} for tenantName={}", externalId, tenantName);

        try {
            fhirResourceStorage.delete("Practitioner", externalId);
            log.info("Successfully deleted provider with externalId: {} from external storage for tenantName: {}", externalId, tenantName);
        } catch (Exception e) {
            log.error("Failed to delete provider from external storage for tenantName: {}, error: {}, stacktrace: {}", tenantName, e.getMessage(), e);
            throw new RuntimeException("Failed to sync with external storage", e); // Rollback transaction
        }
        // DB deletion skipped during orgId deprecation.
    }

    @Transactional(readOnly = true)
    @Override
    public List<ProviderDto> searchAllProviders() {
        String tenantName = getCurrentTenantName();
        List<Practitioner> fhirProviders = fhirResourceStorage.searchAll(Practitioner.class);
        log.debug("Retrieved {} FHIR Providers from external storage for tenantName: {}", fhirProviders.size(), tenantName);
        return fhirProviders.stream()
                .map(this::mapFromFhirPractitioner)
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
    log.debug("Entering mapToFhirPractitioner with ProviderDto: id={}, fhirId={} (orgId deprecated)", providerDto.getId(), providerDto.getFhirId());
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
            if (telecom.getSystem() == ContactPoint.ContactPointSystem.EMAIL) {
                contact.setEmail(telecom.getValue());
                log.debug("Mapped email to Contact: {}", telecom.getValue());
            } else if (telecom.getSystem() == ContactPoint.ContactPointSystem.PHONE) {
                if (contact.getPhoneNumber() == null) contact.setPhoneNumber(telecom.getValue());
                else if (contact.getMobileNumber() == null) contact.setMobileNumber(telecom.getValue());
                log.debug("Mapped phone to Contact: {}", telecom.getValue());
            } else if (telecom.getSystem() == ContactPoint.ContactPointSystem.FAX) {
                contact.setFaxNumber(telecom.getValue());
                log.debug("Mapped fax to Contact: {}", telecom.getValue());
            } // other systems ignored for now
        });
        dto.setContact(contact);
        if (!fhirProvider.getQualification().isEmpty()) {
            ProviderDto.ProfessionalDetails professionalDetails = new ProviderDto.ProfessionalDetails();
            professionalDetails.setSpecialty(fhirProvider.getQualificationFirstRep().getCode().getText());
            dto.setProfessionalDetails(professionalDetails);
            log.debug("Mapped specialty to ProfessionalDetails: {}", professionalDetails.getSpecialty());
        }
        dto.setFhirId(fhirProvider.getIdElement().getIdPart());
        // orgId deprecated at context level
        log.debug("Mapped ProviderDto: id={}, fhirId={} (orgId deprecated)", dto.getId(), dto.getFhirId());
        return dto;
    }

    private String getCurrentTenantName() {
        String tenantName = RequestContext.get() != null ? RequestContext.get().getTenantName() : null;
        if (tenantName == null) {
            log.warn("tenantName is null in RequestContext");
        }
        return tenantName;
    }
}