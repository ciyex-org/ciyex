package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import com.qiaben.ciyex.dto.ReferralProviderDto;
import com.qiaben.ciyex.storage.ExternalReferralProviderStorage;
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
@Component("fhirExternalReferralProviderStorage")
@Slf4j
public class FhirExternalReferralProviderStorage implements ExternalReferralProviderStorage {

    private final FhirClientProvider fhirClientProvider;

    @Autowired
    public FhirExternalReferralProviderStorage(FhirClientProvider fhirClientProvider) {
        this.fhirClientProvider = fhirClientProvider;
        log.info("Initializing FhirExternalReferralProviderStorage with FhirClientProvider");
    }

    @Override
    public String create(ReferralProviderDto entityDto) {
        try {
            // Ensure audit fields are set
            if (entityDto.getAudit() == null) {
                entityDto.setAudit(new ReferralProviderDto.Audit());
            }
            entityDto.getAudit().setCreatedDate(java.time.LocalDate.now().toString());
            entityDto.getAudit().setLastModifiedDate(java.time.LocalDate.now().toString());

            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            Organization fhirOrg = mapToFhirOrg(entityDto);

            // Create resource in FHIR
            return client.create().resource(fhirOrg).execute().getId().getIdPart();
        } catch (FhirClientConnectionException e) {
            log.error("FHIR Client connection failed during create operation: {}", e.getMessage());
            throw new RuntimeException("Failed to connect to FHIR server", e);
        }
    }

    @Override
    public void update(ReferralProviderDto entityDto, String externalId) {
        try {
            // Ensure audit fields are set
            if (entityDto.getAudit() == null) {
                entityDto.setAudit(new ReferralProviderDto.Audit());
            }
            entityDto.getAudit().setLastModifiedDate(java.time.LocalDate.now().toString());

            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            Organization fhirOrg = mapToFhirOrg(entityDto);
            fhirOrg.setId(externalId);

            // Update resource in FHIR
            client.update().resource(fhirOrg).execute();
        } catch (FhirClientConnectionException e) {
            log.error("FHIR Client connection failed during update operation: {}", e.getMessage());
            throw new RuntimeException("Failed to connect to FHIR server", e);
        }
    }

    @Override
    public ReferralProviderDto get(String externalId) {
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
    public List<ReferralProviderDto> searchAll() {
        try {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            Bundle bundle = client.search().forResource(Organization.class).returnBundle(Bundle.class).execute();
            List<ReferralProviderDto> referralProviderDtos = new ArrayList<>();
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.getResource() instanceof Organization) {
                    Organization fhirOrg = (Organization) entry.getResource();
                    referralProviderDtos.add(mapFromFhirOrg(fhirOrg));
                    log.debug("Mapped ReferralProviderDto with externalId: {} for orgId: {}", entry.getResource().getIdElement().getIdPart(), entry.getResource().getIdElement().getIdPart());
                }
            }
            return referralProviderDtos;
        } catch (FhirClientConnectionException e) {
            log.error("FHIR Client connection failed during searchAll operation: {}", e.getMessage());
            throw new RuntimeException("Failed to connect to FHIR server", e);
        }
    }

    private Organization mapToFhirOrg(ReferralProviderDto dto) {
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

    private ReferralProviderDto mapFromFhirOrg(Organization fhirOrg) {
        ReferralProviderDto dto = new ReferralProviderDto();
        dto.setName(fhirOrg.getName());
        if (!fhirOrg.getAddress().isEmpty()) {
            Address addr = fhirOrg.getAddressFirstRep();
            dto.setAddress(addr.getLine().stream().map(StringType::getValue).findFirst().orElse(null));
            dto.setCity(addr.getCity());
            dto.setState(addr.getState());
            dto.setPostalCode(addr.getPostalCode());
            dto.setCountry(addr.getCountry());
        }
        dto.setFhirId(fhirOrg.getIdElement().getIdPart());
        return dto;
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return ReferralProviderDto.class.isAssignableFrom(entityType);
    }
}
