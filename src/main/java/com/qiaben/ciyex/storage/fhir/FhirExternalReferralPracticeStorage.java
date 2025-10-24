package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import com.qiaben.ciyex.dto.ReferralPracticeDto;
import com.qiaben.ciyex.storage.ExternalReferralPracticeStorage;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@StorageType("fhir")
@Component("fhirExternalReferralPracticeStorage")
@Slf4j
public class FhirExternalReferralPracticeStorage implements ExternalReferralPracticeStorage {

    private final FhirClientProvider fhirClientProvider;

    @Autowired
    public FhirExternalReferralPracticeStorage(FhirClientProvider fhirClientProvider) {
        this.fhirClientProvider = fhirClientProvider;
        log.info("Initializing FhirExternalReferralPracticeStorage with FhirClientProvider");
    }

    @Override
    public String create(ReferralPracticeDto entityDto) {
        try {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            Organization fhirOrg = mapToFhirOrg(entityDto);
            return client.create().resource(fhirOrg).execute().getId().getIdPart();
        } catch (FhirClientConnectionException e) {
            log.error("FHIR Client connection failed during create operation: {}", e.getMessage());
            throw new RuntimeException("Failed to connect to FHIR server", e);
        }
    }

    @Override
    public void update(ReferralPracticeDto entityDto, String externalId) {
        try {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            Organization fhirOrg = mapToFhirOrg(entityDto);
            fhirOrg.setId(externalId);
            client.update().resource(fhirOrg).execute();
        } catch (FhirClientConnectionException e) {
            log.error("FHIR Client connection failed during update operation: {}", e.getMessage());
            throw new RuntimeException("Failed to connect to FHIR server", e);
        }
    }

    @Override
    public ReferralPracticeDto get(String externalId) {
        try {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            Organization fhirOrg = client.read().resource(Organization.class).withId(externalId).execute();
            return mapFromFhirOrg(fhirOrg);
        } catch (FhirClientConnectionException e) {
            log.error("FHIR Client connection failed during get operation: {}", e.getMessage());
            throw new RuntimeException("Failed to connect to FHIR server", e);
        }
    }

    @Override
    public void delete(String externalId) {
        try {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            client.delete().resourceById("Organization", externalId).execute();
        } catch (FhirClientConnectionException e) {
            log.error("FHIR Client connection failed during delete operation: {}", e.getMessage());
            throw new RuntimeException("Failed to connect to FHIR server", e);
        }
    }

    @Override
    public List<ReferralPracticeDto> searchAll() {
        try {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            Bundle bundle = client.search().forResource(Organization.class).returnBundle(Bundle.class).execute();
            List<ReferralPracticeDto> referralPracticeDtos = new ArrayList<>();
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.getResource() instanceof Organization) {
                    Organization fhirOrg = (Organization) entry.getResource();
                    ReferralPracticeDto dto = mapFromFhirOrg(fhirOrg);
                    referralPracticeDtos.add(dto);
                    log.debug("Mapped ReferralPracticeDto with externalId: {} for orgId: {}", dto.getFhirId(), entry.getResource().getIdElement().getIdPart());
                } else {
                    log.warn("Unexpected resource type in Bundle entry: {}", entry.getResource().getClass().getName());
                }
            }
            log.info("Retrieved {} referral practices", referralPracticeDtos.size());
            return referralPracticeDtos;
        } catch (FhirClientConnectionException e) {
            log.error("FHIR Client connection failed during searchAll operation: {}", e.getMessage());
            throw new RuntimeException("Failed to connect to FHIR server", e);
        }
    }

    private Organization mapToFhirOrg(ReferralPracticeDto dto) {
        Organization fhirOrg = new Organization();
        fhirOrg.setName(dto.getName());
        Address address = new Address();
        address.addLine(dto.getAddress());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setPostalCode(dto.getPostalCode());
        address.setCountry(dto.getCountry());
        fhirOrg.addAddress(address);
        return fhirOrg;
    }

    private ReferralPracticeDto mapFromFhirOrg(Organization fhirOrg) {
        ReferralPracticeDto dto = new ReferralPracticeDto();
        dto.setName(fhirOrg.getName());
        if (!fhirOrg.getAddress().isEmpty()) {
            Address addr = fhirOrg.getAddressFirstRep();
            dto.setAddress(addr.getLine().stream().map(StringType::getValue).findFirst().orElse(null));  // Corrected here
            dto.setCity(addr.getCity());
            dto.setState(addr.getState());
            dto.setPostalCode(addr.getPostalCode());
            dto.setCountry(addr.getCountry());
        }
        dto.setFhirId(fhirOrg.getIdElement().getIdPart());
        return dto;
    }

    // Implementing the abstract method 'supports' required by ExternalStorage interface
    @Override
    public boolean supports(Class<?> entityType) {
        return ReferralPracticeDto.class.isAssignableFrom(entityType);
    }
}
