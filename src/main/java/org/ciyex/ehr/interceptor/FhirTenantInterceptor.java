package org.ciyex.ehr.interceptor;

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

    private final String tenantName;
    private final IGenericClient theClient;
    private final String interceptorId; // Unique identifier for this interceptor instance
    private final FhirContext fhirContext; // Use FhirContext instead of ObjectMapper

    public FhirTenantInterceptor(String tenantName, IGenericClient client, String interceptorId, FhirContext fhirContext) {
        this.tenantName = tenantName;
        this.theClient = client;
        this.interceptorId = interceptorId;
        this.fhirContext = fhirContext;
        log.info("Created FhirTenantInterceptor instance for tenantName: {}, interceptorId: {}, client hash: {}", tenantName, interceptorId, Integer.toHexString(client.hashCode()));
    }

    private static final String TENANT_TAG_SYSTEM = "http://ciyex.com/tenant";
    private static final String METADATA_ENDPOINT = "/metadata";

    @Hook(Pointcut.CLIENT_REQUEST)
    public void interceptRequest(IHttpRequest theRequest) {
        log.info("Entering interceptRequest for tenantName: {}, interceptorId: {}, client: {}", tenantName, interceptorId, (theClient != null ? Integer.toHexString(theClient.hashCode()) : "null"));
        if (theClient == null) {
            log.warn("IGenericClient is null in interceptRequest for tenantName: {}, interceptorId: {}, skipping tenant tag addition", tenantName, interceptorId);
            new Exception("Stack trace for null client").printStackTrace();
            return;
        }

        log.info("Processing request for tenantName: {}, interceptorId: {}, client hash: {}, request: {}", tenantName, interceptorId, Integer.toHexString(theClient.hashCode()), theRequest);
        if (theRequest instanceof ApacheHttpRequest) {
            ApacheHttpRequest apacheRequest = (ApacheHttpRequest) theRequest;
            log.debug("Request is ApacheHttpRequest for tenantName: {}, interceptorId: {}, request: {}", tenantName, interceptorId, apacheRequest);
            if (apacheRequest.getApacheRequest() instanceof HttpEntityEnclosingRequestBase req) {
                log.debug("Request is HttpEntityEnclosingRequestBase for tenantName: {}, interceptorId: {}", tenantName, interceptorId);
                try {
                    Object entity = req.getEntity();
                    log.debug("Request entity for tenantName: {}, interceptorId: {}: {}", tenantName, interceptorId, entity);
                    Header[] headers = req.getAllHeaders();
                    StringBuilder headerStr = new StringBuilder();
                    for (Header header : headers) {
                        headerStr.append(header.getName()).append(": ").append(header.getValue()).append(", ");
                    }
                    log.debug("Request headers for tenantName: {}, interceptorId: {}: {}", tenantName, interceptorId, headerStr.toString());

                    if (entity == null) {
                        log.warn("No entity in request for tenantName: {}, interceptorId: {}, skipping tenant tag addition", tenantName, interceptorId);
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
                        log.warn("No valid content type in request for tenantName: {}, interceptorId: {}, headers: {}", tenantName, interceptorId, headerStr.toString());
                        return;
                    }

                    String body = IOUtils.toString(req.getEntity().getContent(), StandardCharsets.UTF_8);
                    if (body.isEmpty()) {
                        log.debug("Empty request body for tenantName: {}, interceptorId: {}", tenantName, interceptorId);
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
                        log.debug("Unsupported content type: {} for tenantName: {}, interceptorId: {}", contentType, tenantName, interceptorId);
                        return;
                    }

                    addTenantTag(resource, tenantName);
                    log.info("Added tenant tag to resource for tenantName: {}, interceptorId: {}, resource id: {}", tenantName, interceptorId, resource.getIdElement().getValue());

                    String newBody = parser.encodeResourceToString(resource); // Use FhirContext encoding
                    ContentType newContentType = isJson ? ContentType.create("application/fhir+json", StandardCharsets.UTF_8) : ContentType.create("application/fhir+xml", StandardCharsets.UTF_8);
                    log.info("New request body after tag addition for tenantName: {}, interceptorId: {}: {}", tenantName, interceptorId, newBody);

                    req.setEntity(new StringEntity(newBody, newContentType));
                    log.info("Updated request with new body for tenantName: {}, interceptorId: {}", tenantName, interceptorId);
                } catch (DataFormatException | IOException e) {
                    log.warn("Failed to process request body for tenant tag addition for tenantName: {}, interceptorId: {}, error: {}", tenantName, interceptorId, e.getMessage());
                }
            } else {
                log.debug("Request is not HttpEntityEnclosingRequestBase for tenantName: {}, interceptorId: {}, skipping tenant tag addition (e.g., GET request)", tenantName, interceptorId);
            }
        } else {
            log.warn("Request is not ApacheHttpRequest for tenantName: {}, interceptorId: {}, skipping tenant tag addition", tenantName, interceptorId);
        }
        log.info("Exiting interceptRequest for tenantName: {}, interceptorId: {}", tenantName, interceptorId);
    }

    @Hook(Pointcut.CLIENT_RESPONSE)
    public void interceptResponse(IHttpRequest theRequest, IHttpResponse theResponse) throws IOException {
        log.info("Entering interceptResponse for tenantName: {}, interceptorId: {}, client: {}", tenantName, interceptorId, (theClient != null ? Integer.toHexString(theClient.hashCode()) : "null"));
        if (theClient == null) {
            log.warn("IGenericClient is null in interceptResponse for tenantName: {}, interceptorId: {}, skipping tenant tag check", tenantName, interceptorId);
            return;
        }

        log.info("Processing response for tenantName: {}, interceptorId: {}, client hash: {}, response: {}", tenantName, interceptorId, Integer.toHexString(theClient.hashCode()), theResponse);
        FhirContext ctx = theClient.getFhirContext();
        theResponse.bufferEntity();
        try (InputStream inputStream = theResponse.readEntity()) {
            if (inputStream == null) {
                log.debug("Empty response body for tenantName: {}, interceptorId: {}", tenantName, interceptorId);
                return;
            }
            String body = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            if (body.isEmpty()) {
                log.debug("Empty response body for tenantName: {}, interceptorId: {}", tenantName, interceptorId);
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
                log.warn("No valid content type in response for tenantName: {}, interceptorId: {}, headers: {}", tenantName, interceptorId, headers);
                return;
            }

            boolean isJson = contentType.contains("json");
            boolean isXml = contentType.contains("xml");
            if (!isJson && !isXml) {
                log.debug("Unsupported content type: {} for tenantName: {}, interceptorId: {}", contentType, tenantName, interceptorId);
                return;
            }

            IBaseResource resource;
            try {
                if (isJson) {
                    resource = ctx.newJsonParser().parseResource(body); // Use FhirContext parser
                    try {
                        body = ctx.newJsonParser().encodeResourceToString(resource); // Use FhirContext encoding
                    } catch (Exception e) {
                        log.warn("Failed to encode JSON response for tenantName: {}, interceptorId: {}, falling back to raw body: {}", tenantName, interceptorId, e.getMessage());
                        body = IOUtils.toString(inputStream, StandardCharsets.UTF_8); // Fallback to raw body
                    }
                } else {
                    resource = ctx.newXmlParser().parseResource(body);
                    body = ctx.newXmlParser().encodeResourceToString(resource); // Use FhirContext encoding
                }
                log.info("Response body for tenantName: {}, interceptorId: {}: {}", tenantName, interceptorId, body);

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
                            checkTenantTag(entryResource, tenantName);
                            hasValidTag = true;
                        }
                    }
                    if (hasValidTag) {
                        log.info("Verified tenant tag for all entries in Bundle for tenantName: {}, interceptorId: {}", tenantName, interceptorId);
                    } else {
                        log.error("Unauthorized: No entries in Bundle for tenantName: {}, interceptorId: {} have matching tenant tag", tenantName, interceptorId);
                        throw new RuntimeException("Unauthorized: No entries in Bundle belong to current tenant (tenantName: " + tenantName + ")");
                    }
                } else {
                    checkTenantTag(resource, tenantName);
                    log.info("Verified tenant tag for tenantName: {}, interceptorId: {} in response", tenantName, interceptorId);
                }
            } catch (DataFormatException e) {
                log.warn("Invalid FHIR resource in response for tenantName: {}, interceptorId: {}, skipping tenant tag check: {}", tenantName, interceptorId, e.getMessage());
            } catch (IOException e) {
                log.error("Failed to read or process response for tenantName: {}, interceptorId: {}, error: {}", tenantName, interceptorId, e.getMessage(), e);
            }
        } catch (IOException e) {
            log.error("Failed to read response entity for tenantName: {}, interceptorId: {}, error: {}", tenantName, interceptorId, e.getMessage(), e);
        }
        log.info("Exiting interceptResponse for tenantName: {}, interceptorId: {}", tenantName, interceptorId);
    }

    private void addTenantTag(IBaseResource resource, String tenantName) {
        IBaseMetaType meta = resource.getMeta();
        meta.addTag().setSystem(TENANT_TAG_SYSTEM).setCode(tenantName).setDisplay("Tenant ID");
        log.info("Added tenant tag with system: {}, code: {} to resource for tenantName: {}, interceptorId: {}", TENANT_TAG_SYSTEM, tenantName, tenantName, interceptorId);
    }

    private void checkTenantTag(IBaseResource resource, String tenantName) {
        IBaseMetaType meta = resource.getMeta();
        if (meta.getTag(TENANT_TAG_SYSTEM, tenantName) == null) {
            log.error("Unauthorized: Resource for tenantName: {}, interceptorId: {} does not have matching tenant tag", tenantName, interceptorId);
            throw new RuntimeException("Unauthorized: Resource does not belong to current tenant (tenantName: " + tenantName + ")");
        }
        log.info("Verified tenant tag for tenantName: {}, interceptorId: {} in resource", tenantName, interceptorId);
    }
}