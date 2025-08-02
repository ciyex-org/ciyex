package com.qiaben.ciyex.service.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import com.qiaben.ciyex.config.FhirClientProvider;
import com.qiaben.ciyex.entity.Org;
import com.qiaben.ciyex.service.core.ExternalOrgStorage;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("fhirExternalOrgStorage")
@Slf4j
public class FhirExternalOrgStorage implements ExternalOrgStorage {

    private final FhirClientProvider fhirClientProvider;

    @Autowired
    public FhirExternalOrgStorage(FhirClientProvider fhirClientProvider) {
        this.fhirClientProvider = fhirClientProvider;
    }

    @Override
    public String create(Org localOrg) {
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            Organization fhirOrg = mapToFhir(localOrg);
            return client.create().resource(fhirOrg).execute().getId().getIdPart();
        });
    }

    @Override
    public void update(Org localOrg, String externalId) {
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            Organization fhirOrg = mapToFhir(localOrg);
            fhirOrg.setId(externalId);
            client.update().resource(fhirOrg).execute();
            return null;
        });
    }

    @Override
    public Org get(String externalId) {
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            Organization fhirOrg = client.read().resource(Organization.class).withId(externalId).execute();
            return mapFromFhir(fhirOrg);
        });
    }

    @Override
    public void delete(String externalId) {
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            client.delete().resourceById("Organization", externalId).execute();
            return null;
        });
    }

    private <T> T executeWithRetry(FhirOperation<T> operation) {
        try {
            return operation.execute();
        } catch (FhirClientConnectionException e) {
            if (e.getStatusCode() == 401) {
                // Retry once with a fresh client (which fetches a new token)
                return operation.execute();
            }
            throw e;
        }
    }

    @FunctionalInterface
    private interface FhirOperation<T> {
        T execute();
    }

    private Organization mapToFhir(Org localOrg) {
        Organization fhirOrg = new Organization();
        fhirOrg.setName(localOrg.getOrgName());
        Address address = new Address();
        address.addLine(localOrg.getAddress());
        address.setCity(localOrg.getCity());
        address.setState(localOrg.getState());
        address.setPostalCode(localOrg.getPostalCode());
        address.setCountry(localOrg.getCountry());
        fhirOrg.addAddress(address);
        return fhirOrg;
    }

    private Org mapFromFhir(Organization fhirOrg) {
        Org localOrg = new Org();
        localOrg.setOrgName(fhirOrg.getName());
        if (!fhirOrg.getAddress().isEmpty()) {
            Address addr = fhirOrg.getAddressFirstRep();
            localOrg.setAddress(addr.getLine().stream().map(StringType::getValue).findFirst().orElse(null));
            localOrg.setCity(addr.getCity());
            localOrg.setState(addr.getState());
            localOrg.setPostalCode(addr.getPostalCode());
            localOrg.setCountry(addr.getCountry());
        }
        localOrg.setFhirId(fhirOrg.getIdElement().getIdPart());
        return localOrg;
    }
}