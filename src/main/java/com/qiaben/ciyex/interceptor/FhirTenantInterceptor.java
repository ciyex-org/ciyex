package com.qiaben.ciyex.interceptor;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import ca.uhn.fhir.rest.client.apache.ApacheHttpRequest;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseMetaType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
public class FhirTenantInterceptor {

    private final Long orgId;
    private final IGenericClient theClient;
    private final String interceptorId; // Unique identifier for this interceptor instance
    private final FhirContext fhirContext; // Use FhirContext instead of ObjectMapper

    public FhirTenantInterceptor(Long orgId, IGenericClient client, String interceptorId, FhirContext fhirContext) {
        this.orgId = orgId;
        this.theClient = client;
        this.interceptorId = interceptorId;
        this.fhirContext = fhirContext;
        log.info("Created FhirTenantInterceptor instance for orgId: {}, interceptorId: {}, client hash: {}", orgId, interceptorId, Integer.toHexString(client.hashCode()));
    }

    private static final String TENANT_TAG_SYSTEM = "http://ciyex.com/tenant";
    private static final String METADATA_ENDPOINT = "/metadata";

    @Hook(Pointcut.CLIENT_REQUEST)
    public void interceptRequest(IHttpRequest theRequest) {
        log.info("Entering interceptRequest for orgId: {}, interceptorId: {}, client: {}", orgId, interceptorId, (theClient != null ? Integer.toHexString(theClient.hashCode()) : "null"));
        if (theClient == null) {
            log.warn("IGenericClient is null in interceptRequest for orgId: {}, interceptorId: {}, skipping tenant tag addition", orgId, interceptorId);
            new Exception("Stack trace for null client").printStackTrace();
            return;
        }

        log.info("Processing request for orgId: {}, interceptorId: {}, client hash: {}, request: {}", orgId, interceptorId, Integer.toHexString(theClient.hashCode()), theRequest);
        if (theRequest instanceof ApacheHttpRequest) {
            ApacheHttpRequest apacheRequest = (ApacheHttpRequest) theRequest;
            log.debug("Request is ApacheHttpRequest for orgId: {}, interceptorId: {}, request: {}", orgId, interceptorId, apacheRequest);
            if (apacheRequest.getApacheRequest() instanceof HttpEntityEnclosingRequestBase req) {
                log.debug("Request is HttpEntityEnclosingRequestBase for orgId: {}, interceptorId: {}", orgId, interceptorId);
                try {
                    Object entity = req.getEntity();
                    log.debug("Request entity for orgId: {}, interceptorId: {}: {}", orgId, interceptorId, entity);
                    Header[] headers = req.getAllHeaders();
                    StringBuilder headerStr = new StringBuilder();
                    for (Header header : headers) {
                        headerStr.append(header.getName()).append(": ").append(header.getValue()).append(", ");
                    }
                    log.debug("Request headers for orgId: {}, interceptorId: {}: {}", orgId, interceptorId, headerStr.toString());

                    if (entity == null) {
                        log.warn("No entity in request for orgId: {}, interceptorId: {}, skipping tenant tag addition", orgId, interceptorId);
                        return;
                    }

                    String contentTypeHeader = null;
                    for (Header header : headers) {
                        if (header.getName().equalsIgnoreCase("Content-Type")) {
                            contentTypeHeader = header.getValue();
                            break;
                        }
                    }
                    ContentType contentType = contentTypeHeader != null ? ContentType.parse(contentTypeHeader) : null;
                    if (contentType == null) {
                        log.warn("No valid content type in request for orgId: {}, interceptorId: {}, headers: {}", orgId, interceptorId, headerStr.toString());
                        return;
                    }

                    String body = IOUtils.toString(req.getEntity().getContent(), StandardCharsets.UTF_8);
                    if (body.isEmpty()) {
                        log.debug("Empty request body for orgId: {}, interceptorId: {}", orgId, interceptorId);
                        return;
                    }

                    IBaseResource resource;
                    IParser parser = fhirContext.newJsonParser(); // Use FhirContext parser
                    boolean isJson = contentType.getMimeType().contains("json");
                    boolean isXml = contentType.getMimeType().contains("xml");
                    if (isJson) {
                        resource = parser.parseResource(body);
                    } else if (isXml) {
                        resource = fhirContext.newXmlParser().parseResource(body);
                    } else {
                        log.debug("Unsupported content type: {} for orgId: {}, interceptorId: {}", contentType, orgId, interceptorId);
                        return;
                    }

                    addTenantTag(resource, orgId);
                    log.info("Added tenant tag to resource for orgId: {}, interceptorId: {}, resource id: {}", orgId, interceptorId, resource.getIdElement().getValue());

                    String newBody = parser.encodeResourceToString(resource); // Use FhirContext encoding
                    ContentType newContentType = isJson ? ContentType.create("application/fhir+json", StandardCharsets.UTF_8) : ContentType.create("application/fhir+xml", StandardCharsets.UTF_8);
                    log.info("New request body after tag addition for orgId: {}, interceptorId: {}: {}", orgId, interceptorId, newBody);

                    req.setEntity(new StringEntity(newBody, newContentType));
                    log.info("Updated request with new body for orgId: {}, interceptorId: {}", orgId, interceptorId);
                } catch (DataFormatException | IOException e) {
                    log.warn("Failed to process request body for tenant tag addition for orgId: {}, interceptorId: {}, error: {}", orgId, interceptorId, e.getMessage());
                }
            } else {
                log.debug("Request is not HttpEntityEnclosingRequestBase for orgId: {}, interceptorId: {}, skipping tenant tag addition (e.g., GET request)", orgId, interceptorId);
            }
        } else {
            log.warn("Request is not ApacheHttpRequest for orgId: {}, interceptorId: {}, skipping tenant tag addition", orgId, interceptorId);
        }
        log.info("Exiting interceptRequest for orgId: {}, interceptorId: {}", orgId, interceptorId);
    }

