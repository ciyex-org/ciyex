package com.qiaben.ciyex.fhir;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirClientService {

    private final IGenericClient fhirClient;

    /**
     * Create a new FHIR resource with partition header for multi-practice support
     */
    public <T extends Resource> MethodOutcome create(T resource, String practiceId) {
        log.debug("Creating FHIR resource {} for practice {}", resource.getResourceType(), practiceId);
        
        return fhirClient.create()
                .resource(resource)
                .withAdditionalHeader("X-Request-Tenant-Id", practiceId)
                .execute();
    }

    /**
     * Read a FHIR resource by ID
     */
    public <T extends IBaseResource> T read(Class<T> resourceClass, String id, String practiceId) {
        log.debug("Reading FHIR resource {} with id {} for practice {}", 
                resourceClass.getSimpleName(), id, practiceId);
        
        return fhirClient.read()
                .resource(resourceClass)
                .withId(id)
                .withAdditionalHeader("X-Request-Tenant-Id", practiceId)
                .execute();
    }

    /**
     * Read a FHIR resource by ID, returning Optional
     */
    public <T extends IBaseResource> Optional<T> readOptional(Class<T> resourceClass, String id, String practiceId) {
        try {
            T resource = read(resourceClass, id, practiceId);
            return Optional.of(resource);
        } catch (ResourceNotFoundException e) {
            log.debug("Resource {} with id {} not found in practice {}", 
                    resourceClass.getSimpleName(), id, practiceId);
            return Optional.empty();
        }
    }

    /**
     * Update an existing FHIR resource
     */
    public <T extends Resource> MethodOutcome update(T resource, String practiceId) {
        log.debug("Updating FHIR resource {} with id {} for practice {}", 
                resource.getResourceType(), resource.getId(), practiceId);
        
        return fhirClient.update()
                .resource(resource)
                .withAdditionalHeader("X-Request-Tenant-Id", practiceId)
                .execute();
    }

    /**
     * Delete a FHIR resource by ID
     */
    public void delete(Class<? extends IBaseResource> resourceClass, String id, String practiceId) {
        log.debug("Deleting FHIR resource {} with id {} for practice {}", 
                resourceClass.getSimpleName(), id, practiceId);
        
        fhirClient.delete()
                .resourceById(resourceClass.getSimpleName(), id)
                .withAdditionalHeader("X-Request-Tenant-Id", practiceId)
                .execute();
    }

    /**
     * Search for FHIR resources with a query builder
     */
    public <T extends IBaseResource> Bundle search(Class<T> resourceClass, String practiceId) {
        return fhirClient.search()
                .forResource(resourceClass)
                .withAdditionalHeader("X-Request-Tenant-Id", practiceId)
                .returnBundle(Bundle.class)
                .execute();
    }

    /**
     * Search for FHIR resources by identifier
     */
    public <T extends IBaseResource> Bundle searchByIdentifier(
            Class<T> resourceClass, 
            String system, 
            String value, 
            String practiceId) {
        
        log.debug("Searching FHIR resource {} by identifier {}|{} for practice {}", 
                resourceClass.getSimpleName(), system, value, practiceId);
        
        return fhirClient.search()
                .forResource(resourceClass)
                .where(new ca.uhn.fhir.rest.gclient.TokenClientParam("identifier")
                        .exactly()
                        .systemAndCode(system, value))
                .withAdditionalHeader("X-Request-Tenant-Id", practiceId)
                .returnBundle(Bundle.class)
                .execute();
    }

    /**
     * Extract resources from a Bundle
     */
    @SuppressWarnings("unchecked")
    public <T extends Resource> List<T> extractResources(Bundle bundle, Class<T> resourceClass) {
        List<T> resources = new ArrayList<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && resourceClass.isInstance(entry.getResource())) {
                    resources.add((T) entry.getResource());
                }
            }
        }
        return resources;
    }

    /**
     * Create or update a resource (upsert)
     */
    public <T extends Resource> MethodOutcome createOrUpdate(T resource, String practiceId) {
        if (resource.hasId()) {
            return update(resource, practiceId);
        } else {
            return create(resource, practiceId);
        }
    }

    /**
     * Get the FHIR client for advanced operations
     */
    public IGenericClient getClient() {
        return fhirClient;
    }
}
