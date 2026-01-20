package com.qiaben.ciyex.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.IdType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FHIR Client Service with URL path-based partitioning.
 * Each practice/org has its own partition: /fhir/{org-alias}/Patient
 * Example: /fhir/sunrise-family-medicine/Patient
 */
@Service
@Slf4j
public class FhirClientService {

    private final FhirContext fhirContext;
    private final String baseServerUrl;
    private final int socketTimeout;
    private final int connectTimeout;
    
    // Cache clients per partition to avoid recreating them
    private final ConcurrentHashMap<String, IGenericClient> clientCache = new ConcurrentHashMap<>();

    public FhirClientService(
            FhirContext fhirContext,
            @Value("${fhir.server.url}") String baseServerUrl,
            @Value("${fhir.client.socket-timeout:60000}") int socketTimeout,
            @Value("${fhir.client.connect-timeout:10000}") int connectTimeout) {
        this.fhirContext = fhirContext;
        this.baseServerUrl = baseServerUrl.endsWith("/") ? baseServerUrl.substring(0, baseServerUrl.length() - 1) : baseServerUrl;
        this.socketTimeout = socketTimeout;
        this.connectTimeout = connectTimeout;
    }

    /**
     * Get or create a FHIR client for the specified partition (org alias).
     * URL format: {baseServerUrl}/{orgAlias}
     * Example: https://fhir.apps-dev.in.hinisoft.com/fhir/sunrise-family-medicine
     */
    private IGenericClient getClientForPartition(String orgAlias) {
        if (orgAlias == null) {
            orgAlias = "";
        }
        return clientCache.computeIfAbsent(orgAlias, alias -> {
            String partitionUrl = alias.isEmpty() ? baseServerUrl : baseServerUrl + "/" + alias;
            log.debug("Creating FHIR client for partition: {}", partitionUrl);
            
            fhirContext.getRestfulClientFactory().setSocketTimeout(socketTimeout);
            fhirContext.getRestfulClientFactory().setConnectTimeout(connectTimeout);
            fhirContext.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
            
            return fhirContext.newRestfulGenericClient(partitionUrl);
        });
    }

    /**
     * Create a new FHIR resource in the partition for the specified org alias.
     * URL: /fhir/{orgAlias}/{ResourceType}
     */
    public <T extends Resource> MethodOutcome create(T resource, String orgAlias) {
        log.debug("Creating FHIR resource {} in partition {}", resource.getResourceType(), orgAlias);
        
        return getClientForPartition(orgAlias).create()
                .resource(resource)
                .execute();
    }

    /**
     * Read a FHIR resource by ID from the specified partition.
     */
    public <T extends IBaseResource> T read(Class<T> resourceClass, String id, String orgAlias) {
        log.debug("Reading FHIR resource {} with id {} from partition {}", 
                resourceClass.getSimpleName(), id, orgAlias);
        
        return getClientForPartition(orgAlias).read()
                .resource(resourceClass)
                .withId(id)
                .execute();
    }

