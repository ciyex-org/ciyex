package org.ciyex.ehr.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.IdType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private final FhirBearerTokenInterceptor bearerTokenInterceptor;

    // Cache clients per partition to avoid recreating them
    private final ConcurrentHashMap<String, IGenericClient> clientCache = new ConcurrentHashMap<>();

    public FhirClientService(
            FhirContext fhirContext,
            FhirBearerTokenInterceptor bearerTokenInterceptor,
            @Value("${fhir.server.url}") String baseServerUrl,
            @Value("${fhir.client.socket-timeout:60000}") int socketTimeout,
            @Value("${fhir.client.connect-timeout:10000}") int connectTimeout) {
        this.fhirContext = fhirContext;
        this.bearerTokenInterceptor = bearerTokenInterceptor;
        this.baseServerUrl = baseServerUrl.endsWith("/") ? baseServerUrl.substring(0, baseServerUrl.length() - 1) : baseServerUrl;
        this.socketTimeout = socketTimeout;
        this.connectTimeout = connectTimeout;
    }

    /**
     * Get or create a FHIR client for the specified partition (org alias).
     * URL format: {baseServerUrl}/{orgAlias}
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

            IGenericClient client = fhirContext.newRestfulGenericClient(partitionUrl);
            client.registerInterceptor(bearerTokenInterceptor);
            return client;
        });
    }

    /**
     * Returns the set of org aliases that have active FHIR clients.
     * Used by scheduled tasks that need to iterate over all known partitions.
     */
    public Set<String> getKnownPartitions() {
        return Set.copyOf(clientCache.keySet());
    }

    // ==================== CRUD Operations ====================

    public <T extends Resource> MethodOutcome create(T resource, String orgAlias) {
        log.debug("Creating FHIR resource {} in partition {}", resource.getResourceType(), orgAlias);
        return getClientForPartition(orgAlias).create()
                .resource(resource)
                .execute();
    }

    public <T extends IBaseResource> T read(Class<T> resourceClass, String id, String orgAlias) {
        log.debug("Reading FHIR resource {} with id {} from partition {}",
                resourceClass.getSimpleName(), id, orgAlias);
        return getClientForPartition(orgAlias).read()
                .resource(resourceClass)
                .withId(id)
                .execute();
    }

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

    public IBaseResource readByResourceName(String resourceName, String id, String orgAlias) {
        log.debug("Reading FHIR resource {} with id {} from partition {}",
                resourceName, id, orgAlias);
        return getClientForPartition(orgAlias).read()
                .resource(resourceName)
                .withId(id)
                .execute();
    }

    public <T extends Resource> MethodOutcome update(T resource, String orgAlias) {
        log.debug("Updating FHIR resource {} with id {} in partition {}",
                resource.getResourceType(), resource.getId(), orgAlias);
        return getClientForPartition(orgAlias).update()
                .resource(resource)
                .execute();
    }

    public void delete(Class<? extends IBaseResource> resourceClass, String id, String orgAlias) {
        log.debug("Deleting FHIR resource {} with id {} from partition {}",
                resourceClass.getSimpleName(), id, orgAlias);
        getClientForPartition(orgAlias).delete()
                .resourceById(resourceClass.getSimpleName(), id)
                .execute();
    }

    public void deleteByResourceName(String resourceName, String id, String orgAlias) {
        log.debug("Deleting FHIR resource {} with id {} from partition {}",
                resourceName, id, orgAlias);
        IGenericClient client = getClientForPartition(orgAlias);
        client.delete()
                .resourceById(resourceName, id)
                .execute();

        // Best-effort expunge to hard-delete (requires $expunge to be enabled on the FHIR server)
        try {
            Parameters expungeParams = new Parameters();
            expungeParams.addParameter("expungeDeletedResources", true);
            expungeParams.addParameter("limit", 1);
            client.operation()
                    .onInstance(new IdType(resourceName, id))
                    .named("$expunge")
                    .withParameters(expungeParams)
                    .execute();
        } catch (Exception e) {
            log.debug("Expunge not available for {} {}: {}", resourceName, id, e.getMessage());
        }
    }

    public <T extends Resource> MethodOutcome createOrUpdate(T resource, String orgAlias) {
        if (resource.hasId()) {
            return update(resource, orgAlias);
        } else {
            return create(resource, orgAlias);
        }
    }

    // ==================== Search Operations ====================

    /**
     * Search with pagination — returns a single page Bundle.
     * Uses FHIR _count and next-link paging (skips pages via link following).
     */
    public <T extends IBaseResource> Bundle searchPaged(Class<T> resourceClass, String orgAlias, int count, int offset) {
        // First request — get page with _count
        Bundle bundle = getClientForPartition(orgAlias).search()
                .forResource(resourceClass)
                .count(count)
                .cacheControl(new ca.uhn.fhir.rest.api.CacheControlDirective().setNoCache(true))
                .returnBundle(Bundle.class)
                .execute();

        // Skip pages to reach the requested offset
        int pagesToSkip = offset / count;
        for (int i = 0; i < pagesToSkip && bundle.getLink(Bundle.LINK_NEXT) != null; i++) {
            bundle = getClientForPartition(orgAlias).loadPage()
                    .next(bundle)
                    .execute();
        }

        return bundle;
    }

    /**
     * Search all — loads all pages by following next links.
     */
    public <T extends IBaseResource> Bundle search(Class<T> resourceClass, String orgAlias) {
        Bundle bundle = getClientForPartition(orgAlias).search()
                .forResource(resourceClass)
                .count(1000)
                .cacheControl(new ca.uhn.fhir.rest.api.CacheControlDirective().setNoCache(true))
                .returnBundle(Bundle.class)
                .execute();

        // Follow next links to load all pages
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
     * Search with additional query parameters (single page).
     */
    public <T extends IBaseResource> Bundle searchWithParams(
            Class<T> resourceClass, String orgAlias, Map<String, String> params) {
        var query = getClientForPartition(orgAlias).search()
                .forResource(resourceClass)
                .cacheControl(new ca.uhn.fhir.rest.api.CacheControlDirective().setNoCache(true))
                .returnBundle(Bundle.class);
        for (var e : params.entrySet()) {
            query = query.where(new ca.uhn.fhir.rest.gclient.StringClientParam(e.getKey()).matches().value(e.getValue()));
        }
        return query.execute();
    }

    /**
     * Search by code — loads all pages.
     */
    public <T extends IBaseResource> Bundle searchByCode(
            Class<T> resourceClass, String system, String code, String orgAlias) {

        log.debug("Searching FHIR resource {} by code {}|{} in partition {}",
                resourceClass.getSimpleName(), system, code, orgAlias);

        Bundle bundle = getClientForPartition(orgAlias).search()
                .forResource(resourceClass)
                .where(new ca.uhn.fhir.rest.gclient.TokenClientParam("code")
                        .exactly()
                        .systemAndCode(system, code))
                .count(200)
                .returnBundle(Bundle.class)
                .execute();

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
     * Search by identifier — loads all pages.
     */
    public <T extends IBaseResource> Bundle searchByIdentifier(
            Class<T> resourceClass, String system, String value, String orgAlias) {

        log.debug("Searching FHIR resource {} by identifier {}|{} in partition {}",
                resourceClass.getSimpleName(), system, value, orgAlias);

        Bundle bundle = getClientForPartition(orgAlias).search()
                .forResource(resourceClass)
                .where(new ca.uhn.fhir.rest.gclient.TokenClientParam("identifier")
                        .exactly()
                        .systemAndCode(system, value))
                .count(200)
                .returnBundle(Bundle.class)
                .execute();

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

    // ==================== Helpers ====================

    /**
     * Load a page by URL (for pagination).
     */
    public Bundle loadPage(String url, String orgAlias) {
        log.debug("Loading page from URL: {} in partition {}", url, orgAlias);
        return getClientForPartition(orgAlias).loadPage()
                .byUrl(url)
                .andReturnBundle(Bundle.class)
                .execute();
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
     * Get the FHIR client for a specific partition (org alias).
     * Use this for advanced operations.
     */
    public IGenericClient getClient(String orgAlias) {
        return getClientForPartition(orgAlias);
    }
}
