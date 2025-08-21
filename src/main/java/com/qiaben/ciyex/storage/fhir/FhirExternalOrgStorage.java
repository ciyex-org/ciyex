package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import com.qiaben.ciyex.dto.OrgDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalOrgStorage;
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
@Component("fhirExternalOrgStorage")
@Slf4j
public class FhirExternalOrgStorage implements ExternalOrgStorage {

    private final FhirClientProvider fhirClientProvider;

    @Autowired
    public FhirExternalOrgStorage(FhirClientProvider fhirClientProvider) {
        this.fhirClientProvider = fhirClientProvider;
        log.info("Initializing FhirExternalOrgStorage with FhirClientProvider");
    }

    // Org methods
    @Override
    public String create(OrgDto entityDto) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.info("Entering create for orgId: {}, orgName: {}", orgId, entityDto.getOrgName());
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            log.debug("Fetched IGenericClient for orgId: {}", orgId);
            Organization fhirOrg = mapToFhirOrg(entityDto);
            log.debug("Mapped OrgDto to FHIR Organization: {}", fhirOrg.getName());
            String externalId = client.create().resource(fhirOrg).execute().getId().getIdPart();
            log.info("Created Org with externalId: {} for orgId: {}", externalId, orgId);
            return externalId;
        });
    }

    @Override
    public void update(OrgDto entityDto, String externalId) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.info("Entering update for orgId: {}, externalId: {}, orgName: {}", orgId, externalId, entityDto.getOrgName());
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            log.debug("Fetched IGenericClient for orgId: {}", orgId);
            Organization fhirOrg = mapToFhirOrg(entityDto);
            fhirOrg.setId(externalId);
            log.debug("Updating FHIR Organization with id: {}", externalId);
            client.update().resource(fhirOrg).execute();
            log.info("Updated Org with externalId: {} for orgId: {}", externalId, orgId);
            return null;
        });
    }

    @Override
    public OrgDto get(String externalId) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.info("Entering get for orgId: {}, externalId: {}", orgId, externalId);
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            log.debug("Fetched IGenericClient for orgId: {}", orgId);
            Organization fhirOrg = client.read().resource(Organization.class).withId(externalId).execute();
            log.debug("Retrieved FHIR Organization with id: {}", externalId);
            OrgDto orgDto = mapFromFhirOrg(fhirOrg);
            log.info("Retrieved OrgDto with externalId: {} for orgId: {}", externalId, orgId);
            return orgDto;
        });
    }

    @Override
    public void delete(String externalId) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.info("Entering delete for orgId: {}, externalId: {}", orgId, externalId);
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            log.debug("Fetched IGenericClient for orgId: {}", orgId);
            log.info("Deleting Org with externalId: {} for orgId: {}", externalId, orgId);
            client.delete().resourceById("Organization", externalId).execute();
            log.info("Deleted Org with externalId: {} for orgId: {}", externalId, orgId);
            return null;
        });
    }

    @Override
    public List<OrgDto> searchAll() {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.info("Entering searchAll for orgId: {}", orgId);
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            log.debug("Fetched IGenericClient for orgId: {}", orgId);
            if (orgId == null) {
                log.error("No orgId in RequestContext during searchAll");
                throw new IllegalStateException("No orgId in request context");
            }

            // Search for Organization resources with the tenant tag
            Bundle bundle = client.search()
                    .forResource(Organization.class)
                    .where(new TokenClientParam("_tag").exactly().systemAndCode("http://ciyex.com/tenant", orgId.toString()))
                    .returnBundle(Bundle.class)
                    .execute();
            log.debug("Executed FHIR search, Bundle total: {}, entry count: {}", bundle.getTotal(), bundle.getEntry().size());

            List<OrgDto> orgDtos = new ArrayList<>();
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.getResource() instanceof Organization) {
                    Organization fhirOrg = (Organization) entry.getResource();
                    OrgDto orgDto = mapFromFhirOrg(fhirOrg);
                    orgDtos.add(orgDto);
                    log.debug("Mapped OrgDto with externalId: {} for orgId: {}", orgDto.getFhirId(), orgId);
                } else {
                    log.warn("Unexpected resource type in Bundle entry: {}", entry.getResource().getClass().getName());
                }
            }
            log.info("Retrieved {} orgs for orgId: {} after mapping", orgDtos.size(), orgId);
            return orgDtos;
        });
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return OrgDto.class.isAssignableFrom(entityType);
    }

    private <T> T executeWithRetry(FhirOperation<T> operation) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.debug("Entering executeWithRetry for orgId: {}", orgId);
        try {
            T result = operation.execute();
            log.debug("executeWithRetry succeeded for orgId: {}", orgId);
            return result;
        } catch (FhirClientConnectionException e) {
            log.error("FhirClientConnectionException for orgId: {} with status: {}", orgId, e.getStatusCode(), e);
            if (e.getStatusCode() == 401) {
                log.warn("Received 401, retrying with fresh FHIR client for orgId: {}", orgId);
                return operation.execute();
            }
            throw e;
        } catch (Exception e) {
            log.error("Unexpected exception in executeWithRetry for orgId: {}", orgId, e);
            throw e;
        }
    }

    @FunctionalInterface
    private interface FhirOperation<T> {
        T execute();
    }

    private Organization mapToFhirOrg(OrgDto orgDto) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.debug("Mapping OrgDto to FHIR Organization for orgId: {}, orgName: {}", orgId, orgDto.getOrgName());
        Organization fhirOrg = new Organization();
        fhirOrg.setName(orgDto.getOrgName());
        Address address = new Address();
        address.addLine(orgDto.getAddress());
        address.setCity(orgDto.getCity());
        address.setState(orgDto.getState());
        address.setPostalCode(orgDto.getPostalCode());
        address.setCountry(orgDto.getCountry());
        fhirOrg.addAddress(address);
        log.debug("Mapped FHIR Organization for orgId: {}, name: {}", orgId, fhirOrg.getName());
        return fhirOrg;
    }

    private OrgDto mapFromFhirOrg(Organization fhirOrg) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.debug("Mapping FHIR Organization to OrgDto for orgId: {}, id: {}", orgId, fhirOrg.getIdElement().getIdPart());
        OrgDto orgDto = new OrgDto();
        orgDto.setOrgName(fhirOrg.getName());
        if (!fhirOrg.getAddress().isEmpty()) {
            Address addr = fhirOrg.getAddressFirstRep();
            orgDto.setAddress(addr.getLine().stream().map(StringType::getValue).findFirst().orElse(null));
            orgDto.setCity(addr.getCity());
            orgDto.setState(addr.getState());
            orgDto.setPostalCode(addr.getPostalCode());
            orgDto.setCountry(addr.getCountry());
        }
        orgDto.setFhirId(fhirOrg.getIdElement().getIdPart());
        log.debug("Mapped OrgDto for orgId: {}, externalId: {}", orgId, orgDto.getFhirId());
        return orgDto;
    }
}