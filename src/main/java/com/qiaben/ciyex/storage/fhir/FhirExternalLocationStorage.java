package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import com.qiaben.ciyex.dto.LocationDto;
import com.qiaben.ciyex.dto.core.integration.RequestContext;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalLocationStorage;
import com.qiaben.ciyex.storage.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@StorageType("fhir")
@Component("fhirExternalLocationStorage")
@Slf4j
public class FhirExternalLocationStorage implements ExternalLocationStorage {

    private final FhirClientProvider fhirClientProvider;

    @Autowired
    public FhirExternalLocationStorage(FhirClientProvider fhirClientProvider) {
        this.fhirClientProvider = fhirClientProvider;
        log.info("Initializing FhirExternalLocationStorage with FhirClientProvider");
    }

    @Override
    public String create(LocationDto entityDto) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.info("Entering create for orgId: {}, locationName: {}", orgId, entityDto.getName());
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            log.debug("Fetched IGenericClient for orgId: {}", orgId);
            Location fhirLocation = mapToFhirLocation(entityDto);
            log.debug("Mapped LocationDto to FHIR Location: name={}, address={}", fhirLocation.getName(), fhirLocation.getAddress());
            String externalId = client.create().resource(fhirLocation).execute().getId().getIdPart();
            log.info("Created Location with externalId: {} for orgId: {}", externalId, orgId);
            return externalId;
        });
    }

    @Override
    public void update(LocationDto entityDto, String externalId) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.info("Entering update for orgId: {}, externalId: {}, locationName: {}", orgId, externalId, entityDto.getName());
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            log.debug("Fetched IGenericClient for orgId: {}", orgId);
            Location fhirLocation = mapToFhirLocation(entityDto);
            fhirLocation.setId(externalId);
            log.debug("Updating FHIR Location with id: {}, name={}, address={}", externalId, fhirLocation.getName(), fhirLocation.getAddress());
            client.update().resource(fhirLocation).execute();
            log.info("Updated Location with externalId: {} for orgId: {}", externalId, orgId);
            return null;
        });
    }

    @Override
    public LocationDto get(String externalId) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.info("Entering get for orgId: {}, externalId: {}", orgId, externalId);
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            log.debug("Fetched IGenericClient for orgId: {}", orgId);
            Location fhirLocation = client.read().resource(Location.class).withId(externalId).execute();
            log.debug("Retrieved FHIR Location with id: {}, name={}, address={}", externalId, fhirLocation.getName(), fhirLocation.getAddress());
            LocationDto locationDto = mapFromFhirLocation(fhirLocation);
            log.info("Retrieved LocationDto with externalId: {}, orgId: {}", externalId, orgId);
            log.debug("Mapped LocationDto: externalId={}, name={}, address={}", locationDto.getExternalId(), locationDto.getName(), locationDto.getAddress());
            return locationDto;
        });
    }

    @Override
    public void delete(String externalId) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.info("Entering delete for orgId: {}, externalId: {}", orgId, externalId);
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            log.debug("Fetched IGenericClient for orgId: {}", orgId);
            log.info("Deleting Location with externalId: {} for orgId: {}", externalId, orgId);
            client.delete().resourceById("Location", externalId).execute();
            log.info("Deleted Location with externalId: {} for orgId: {}", externalId, orgId);
            return null;
        });
    }

    @Override
    public List<LocationDto> searchAll() {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.info("Entering searchAll for orgId: {}", orgId);
        if (orgId == null) {
            log.warn("orgId is null in RequestContext, defaulting to no filtering");
        }

        Bundle bundle = fhirClientProvider.getForCurrentOrg().search()
                .forResource(org.hl7.fhir.r4.model.Location.class)
                .where(new TokenClientParam("_tag").exactly().systemAndCode("http://ciyex.com/tenant", orgId != null ? orgId.toString() : ""))
                .returnBundle(Bundle.class)
                .execute();

        log.debug("Received Bundle with {} entries for orgId: {}", bundle.getEntry().size(), orgId);
        List<LocationDto> locationDtos = bundle.getEntry().stream()
                .map(entry -> {
                    Location location = (Location) entry.getResource();
                    log.debug("Processing Location entry: id={}, name={}", location.getIdElement().getIdPart(), location.getName());
                    LocationDto dto = new LocationDto();
                    dto.setExternalId(location.getIdElement().getIdPart());
                    dto.setName(location.getName());
                    dto.setAddress(location.getAddress() != null ? location.getAddress().getLine().stream().findFirst().map(StringType::getValue).orElse(null) : null);
                    dto.setCity(location.getAddress() != null ? location.getAddress().getCity() : null);
                    dto.setState(location.getAddress() != null ? location.getAddress().getState() : null);
                    dto.setPostalCode(location.getAddress() != null ? location.getAddress().getPostalCode() : null);
                    dto.setCountry(location.getAddress() != null ? location.getAddress().getCountry() : null);
                    dto.setOrgId(orgId); // Set orgId from RequestContext
                    log.debug("Mapped LocationDto: externalId={}, name={}, address={}", dto.getExternalId(), dto.getName(), dto.getAddress());
                    return dto;
                })
                .collect(Collectors.toList());

        log.info("Retrieved {} locations for orgId: {} after mapping", locationDtos.size(), orgId);
        return locationDtos;
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return LocationDto.class.isAssignableFrom(entityType);
    }

    private <T> T executeWithRetry(FhirOperation<T> operation) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.debug("Entering executeWithRetry for orgId: {}", orgId);
        try {
            T result = operation.execute();
            log.debug("executeWithRetry succeeded for orgId: {}", orgId);
            return result;
        } catch (FhirClientConnectionException e) {
            log.error("FhirClientConnectionException for orgId: {} with status: {}, message: {}", orgId, e.getStatusCode(), e.getMessage());
            if (e.getStatusCode() == 401) {
                log.warn("Received 401, retrying with fresh FHIR client for orgId: {}", orgId);
                return operation.execute();
            }
            throw e;
        } catch (Exception e) {
            log.error("Unexpected exception in executeWithRetry for orgId: {}, message: {}, stacktrace: {}", orgId, e.getMessage(), e);
            throw e;
        }
    }

    @FunctionalInterface
    private interface FhirOperation<T> {
        T execute();
    }

    private Location mapToFhirLocation(LocationDto locationDto) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.debug("Mapping LocationDto to FHIR Location for orgId: {}, name: {}, address: {}", orgId, locationDto.getName(), locationDto.getAddress());
        Location fhirLocation = new Location();
        fhirLocation.setName(locationDto.getName());
        Address address = new Address();
        if (locationDto.getAddress() != null) address.addLine(locationDto.getAddress());
        address.setCity(locationDto.getCity());
        address.setState(locationDto.getState());
        address.setPostalCode(locationDto.getPostalCode());
        address.setCountry(locationDto.getCountry());
        fhirLocation.setAddress(address);
        log.debug("Mapped FHIR Location for orgId: {}, name: {}, address: {}", orgId, fhirLocation.getName(), fhirLocation.getAddress());
        return fhirLocation;
    }

    private LocationDto mapFromFhirLocation(Location fhirLocation) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.debug("Mapping FHIR Location to LocationDto for orgId: {}, id: {}, name: {}, address: {}", orgId, fhirLocation.getIdElement().getIdPart(), fhirLocation.getName(), fhirLocation.getAddress());
        LocationDto dto = new LocationDto();
        dto.setExternalId(fhirLocation.getIdElement().getIdPart());
        dto.setName(fhirLocation.getName());
        if (fhirLocation.getAddress() != null) {
            Address addr = fhirLocation.getAddress();
            dto.setAddress(addr.getLine().stream().map(StringType::getValue).findFirst().orElse(null));
            dto.setCity(addr.getCity());
            dto.setState(addr.getState());
            dto.setPostalCode(addr.getPostalCode());
            dto.setCountry(addr.getCountry());
        }
        dto.setOrgId(orgId); // Set orgId from RequestContext
        log.debug("Mapped LocationDto for orgId: {}, externalId: {}, name: {}, address: {}", orgId, dto.getExternalId(), dto.getName(), dto.getAddress());
        return dto;
    }
}