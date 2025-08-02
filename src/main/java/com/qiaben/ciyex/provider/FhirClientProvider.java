package com.qiaben.ciyex.provider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import com.qiaben.ciyex.dto.core.integration.FhirConfig;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.RequestContext;
import com.qiaben.ciyex.interceptor.FhirTenantInterceptor;
import com.qiaben.ciyex.storage.fhir.FhirAuthService;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class FhirClientProvider {

    private final OrgIntegrationConfigProvider configProvider;
    private final FhirAuthService fhirAuthService;
    private final FhirContext fhirContext; // Inject FhirContext instead of ObjectMapper

    @Autowired
    public FhirClientProvider(OrgIntegrationConfigProvider configProvider, FhirAuthService fhirAuthService, FhirContext fhirContext) {
        this.configProvider = configProvider;
        this.fhirAuthService = fhirAuthService;
        this.fhirContext = fhirContext;
        log.info("Initialized FhirClientProvider with dependencies");
    }

    public IGenericClient getForCurrentOrg() {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.info("Entering getForCurrentOrg for orgId: {}", orgId);
        if (orgId == null) throw new IllegalStateException("No orgId in request context");
        FhirConfig config = configProvider.getForCurrentOrg(IntegrationKey.FHIR);
        if (config == null) throw new RuntimeException("No FHIR configuration found for the current organization");
        String accessToken = fhirAuthService.getCachedAccessToken();
        log.info("Obtained access token and FhirConfig for orgId: {}, apiUrl: {}", orgId, config.getApiUrl());

        log.info("Creating new FHIR context for orgId: {}", orgId);
        IGenericClient client = fhirContext.newRestfulGenericClient(config.getApiUrl());
        log.info("Created IGenericClient for orgId: {}, apiUrl: {}, client hash: {}", orgId, config.getApiUrl(), Integer.toHexString(client.hashCode()));
        client.registerInterceptor(new BearerTokenAuthInterceptor(accessToken));
        log.info("Registered BearerTokenAuthInterceptor with token for orgId: {}, client hash: {}", orgId, Integer.toHexString(client.hashCode()));

        String interceptorId = UUID.randomUUID().toString();
        FhirTenantInterceptor tenantInterceptor = new FhirTenantInterceptor(orgId, client, interceptorId, fhirContext);
        client.registerInterceptor(tenantInterceptor);
        log.info("Registered FhirTenantInterceptor with id: {}, orgId: {}, client hash: {}", interceptorId, orgId, Integer.toHexString(client.hashCode()));
        log.info("Exiting getForCurrentOrg with valid client for orgId: {}, client hash: {}", orgId, Integer.toHexString(client.hashCode()));
        return client;
    }
}