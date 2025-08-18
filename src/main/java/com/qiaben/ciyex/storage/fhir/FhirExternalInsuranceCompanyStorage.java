package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import com.qiaben.ciyex.dto.InsuranceCompanyDto;
import com.qiaben.ciyex.storage.ExternalInsuranceCompanyStorage;
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
@Component("fhirExternalInsuranceCompanyStorage")
@Slf4j
public class FhirExternalInsuranceCompanyStorage implements ExternalInsuranceCompanyStorage {

    private final FhirClientProvider fhirClientProvider;

    @Autowired
    public FhirExternalInsuranceCompanyStorage(FhirClientProvider fhirClientProvider) {
        this.fhirClientProvider = fhirClientProvider;
        log.info("Initializing FhirExternalInsuranceCompanyStorage with FhirClientProvider");
    }

    @Override
    public String create(InsuranceCompanyDto entityDto) {
        try {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            Organization fhirOrg = mapToFhirOrg(entityDto);
            return client.create().resource(fhirOrg).execute().getId().getIdPart();
        } catch (FhirClientConnectionException e) {
            log.error("FHIR Client connection failed during create operation: {}", e.getMessage());
            throw new RuntimeException("Failed to connect to FHIR server", e);
        }
    }

    @Override
    public void update(InsuranceCompanyDto entityDto, String externalId) {
        try {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            Organization fhirOrg = mapToFhirOrg(entityDto);
            fhirOrg.setId(externalId);
            client.update().resource(fhirOrg).execute();
        } catch (FhirClientConnectionException e) {
            log.error("FHIR Client connection failed during update operation: {}", e.getMessage());
            throw new RuntimeException("Failed to connect to FHIR server", e);
        }
    }

    @Override
    public InsuranceCompanyDto get(String externalId) {
        try {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
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
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            client.delete().resourceById("Organization", externalId).execute();
        } catch (FhirClientConnectionException e) {
            log.error("FHIR Client connection failed during delete operation: {}", e.getMessage());
            throw new RuntimeException("Failed to connect to FHIR server", e);
        }
    }

    @Override
    public List<InsuranceCompanyDto> searchAll() {
        try {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            Bundle bundle = client.search()
                    .forResource(Organization.class)
                    .returnBundle(Bundle.class)
                    .execute();
            List<InsuranceCompanyDto> insuranceCompanyDtos = new ArrayList<>();
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.getResource() instanceof Organization) {
                    Organization fhirOrg = (Organization) entry.getResource();
                    insuranceCompanyDtos.add(mapFromFhirOrg(fhirOrg));
                }
            }
            return insuranceCompanyDtos;
        } catch (FhirClientConnectionException e) {
            log.error("FHIR Client connection failed during searchAll operation: {}", e.getMessage());
            throw new RuntimeException("Failed to connect to FHIR server", e);
        }
    }

    private Organization mapToFhirOrg(InsuranceCompanyDto dto) {
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

    private InsuranceCompanyDto mapFromFhirOrg(Organization fhirOrg) {
        InsuranceCompanyDto dto = new InsuranceCompanyDto();
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

    // Implementing the supports method from ExternalStorage interface
    @Override
    public boolean supports(Class<?> entityType) {
        return InsuranceCompanyDto.class.isAssignableFrom(entityType);
    }
}
