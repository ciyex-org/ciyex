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
     */
    public IGenericClient getForCurrentTenant() {
        log.info("Entering getForCurrentTenant (no tenant required)");

        FhirConfig config = configProvider.getForCurrentTenant(IntegrationKey.FHIR);
        if (config == null) {
            log.error("No FHIR configuration found in org_config table. Required keys: fhir_api_url, fhir_client_id, fhir_client_secret, fhir_token_url, fhir_scope");
            throw new RuntimeException("No FHIR configuration found. Please configure FHIR in org_config table or disable storage_type.");
        }

        String accessToken = fhirAuthService.getCachedAccessToken();

        log.info("Creating FHIR client for apiUrl: {}", config.getApiUrl());
        IGenericClient client = fhirContext.newRestfulGenericClient(config.getApiUrl());
        client.registerInterceptor(new BearerTokenAuthInterceptor(accessToken));

        log.info("FHIR client created successfully (no tenant tags)");
        return client;
    }
}