    /**
     * Read a FHIR resource by ID, returning Optional.
     */
    public <T extends IBaseResource> Optional<T> readOptional(Class<T> resourceClass, String id, String orgAlias) {
        try {
            T resource = read(resourceClass, id, orgAlias);
            return Optional.of(resource);
        } catch (ResourceNotFoundException e) {
            log.debug("Resource {} with id {} not found in partition {}", 
                    resourceClass.getSimpleName(), id, orgAlias);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Error reading resource {} with id {} from partition {}: {}", 
                    resourceClass.getSimpleName(), id, orgAlias, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Update an existing FHIR resource in the specified partition.
     */
    public <T extends Resource> MethodOutcome update(T resource, String orgAlias) {
        log.debug("Updating FHIR resource {} with id {} in partition {}", 
                resource.getResourceType(), resource.getId(), orgAlias);
        
        return getClientForPartition(orgAlias).update()
                .resource(resource)
                .execute();
    }

    /**
     * Delete a FHIR resource by ID from the specified partition.
     */
    public void delete(Class<? extends IBaseResource> resourceClass, String id, String orgAlias) {
        log.debug("Deleting FHIR resource {} with id {} from partition {}", 
                resourceClass.getSimpleName(), id, orgAlias);
        
        getClientForPartition(orgAlias).delete()
                .resourceById(resourceClass.getSimpleName(), id)
                .execute();
    }

    /**
     * Search for FHIR resources in the specified partition with pagination support.
     */
    public <T extends IBaseResource> Bundle search(Class<T> resourceClass, String orgAlias) {
        Bundle bundle = getClientForPartition(orgAlias).search()
                .forResource(resourceClass)
                .count(1000)
                .returnBundle(Bundle.class)
                .execute();
        
        // Load all pages
        Bundle firstBundle = bundle;
        while (bundle.getLink(Bundle.LINK_NEXT) != null) {
            Bundle nextPage = getClientForPartition(orgAlias).loadPage()
                    .next(bundle)
                    .execute();
            if (nextPage.hasEntry()) {
                firstBundle.getEntry().addAll(nextPage.getEntry());
            }
            bundle = nextPage;
        }
        
        return firstBundle;
    }

    /**
     * Search for FHIR resources by code in the specified partition with pagination.
     */
    public <T extends IBaseResource> Bundle searchByCode(
            Class<T> resourceClass,
            String system,
            String code,
            String orgAlias) {
        
        log.debug("Searching FHIR resource {} by code {}|{} in partition {}",
                resourceClass.getSimpleName(), system, code, orgAlias);
        
        Bundle bundle = getClientForPartition(orgAlias).search()
                .forResource(resourceClass)
                .where(new ca.uhn.fhir.rest.gclient.TokenClientParam("code")
                        .exactly()
                        .systemAndCode(system, code))
                .count(1000)
                .cacheControl(new ca.uhn.fhir.rest.api.CacheControlDirective().setNoCache(true).setNoStore(true))
                .returnBundle(Bundle.class)
                .execute();
        
        // Load all pages
        Bundle firstBundle = bundle;
        while (bundle.getLink(Bundle.LINK_NEXT) != null) {
            Bundle nextPage = getClientForPartition(orgAlias).loadPage()
                    .next(bundle)
                    .execute();
            if (nextPage.hasEntry()) {
                firstBundle.getEntry().addAll(nextPage.getEntry());
            }
            bundle = nextPage;
        }
        
        return firstBundle;
    }

    /**
     * Search for FHIR resources by identifier in the specified partition with pagination.
     */
    public <T extends IBaseResource> Bundle searchByIdentifier(
            Class<T> resourceClass, 
            String system, 
            String value, 
            String orgAlias) {
        
        log.debug("Searching FHIR resource {} by identifier {}|{} in partition {}", 
                resourceClass.getSimpleName(), system, value, orgAlias);
        
        Bundle bundle = getClientForPartition(orgAlias).search()
                .forResource(resourceClass)
                .where(new ca.uhn.fhir.rest.gclient.TokenClientParam("identifier")
                        .exactly()
                        .systemAndCode(system, value))
                .count(1000)
                .cacheControl(new ca.uhn.fhir.rest.api.CacheControlDirective().setNoCache(true).setNoStore(true))
                .returnBundle(Bundle.class)
                .execute();
        
        // Load all pages
        Bundle firstBundle = bundle;
        while (bundle.getLink(Bundle.LINK_NEXT) != null) {
            Bundle nextPage = getClientForPartition(orgAlias).loadPage()
                    .next(bundle)
                    .execute();
            if (nextPage.hasEntry()) {
                firstBundle.getEntry().addAll(nextPage.getEntry());
            }
            bundle = nextPage;
        }
        
        return firstBundle;
    }


    /**
     * Extract resources from a Bundle.
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
     * Create or update a resource (upsert) in the specified partition.
     */
    public <T extends Resource> MethodOutcome createOrUpdate(T resource, String orgAlias) {
        if (resource.hasId()) {
            return update(resource, orgAlias);
        } else {
            return create(resource, orgAlias);
        }
    }

    /**
     * Get the FHIR client for a specific partition (org alias).
     * Use this for advanced operations.
     */
    public IGenericClient getClient(String orgAlias) {
        return getClientForPartition(orgAlias);
    }

    /**
     * Clear the client cache for a specific partition.
     * Use this after creating/updating resources to ensure fresh search results.
     */
    public void clearCache(String orgAlias) {
        if (orgAlias != null) {
            clientCache.remove(orgAlias);
            log.debug("Cleared FHIR client cache for partition: {}", orgAlias);
        }
    }
}
