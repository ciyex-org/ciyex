package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import com.qiaben.ciyex.dto.core.integration.RequestContext;
import com.qiaben.ciyex.entity.Location;
import com.qiaben.ciyex.entity.Org;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalOrgStorage;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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
    public String create(Org localOrg) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.info("Entering create for orgId: {}, orgName: {}", orgId, localOrg.getOrgName());
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            log.debug("Fetched IGenericClient for orgId: {}", orgId);
            Organization fhirOrg = mapToFhirOrg(localOrg);
            log.debug("Mapped Org to FHIR Organization: {}", fhirOrg.getName());
            String externalId = client.create().resource(fhirOrg).execute().getId().getIdPart();
            log.info("Created Org with externalId: {} for orgId: {}", externalId, orgId);
            return externalId;
        });
    }

    @Override
    public void update(Org localOrg, String externalId) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.info("Entering update for orgId: {}, externalId: {}, orgName: {}", orgId, externalId, localOrg.getOrgName());
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            log.debug("Fetched IGenericClient for orgId: {}", orgId);
            Organization fhirOrg = mapToFhirOrg(localOrg);
            fhirOrg.setId(externalId);
            log.debug("Updating FHIR Organization with id: {}", externalId);
            client.update().resource(fhirOrg).execute();
            log.info("Updated Org with externalId: {} for orgId: {}", externalId, orgId);
            return null;
        });
    }

    @Override
    public Org get(String externalId) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.info("Entering get for orgId: {}, externalId: {}", orgId, externalId);
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            log.debug("Fetched IGenericClient for orgId: {}", orgId);
            Organization fhirOrg = client.read().resource(Organization.class).withId(externalId).execute();
            log.debug("Retrieved FHIR Organization with id: {}", externalId);
            Org org = mapFromFhirOrg(fhirOrg);
            log.info("Retrieved Org with externalId: {} for orgId: {}", externalId, orgId);
            return org;
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

    // Location methods
    @Override
    public String createLocation(Location location) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.info("Entering createLocation for orgId: {}, locationName: {}", orgId, location.getName());
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            log.debug("Fetched IGenericClient for orgId: {}", orgId);
            org.hl7.fhir.r4.model.Location fhirLocation = mapToFhirLocation(location);
            log.debug("Mapped Location to FHIR Location: {}", fhirLocation.getName());
            String externalId = client.create().resource(fhirLocation).execute().getId().getIdPart();
            log.info("Created Location with externalId: {} for orgId: {}", externalId, orgId);
            return externalId;
        });
    }

    @Override
    public void updateLocation(Location location, String externalId) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.info("Entering updateLocation for orgId: {}, externalId: {}, locationName: {}", orgId, externalId, location.getName());
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            log.debug("Fetched IGenericClient for orgId: {}", orgId);
            org.hl7.fhir.r4.model.Location fhirLocation = mapToFhirLocation(location);
            fhirLocation.setId(externalId);
            log.debug("Updating FHIR Location with id: {}", externalId);
            client.update().resource(fhirLocation).execute();
            log.info("Updated Location with externalId: {} for orgId: {}", externalId, orgId);
            return null;
        });
    }

    @Override
    public Location getLocation(String externalId) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.info("Entering getLocation for orgId: {}, externalId: {}", orgId, externalId);
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            log.debug("Fetched IGenericClient for orgId: {}", orgId);
            org.hl7.fhir.r4.model.Location fhirLocation = client.read().resource(org.hl7.fhir.r4.model.Location.class).withId(externalId).execute();
            log.debug("Retrieved FHIR Location with id: {}", externalId);
            Location location = mapFromFhirLocation(fhirLocation);
            log.info("Retrieved Location with externalId: {} for orgId: {}", externalId, orgId);
            return location;
        });
    }

    @Override
    public void deleteLocation(String externalId) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.info("Entering deleteLocation for orgId: {}, externalId: {}", orgId, externalId);
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            log.debug("Fetched IGenericClient for orgId: {}", orgId);
            log.info("Deleting Location with externalId: {} for orgId: {}", externalId, orgId);
            client.delete().resourceById("Location", externalId).execute();
            log.info("Deleted Location with externalId: {} for orgId: {}", externalId, orgId);
            return null;
        });
    }

    // Load all locations for the current tenant
    public List<Location> searchAllLocations() {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.info("Entering searchAllLocations for orgId: {}", orgId);
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            log.debug("Fetched IGenericClient for orgId: {}", orgId);
            if (orgId == null) {
                log.error("No orgId in RequestContext during searchAllLocations");
                throw new IllegalStateException("No orgId in request context");
            }

            // Search for Location resources with the tenant tag
            Bundle bundle = client.search()
                    .forResource(org.hl7.fhir.r4.model.Location.class)
                    .where(new TokenClientParam("_tag").exactly().systemAndCode("http://ciyex.com/tenant", orgId.toString()))
                    .returnBundle(Bundle.class)
                    .execute();
            log.debug("Executed FHIR search, Bundle total: {}, entry count: {}", bundle.getTotal(), bundle.getEntry().size());

            List<Location> locations = new ArrayList<>();
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.getResource() instanceof org.hl7.fhir.r4.model.Location) {
                    org.hl7.fhir.r4.model.Location fhirLocation = (org.hl7.fhir.r4.model.Location) entry.getResource();
                    Location location = mapFromFhirLocation(fhirLocation);
                    locations.add(location);
                    log.debug("Mapped Location with externalId: {} for orgId: {}", location.getExternalId(), orgId);
                } else {
                    log.warn("Unexpected resource type in Bundle entry: {}", entry.getResource().getClass().getName());
                }
            }
            log.info("Retrieved {} locations for orgId: {} after mapping", locations.size(), orgId);
            return locations;
        });
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

    private Organization mapToFhirOrg(Org localOrg) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.debug("Mapping Org to FHIR Organization for orgId: {}, orgName: {}", orgId, localOrg.getOrgName());
        Organization fhirOrg = new Organization();
        fhirOrg.setName(localOrg.getOrgName());
        Address address = new Address();
        address.addLine(localOrg.getAddress());
        address.setCity(localOrg.getCity());
        address.setState(localOrg.getState());
        address.setPostalCode(localOrg.getPostalCode());
        address.setCountry(localOrg.getCountry());
        fhirOrg.addAddress(address);
        log.debug("Mapped FHIR Organization for orgId: {}, name: {}", orgId, fhirOrg.getName());
        return fhirOrg;
    }

    private Org mapFromFhirOrg(Organization fhirOrg) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.debug("Mapping FHIR Organization to Org for orgId: {}, id: {}", orgId, fhirOrg.getIdElement().getIdPart());
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
        log.debug("Mapped Org for orgId: {}, externalId: {}", orgId, localOrg.getFhirId());
        return localOrg;
    }

    private org.hl7.fhir.r4.model.Location mapToFhirLocation(Location location) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.debug("Mapping Location to FHIR Location for orgId: {}, name: {}", orgId, location.getName());
        org.hl7.fhir.r4.model.Location fhirLocation = new org.hl7.fhir.r4.model.Location();
        fhirLocation.setName(location.getName());
        Address address = new Address();
        address.addLine(location.getAddress());
        address.setCity(location.getCity());
        address.setState(location.getState());
        address.setPostalCode(location.getPostalCode());
        address.setCountry(location.getCountry());
        fhirLocation.setAddress(address);
        log.debug("Mapped FHIR Location for orgId: {}, name: {}", orgId, fhirLocation.getName());
        return fhirLocation;
    }

    private Location mapFromFhirLocation(org.hl7.fhir.r4.model.Location fhirLocation) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.debug("Mapping FHIR Location to Location for orgId: {}, id: {}", orgId, fhirLocation.getIdElement().getIdPart());
        Location location = new Location();
        location.setName(fhirLocation.getName());
        if (fhirLocation.getAddress() != null) {
            Address addr = fhirLocation.getAddress();
            location.setAddress(addr.getLine().stream().map(StringType::getValue).findFirst().orElse(null));
            location.setCity(addr.getCity());
            location.setState(addr.getState());
            location.setPostalCode(addr.getPostalCode());
            location.setCountry(addr.getCountry());
        }
        location.setExternalId(fhirLocation.getIdElement().getIdPart());
        log.debug("Mapped Location for orgId: {}, externalId: {}", orgId, location.getExternalId());
        return location;
    }
}