    @Hook(Pointcut.CLIENT_RESPONSE)
    public void interceptResponse(IHttpRequest theRequest, IHttpResponse theResponse) throws IOException {
        log.info("Entering interceptResponse for orgId: {}, interceptorId: {}, client: {}", orgId, interceptorId, (theClient != null ? Integer.toHexString(theClient.hashCode()) : "null"));
        if (theClient == null) {
            log.warn("IGenericClient is null in interceptResponse for orgId: {}, interceptorId: {}, skipping tenant tag check", orgId, interceptorId);
            return;
        }

        log.info("Processing response for orgId: {}, interceptorId: {}, client hash: {}, response: {}", orgId, interceptorId, Integer.toHexString(theClient.hashCode()), theResponse);
        FhirContext ctx = theClient.getFhirContext();
        theResponse.bufferEntity();
        try (InputStream inputStream = theResponse.readEntity()) {
            if (inputStream == null) {
                log.debug("Empty response body for orgId: {}, interceptorId: {}", orgId, interceptorId);
                return;
            }
            String body = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            if (body.isEmpty()) {
                log.debug("Empty response body for orgId: {}, interceptorId: {}", orgId, interceptorId);
                return;
            }

            String contentType = null;
            Map<String, List<String>> headers = theResponse.getAllHeaders();
            List<String> contentTypeHeaders = null;
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (entry.getKey().equalsIgnoreCase("Content-Type")) {
                    contentTypeHeaders = entry.getValue();
                    break;
                }
            }
            if (contentTypeHeaders != null && !contentTypeHeaders.isEmpty()) {
                contentType = contentTypeHeaders.get(0);
            }
            if (contentType == null) {
                log.warn("No valid content type in response for orgId: {}, interceptorId: {}, headers: {}", orgId, interceptorId, headers);
                return;
            }

            boolean isJson = contentType.contains("json");
            boolean isXml = contentType.contains("xml");
            if (!isJson && !isXml) {
                log.debug("Unsupported content type: {} for orgId: {}, interceptorId: {}", contentType, orgId, interceptorId);
                return;
            }

            IBaseResource resource;
            try {
                if (isJson) {
                    resource = ctx.newJsonParser().parseResource(body); // Use FhirContext parser
                    try {
                        body = ctx.newJsonParser().encodeResourceToString(resource); // Use FhirContext encoding
                    } catch (Exception e) {
                        log.warn("Failed to encode JSON response for orgId: {}, interceptorId: {}, falling back to raw body: {}", orgId, interceptorId, e.getMessage());
                        body = IOUtils.toString(inputStream, StandardCharsets.UTF_8); // Fallback to raw body
                    }
                } else {
                    resource = ctx.newXmlParser().parseResource(body);
                    body = ctx.newXmlParser().encodeResourceToString(resource); // Use FhirContext encoding
                }
                log.info("Response body for orgId: {}, interceptorId: {}: {}", orgId, interceptorId, body);

                // Skip tenant tag check for metadata responses and handle Bundle
                String requestUrl = theRequest.getUri().toString();
                if (requestUrl.contains(METADATA_ENDPOINT)) {
                    log.debug("Skipping tenant tag check for metadata response at {}", requestUrl);
                } else if (resource instanceof Bundle) {
                    Bundle bundle = (Bundle) resource;
                    boolean hasValidTag = false;
                    for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                        IBaseResource entryResource = entry.getResource();
                        if (entryResource != null) {
                            checkTenantTag(entryResource, orgId);
                            hasValidTag = true;
                        }
                    }
                    if (hasValidTag) {
                        log.info("Verified tenant tag for all entries in Bundle for orgId: {}, interceptorId: {}", orgId, interceptorId);
                    } else {
                        log.error("Unauthorized: No entries in Bundle for orgId: {}, interceptorId: {} have matching tenant tag", orgId, interceptorId);
                        throw new RuntimeException("Unauthorized: No entries in Bundle belong to current tenant (orgId: " + orgId + ")");
                    }
                } else {
                    checkTenantTag(resource, orgId);
                    log.info("Verified tenant tag for orgId: {}, interceptorId: {} in response", orgId, interceptorId);
                }
            } catch (DataFormatException e) {
                log.warn("Invalid FHIR resource in response for orgId: {}, interceptorId: {}, skipping tenant tag check: {}", orgId, interceptorId, e.getMessage());
            } catch (IOException e) {
                log.error("Failed to read or process response for orgId: {}, interceptorId: {}, error: {}", orgId, interceptorId, e.getMessage(), e);
            }
        } catch (IOException e) {
            log.error("Failed to read response entity for orgId: {}, interceptorId: {}, error: {}", orgId, interceptorId, e.getMessage(), e);
        }
        log.info("Exiting interceptResponse for orgId: {}, interceptorId: {}", orgId, interceptorId);
    }

    private void addTenantTag(IBaseResource resource, Long orgId) {
        IBaseMetaType meta = resource.getMeta();
        meta.addTag().setSystem(TENANT_TAG_SYSTEM).setCode(orgId.toString()).setDisplay("Tenant ID");
        log.info("Added tenant tag with system: {}, code: {} to resource for orgId: {}, interceptorId: {}", TENANT_TAG_SYSTEM, orgId, orgId, interceptorId);
    }

    private void checkTenantTag(IBaseResource resource, Long orgId) {
        IBaseMetaType meta = resource.getMeta();
        if (meta.getTag(TENANT_TAG_SYSTEM, orgId.toString()) == null) {
            log.error("Unauthorized: Resource for orgId: {}, interceptorId: {} does not have matching tenant tag", orgId, interceptorId);
            throw new RuntimeException("Unauthorized: Resource does not belong to current tenant (orgId: " + orgId + ")");
        }
        log.info("Verified tenant tag for orgId: {}, interceptorId: {} in resource", orgId, interceptorId);
    }
}