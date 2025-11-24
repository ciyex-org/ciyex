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
//    public IGenericClient getForCurrentTenant() {
//         = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
//        log.info("Entering getForCurrentTenant for orgId: {}", orgId);
//        if (orgId == null) throw new IllegalStateException("No orgId in request context");
//        FhirConfig config = configProvider.getForCurrentTenant(IntegrationKey.FHIR);
//        if (config == null) throw new RuntimeException("No FHIR configuration found for the current organization");
//        String accessToken = fhirAuthService.getCachedAccessToken();
//        log.info("Obtained access token and FhirConfig for orgId: {}, apiUrl: {}", config.getApiUrl());
//
//        log.info("Creating new FHIR context for orgId: {}", orgId);
//        IGenericClient client = fhirContext.newRestfulGenericClient(config.getApiUrl());
//        log.info("Created IGenericClient for orgId: {}, apiUrl: {}, client hash: {}", config.getApiUrl(), Integer.toHexString(client.hashCode()));
//        client.registerInterceptor(new BearerTokenAuthInterceptor(accessToken));
//        log.info("Registered BearerTokenAuthInterceptor with token for orgId: {}, client hash: {}", Integer.toHexString(client.hashCode()));
//
//        String interceptorId = UUID.randomUUID().toString();
//        FhirTenantInterceptor tenantInterceptor = new FhirTenantInterceptor(client, interceptorId, fhirContext);
//        client.registerInterceptor(tenantInterceptor);
//        log.info("Registered FhirTenantInterceptor with id: {}, orgId: {}, client hash: {}", interceptorId, Integer.toHexString(client.hashCode()));
//        log.info("Exiting getForCurrentTenant with valid client for orgId: {}, client hash: {}", Integer.toHexString(client.hashCode()));
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
import com.qiaben.ciyex.storage.fhir.FhirAuthService;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

    /**
     * Get FHIR client without requiring tenant context.
     * Works with single-tenant configuration from org_config table.
     * No tenant name or org ID required - simplified for single tenant setup.
     */
    public IGenericClient getForCurrentTenant() {
        log.info("Creating FHIR client (single-tenant mode, no tenant/org validation required)");

        FhirConfig config = configProvider.getForCurrentTenant(IntegrationKey.FHIR);
        if (config == null) {
            log.warn("No FHIR configuration found in org_config table. External storage sync will be skipped.");
            log.info("To enable FHIR storage, configure these keys in org_config: fhir_api_url, fhir_client_id, fhir_client_secret, fhir_token_url, fhir_scope");
            return null; // Return null instead of throwing exception
        }

        try {
            String accessToken = fhirAuthService.getCachedAccessToken();
            
            log.info("Creating FHIR client for apiUrl: {}", config.getApiUrl());
            IGenericClient client = fhirContext.newRestfulGenericClient(config.getApiUrl());
            
            if (accessToken != null && !accessToken.isBlank()) {
                client.registerInterceptor(new BearerTokenAuthInterceptor(accessToken));
                log.info("FHIR client created successfully with authentication");
            } else {
                log.warn("No access token available, creating FHIR client without authentication");
            }
            
            return client;
            
        } catch (Exception e) {
            log.error("Failed to create FHIR client: {}. External storage will be skipped.", e.getMessage());
            return null; // Return null instead of throwing exception
        }
    }
}

