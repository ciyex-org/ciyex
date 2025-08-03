package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import com.qiaben.ciyex.dto.core.integration.RequestContext;
import com.qiaben.ciyex.provider.FhirClientProvider;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class FhirResourceStorage {

    private final FhirClientProvider fhirClientProvider;

    @Autowired
    public FhirResourceStorage(FhirClientProvider fhirClientProvider) {
        this.fhirClientProvider = fhirClientProvider;
    }

    public <R extends IBaseResource> String create(R resource) {
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            return client.create().resource(resource).execute().getId().getIdPart();
        });
    }

    public <R extends IBaseResource> void update(R resource, String externalId) {
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            resource.setId(externalId);
            client.update().resource(resource).execute();
            return null;
        });
    }

    public <R extends IBaseResource> R get(Class<R> resourceType, String externalId) {
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            return client.read().resource(resourceType).withId(externalId).execute();
        });
    }

    public void delete(String resourceType, String externalId) {
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            client.delete().resourceById(resourceType, externalId).execute();
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    public <R extends IBaseResource> List<R> searchAll(Class<R> resourceType) {
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            Long orgId = RequestContext.get().getOrgId();
            if (orgId == null) {
                log.error("No orgId in RequestContext during searchAll");
                throw new IllegalStateException("No orgId in request context");
            }

            Bundle bundle = client.search()
                    .forResource(resourceType)
                    .where(new TokenClientParam("_tag").exactly().systemAndCode("http://ciyex.com/tenant", orgId.toString()))
                    .returnBundle(Bundle.class)
                    .execute();

            log.debug("Executed FHIR search, Bundle total: {}, entry count: {}", bundle.getTotal(), bundle.getEntry().size());

            List<R> resources = new ArrayList<>();
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                IBaseResource resource = entry.getResource();
                if (resource != null && resourceType.isInstance(resource)) {
                    resources.add((R) resource); // Safe cast with type check
                } else {
                    log.warn("Unexpected resource type or null in Bundle entry: {}", resource != null ? resource.getClass().getName() : "null");
                }
            }
            log.info("Retrieved {} resources for orgId: {}", resources.size(), orgId);
            return resources;
        });
    }

    private <T> T executeWithRetry(FhirOperation<T> operation) {
        try {
            return operation.execute();
        } catch (Exception e) {
            log.error("FHIR operation failed: {}", e.getMessage());
            throw e;
        }
    }

    @FunctionalInterface
    private interface FhirOperation<T> {
        T execute();
    }
}