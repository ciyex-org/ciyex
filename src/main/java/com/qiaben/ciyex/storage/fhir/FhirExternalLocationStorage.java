package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import com.qiaben.ciyex.dto.LocationDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
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
        String tenantName = RequestContext.get() != null ? RequestContext.get().getTenantName() : null;
        log.info("Entering create for tenantName: {}, locationName: {}", tenantName, entityDto.getName());
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            log.debug("Fetched IGenericClient for tenantName: {}", tenantName);
            Location fhirLocation = mapToFhirLocation(entityDto);
            log.debug("Mapped LocationDto to FHIR Location: name={}, address={}", fhirLocation.getName(), fhirLocation.getAddress());
            String externalId = client.create().resource(fhirLocation).execute().getId().getIdPart();
            log.info("Created Location with externalId: {} for tenantName: {}", externalId, tenantName);
            return externalId;
        });
    }

    @Override
    public void update(LocationDto entityDto, String externalId) {
        String tenantName = RequestContext.get() != null ? RequestContext.get().getTenantName() : null;
        log.info("Entering update for tenantName: {}, externalId: {}, locationName: {}", tenantName, externalId, entityDto.getName());
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            log.debug("Fetched IGenericClient for tenantName: {}", tenantName);
            Location fhirLocation = mapToFhirLocation(entityDto);
            fhirLocation.setId(externalId);
            log.debug("Updating FHIR Location with id: {}, name={}, address={}", externalId, fhirLocation.getName(), fhirLocation.getAddress());
            client.update().resource(fhirLocation).execute();
            log.info("Updated Location with externalId: {} for tenantName: {}", externalId, tenantName);
            return null;
        });
    }

    @Override
    public LocationDto get(String externalId) {
        String tenantName = RequestContext.get() != null ? RequestContext.get().getTenantName() : null;
        log.info("Entering get for tenantName: {}, externalId: {}", tenantName, externalId);
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            log.debug("Fetched IGenericClient for tenantName: {}", tenantName);
            Location fhirLocation = client.read().resource(Location.class).withId(externalId).execute();
            log.debug("Retrieved FHIR Location with id: {}, name={}, address={}", externalId, fhirLocation.getName(), fhirLocation.getAddress());
            LocationDto locationDto = mapFromFhirLocation(fhirLocation);
            log.info("Retrieved LocationDto with externalId: {}, tenantName: {}", externalId, tenantName);
            log.debug("Mapped LocationDto: externalId={}, name={}, address={}", locationDto.getExternalId(), locationDto.getName(), locationDto.getAddress());
            return locationDto;
        });
    }

    @Override
    public void delete(String externalId) {
        String tenantName = RequestContext.get() != null ? RequestContext.get().getTenantName() : null;
        log.info("Entering delete for tenantName: {}, externalId: {}", tenantName, externalId);
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            log.debug("Fetched IGenericClient for tenantName: {}", tenantName);
            log.info("Deleting Location with externalId: {} for tenantName: {}", externalId, tenantName);
            client.delete().resourceById("Location", externalId).execute();
            log.info("Deleted Location with externalId: {} for tenantName: {}", externalId, tenantName);
            return null;
        });
    }

    @Override
    public List<LocationDto> searchAll() {
        String tenantName = RequestContext.get() != null ? RequestContext.get().getTenantName() : null;
        log.info("Entering searchAll for tenantName: {}", tenantName);
        if (tenantName == null) {
            log.warn("tenantName is null in RequestContext, defaulting to no filtering");
        }

        Bundle bundle = fhirClientProvider.getForCurrentTenant().search()
                .forResource(org.hl7.fhir.r4.model.Location.class)
                .where(new TokenClientParam("_tag").exactly().systemAndCode("http://ciyex.com/tenant", tenantName != null ? tenantName : ""))
                .returnBundle(Bundle.class)
                .execute();

        log.debug("Received Bundle with {} entries for tenantName: {}", bundle.getEntry().size(), tenantName);
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
                    dto.setTenantName(tenantName); // Set tenantName from RequestContext
                    log.debug("Mapped LocationDto: externalId={}, name={}, address={}", dto.getExternalId(), dto.getName(), dto.getAddress());
                    return dto;
                })
                .collect(Collectors.toList());

        log.info("Retrieved {} locations for tenantName: {} after mapping", locationDtos.size(), tenantName);
        return locationDtos;
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return LocationDto.class.isAssignableFrom(entityType);
    }

    private <T> T executeWithRetry(FhirOperation<T> operation) {
        String tenantName = RequestContext.get() != null ? RequestContext.get().getTenantName() : null;
        log.debug("Entering executeWithRetry for tenantName: {}", tenantName);
        try {
            T result = operation.execute();
            log.debug("executeWithRetry succeeded for tenantName: {}", tenantName);
            return result;
        } catch (FhirClientConnectionException e) {
            log.error("FhirClientConnectionException for tenantName: {} with status: {}, message: {}", tenantName, e.getStatusCode(), e.getMessage());
            if (e.getStatusCode() == 401) {
                log.warn("Received 401, retrying with fresh FHIR client for tenantName: {}", tenantName);
                return operation.execute();
            }
            throw e;
        } catch (Exception e) {
            log.error("Unexpected exception in executeWithRetry for tenantName: {}, message: {}, stacktrace: {}", tenantName, e.getMessage(), e);
            throw e;
        }
    }

    @FunctionalInterface
    private interface FhirOperation<T> {
        T execute();
    }

    private Location mapToFhirLocation(LocationDto locationDto) {
        String tenantName = RequestContext.get() != null ? RequestContext.get().getTenantName() : null;
        log.debug("Mapping LocationDto to FHIR Location for tenantName: {}, name: {}, address: {}", tenantName, locationDto.getName(), locationDto.getAddress());
        Location fhirLocation = new Location();
        fhirLocation.setName(locationDto.getName());
        Address address = new Address();
        if (locationDto.getAddress() != null) address.addLine(locationDto.getAddress());
        address.setCity(locationDto.getCity());
        address.setState(locationDto.getState());
        address.setPostalCode(locationDto.getPostalCode());
        address.setCountry(locationDto.getCountry());
        fhirLocation.setAddress(address);
        log.debug("Mapped FHIR Location for tenantName: {}, name: {}, address: {}", tenantName, fhirLocation.getName(), fhirLocation.getAddress());
        return fhirLocation;
    }

    private LocationDto mapFromFhirLocation(Location fhirLocation) {
        String tenantName = RequestContext.get() != null ? RequestContext.get().getTenantName() : null;
        log.debug("Mapping FHIR Location to LocationDto for tenantName: {}, id: {}, name: {}, address: {}", tenantName, fhirLocation.getIdElement().getIdPart(), fhirLocation.getName(), fhirLocation.getAddress());
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
        // orgId deprecated; tenantName available via RequestContext if needed
        log.debug("Mapped LocationDto for tenantName: {}, externalId: {}, name: {}, address: {}", tenantName, dto.getExternalId(), dto.getName(), dto.getAddress());
        return dto;
    }
}