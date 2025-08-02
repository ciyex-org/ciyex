package com.qiaben.ciyex.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import ca.uhn.fhir.rest.client.apache.ApacheHttpRequest;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import com.qiaben.ciyex.dto.core.integration.FhirConfig;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.RequestContext;
import com.qiaben.ciyex.service.fhir.FhirAuthService;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IBaseMetaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class FhirClientProvider {

    private final OrgIntegrationConfigProvider configProvider;
    private final FhirAuthService fhirAuthService;
    private final TenantInterceptor tenantInterceptor;

    @Autowired
    public FhirClientProvider(OrgIntegrationConfigProvider configProvider, FhirAuthService fhirAuthService, TenantInterceptor tenantInterceptor) {
        this.configProvider = configProvider;
        this.fhirAuthService = fhirAuthService;
        this.tenantInterceptor = tenantInterceptor;
    }

    public IGenericClient getForCurrentOrg() {
        FhirConfig config = configProvider.getForCurrentOrg(IntegrationKey.FHIR);
        if (config == null) {
            throw new RuntimeException("No FHIR configuration found for the current organization");
        }

        // Get cached or fresh access token
        String accessToken;
        try {
            accessToken = fhirAuthService.getCachedAccessToken();
        } catch (Exception e) {
            log.error("Failed to obtain FHIR access token for orgId: {}", RequestContext.get().getOrgId(), e);
            throw new RuntimeException("Failed to obtain FHIR access token", e);
        }

        // Create FHIR client with bearer token and tenant interceptors
        FhirContext ctx = FhirContext.forR4();
        IGenericClient client = ctx.newRestfulGenericClient(config.getApiUrl());
        client.registerInterceptor(new BearerTokenAuthInterceptor(accessToken));
        client.registerInterceptor(tenantInterceptor);
        log.debug("Created FHIR client for orgId: {} with apiUrl: {}", RequestContext.get().getOrgId(), config.getApiUrl());
        return client;
    }
}

@Component
@Interceptor
@Slf4j
class TenantInterceptor {

    private static final String TENANT_TAG_SYSTEM = "http://ciyex.com/tenant";

    @Hook(Pointcut.CLIENT_REQUEST)
    public void interceptRequest(IHttpRequest theRequest, IGenericClient theClient) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        if (orgId == null) {
            log.error("No orgId in request context");
            throw new IllegalStateException("No orgId in request context");
        }

        FhirContext ctx = theClient.getFhirContext();
        if (theRequest instanceof ApacheHttpRequest) {
            ApacheHttpRequest apacheRequest = (ApacheHttpRequest) theRequest;
            if (apacheRequest.getApacheRequest() instanceof HttpEntityEnclosingRequestBase) {
                HttpEntityEnclosingRequestBase req = (HttpEntityEnclosingRequestBase) apacheRequest.getApacheRequest();
                try {
                    ContentType contentType = ContentType.get(req.getEntity());
                    if (contentType == null) {
                        log.debug("No content type in request, skipping tenant tag addition");
                        return;
                    }

                    String body = IOUtils.toString(req.getEntity().getContent(), StandardCharsets.UTF_8);
                    if (body.isEmpty()) {
                        log.debug("Empty request body, skipping tenant tag addition");
                        return;
                    }

                    // Parse the resource
                    IBaseResource resource;
                    boolean isJson = contentType.getMimeType().contains("json");
                    boolean isXml = contentType.getMimeType().contains("xml");
                    if (isJson) {
                        resource = ctx.newJsonParser().parseResource(body);
                    } else if (isXml) {
                        resource = ctx.newXmlParser().parseResource(body);
                    } else {
                        log.debug("Unsupported content type: {}, skipping tenant tag addition", contentType);
                        return;
                    }

                    // Add tenant tag
                    addTenantTag(resource, orgId);

                    // Encode back to string
                    String newBody;
                    ContentType newContentType;
                    if (isJson) {
                        newBody = ctx.newJsonParser().encodeResourceToString(resource);
                        newContentType = ContentType.create("application/fhir+json", StandardCharsets.UTF_8);
                    } else {
                        newBody = ctx.newXmlParser().encodeResourceToString(resource);
                        newContentType = ContentType.create("application/fhir+xml", StandardCharsets.UTF_8);
                    }

                    // Update request with new body
                    req.setEntity(new StringEntity(newBody, newContentType));
                    log.debug("Added tenant tag for orgId: {} to request", orgId);
                } catch (DataFormatException e) {
                    log.warn("Invalid FHIR resource in request, skipping tenant tag addition: {}", e.getMessage());
                } catch (IOException e) {
                    log.error("Failed to process request body for tenant tag addition for orgId: {}", orgId, e);
                    throw new RuntimeException("Failed to add tenant tag to request", e);
                }
            }
        }
    }

    @Hook(Pointcut.CLIENT_RESPONSE)
    public void interceptResponse(IHttpRequest theRequest, IHttpResponse theResponse, IGenericClient theClient) throws IOException {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        if (orgId == null) {
            log.error("No orgId in request context");
            throw new IllegalStateException("No orgId in request context");
        }

        FhirContext ctx = theClient.getFhirContext();
        theResponse.bufferEntity();
        try (InputStream inputStream = theResponse.readEntity()) {
            if (inputStream == null) {
                log.debug("Empty response body, skipping tenant tag check");
                return;
            }
            String body = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            if (body.isEmpty()) {
                log.debug("Empty response body, skipping tenant tag check");
                return;
            }

            // Determine content type from response headers
            String contentType = null;
            Map<String, List<String>> headers = theResponse.getAllHeaders();
            List<String> contentTypeHeaders = headers.get("Content-Type");
            if (contentTypeHeaders != null && !contentTypeHeaders.isEmpty()) {
                contentType = contentTypeHeaders.get(0);
            }
            if (contentType == null) {
                log.debug("No content type in response, skipping tenant tag check");
                return;
            }

            boolean isJson = contentType.contains("json");
            boolean isXml = contentType.contains("xml");
            if (!isJson && !isXml) {
                log.debug("Unsupported content type: {}, skipping tenant tag check", contentType);
                return;
            }

            // Parse the resource
            IBaseResource resource;
            try {
                if (isJson) {
                    resource = ctx.newJsonParser().parseResource(body);
                } else {
                    resource = ctx.newXmlParser().parseResource(body);
                }
                checkTenantTag(resource, orgId);
                log.debug("Verified tenant tag for orgId: {} in response", orgId);
            } catch (DataFormatException e) {
                log.warn("Invalid FHIR resource in response, skipping tenant tag check: {}", e.getMessage());
            } catch (Exception e) {
                log.error("Failed to verify tenant tag in response for orgId: {}", orgId, e);
                throw new RuntimeException("Failed to verify tenant tag in response", e);
            }
        }
    }

    private void addTenantTag(IBaseResource resource, Long orgId) {
        IBaseMetaType meta = resource.getMeta();
        meta.addTag().setSystem(TENANT_TAG_SYSTEM).setCode(orgId.toString()).setDisplay("Tenant ID");
    }

    private void checkTenantTag(IBaseResource resource, Long orgId) {
        IBaseMetaType meta = resource.getMeta();
        if (meta.getTag(TENANT_TAG_SYSTEM, orgId.toString()) == null) {
            throw new RuntimeException("Unauthorized: Resource does not belong to current tenant (orgId: " + orgId + ")");
        }
    }
}