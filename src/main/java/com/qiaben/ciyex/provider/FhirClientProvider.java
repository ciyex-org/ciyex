//package com.qiaben.ciyex.provider;
//
//import ca.uhn.fhir.context.FhirContext;
//import ca.uhn.fhir.rest.client.api.IGenericClient;
//import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
//import com.qiaben.ciyex.dto.integration.FhirConfig;
//import com.qiaben.ciyex.dto.integration.IntegrationKey;
//import com.qiaben.ciyex.dto.integration.RequestContext;
//import com.qiaben.ciyex.interceptor.FhirTenantInterceptor;
//import com.qiaben.ciyex.storage.fhir.FhirAuthService;
//import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import java.util.UUID;
//
//@Component
//@Slf4j
//public class FhirClientProvider {
//
//    private final OrgIntegrationConfigProvider configProvider;
//    private final FhirAuthService fhirAuthService;
//    private final FhirContext fhirContext; // Inject FhirContext instead of ObjectMapper
//
//    @Autowired
//    public FhirClientProvider(OrgIntegrationConfigProvider configProvider, FhirAuthService fhirAuthService, FhirContext fhirContext) {
//        this.configProvider = configProvider;
//        this.fhirAuthService = fhirAuthService;
//        this.fhirContext = fhirContext;
//        log.info("Initialized FhirClientProvider with dependencies");
//    }
//
//    public IGenericClient getForCurrentOrg() {
//        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
//        log.info("Entering getForCurrentOrg for orgId: {}", orgId);
//        if (orgId == null) throw new IllegalStateException("No orgId in request context");
//        FhirConfig config = configProvider.getForCurrentOrg(IntegrationKey.FHIR);
//        if (config == null) throw new RuntimeException("No FHIR configuration found for the current organization");
//        String accessToken = fhirAuthService.getCachedAccessToken();
//        log.info("Obtained access token and FhirConfig for orgId: {}, apiUrl: {}", orgId, config.getApiUrl());
//
//        log.info("Creating new FHIR context for orgId: {}", orgId);
//        IGenericClient client = fhirContext.newRestfulGenericClient(config.getApiUrl());
//        log.info("Created IGenericClient for orgId: {}, apiUrl: {}, client hash: {}", orgId, config.getApiUrl(), Integer.toHexString(client.hashCode()));
//        client.registerInterceptor(new BearerTokenAuthInterceptor(accessToken));
//        log.info("Registered BearerTokenAuthInterceptor with token for orgId: {}, client hash: {}", orgId, Integer.toHexString(client.hashCode()));
//
//        String interceptorId = UUID.randomUUID().toString();
//        FhirTenantInterceptor tenantInterceptor = new FhirTenantInterceptor(orgId, client, interceptorId, fhirContext);
//        client.registerInterceptor(tenantInterceptor);
//        log.info("Registered FhirTenantInterceptor with id: {}, orgId: {}, client hash: {}", interceptorId, orgId, Integer.toHexString(client.hashCode()));
//        log.info("Exiting getForCurrentOrg with valid client for orgId: {}, client hash: {}", orgId, Integer.toHexString(client.hashCode()));
//        return client;
//    }
//
//
//}

package com.qiaben.ciyex.provider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import com.qiaben.ciyex.dto.integration.FhirConfig;
import com.qiaben.ciyex.dto.integration.IntegrationKey;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.interceptor.FhirTenantInterceptor;
import com.qiaben.ciyex.storage.fhir.FhirAuthService;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class FhirClientProvider {

    private final OrgIntegrationConfigProvider configProvider;
    private final FhirAuthService fhirAuthService;
    private final FhirContext fhirContext = FhirContext.forR4();

    public FhirClientProvider(OrgIntegrationConfigProvider configProvider,
                              FhirAuthService fhirAuthService) {
        this.configProvider = configProvider;
        this.fhirAuthService = fhirAuthService;
    }

    /** Uses RequestContext’s orgId (must be populated upstream). */
    public IGenericClient getForCurrentOrg() {
        RequestContext rc = RequestContext.get();
        Long orgId = (rc != null) ? rc.getOrgId() : null;
        log.info("Entering getForCurrentOrg for orgId: {}", orgId);
        if (orgId == null) throw new IllegalStateException("No orgId in request context");

        FhirConfig config = configProvider.getForCurrentOrg(IntegrationKey.FHIR);
        if (config == null) throw new RuntimeException("No FHIR configuration for orgId: " + orgId);

        String accessToken = fhirAuthService.getCachedAccessToken();

        log.info("Creating FHIR client for apiUrl: {}", config.getApiUrl());
        IGenericClient client = fhirContext.newRestfulGenericClient(config.getApiUrl());
        client.registerInterceptor(new BearerTokenAuthInterceptor(accessToken));

        String interceptorId = UUID.randomUUID().toString();
        client.registerInterceptor(new FhirTenantInterceptor(orgId, client, interceptorId, fhirContext));
        log.info("Exiting getForCurrentOrg with client hash {}", Integer.toHexString(client.hashCode()));
        return client;
    }

    /**
     * Build a client for the given orgId (without requiring RequestContext to already have it).
     * Temporarily sets rc.orgId, delegates to getForCurrentOrg(), then restores the previous value.
     */
    public IGenericClient getForOrg(Long orgId) {
        if (orgId == null) throw new IllegalArgumentException("orgId must not be null");

        RequestContext rc = RequestContext.get();
        if (rc == null) {
            throw new IllegalStateException(
                    "RequestContext not initialized; add a filter to initialize it or call getForCurrentOrg().");
        }

        Long prev = rc.getOrgId();
        try {
            rc.setOrgId(orgId);
            return getForCurrentOrg();
        } finally {
            try { rc.setOrgId(prev); } catch (Exception ignored) {}
        }
    }
}

