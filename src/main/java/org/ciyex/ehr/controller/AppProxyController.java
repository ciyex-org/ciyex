package org.ciyex.ehr.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.marketplace.dto.AppInstallationResponse;
import org.ciyex.ehr.marketplace.repository.AppInstallationRepository;
import org.ciyex.ehr.marketplace.service.AppInstallationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic proxy controller that forwards requests to any installed app's backend service.
 * Service URLs are discovered from the app_installations table (set by marketplace webhook or seed migration).
 * <p>
 * Usage: /api/app-proxy/{appSlug}/any/path/here
 * <p>
 * This replaces all per-app hardcoded proxy controllers (e.g. CodesProxyController).
 */
@PreAuthorize("hasAuthority('SCOPE_user/Organization.read')")
@RestController
@RequestMapping("/api/app-proxy")
@Slf4j
public class AppProxyController {

    private final AppInstallationService appInstallationService;
    private final AppInstallationRepository appInstallationRepository;
    private final ConcurrentHashMap<String, RestClient> clientCache = new ConcurrentHashMap<>();

    public AppProxyController(AppInstallationService appInstallationService,
                              AppInstallationRepository appInstallationRepository) {
        this.appInstallationService = appInstallationService;
        this.appInstallationRepository = appInstallationRepository;
    }

    @RequestMapping(value = "/{appSlug}/**", method = {
            RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
            RequestMethod.DELETE, RequestMethod.PATCH
    })
    public ResponseEntity<byte[]> proxy(
            @PathVariable String appSlug,
            HttpServletRequest request) throws IOException {

        // Path traversal protection
        if (appSlug.contains("..") || appSlug.contains("/")) {
            return ResponseEntity.badRequest().build();
        }

        RequestContext ctx = RequestContext.get();
        String orgId = ctx.getOrgName();

        // Resolve service URL from app installation
        String serviceUrl = resolveServiceUrl(orgId, appSlug);
        if (serviceUrl == null) {
            log.debug("App {} not installed or no service_url for org {}", appSlug, orgId);
            return ResponseEntity.status(404)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(("{\"error\":\"App not installed\",\"app\":\"" + appSlug + "\"}").getBytes());
        }

        // Extract the downstream path (everything after /api/app-proxy/{appSlug})
        String fullPath = request.getRequestURI();
        String prefix = "/api/app-proxy/" + appSlug;
        String downstreamPath = fullPath.substring(prefix.length());
        if (downstreamPath.contains("..")) {
            return ResponseEntity.badRequest().build();
        }

        // Append query string
        String queryString = request.getQueryString();
        String targetUrl = downstreamPath + (queryString != null ? "?" + queryString : "");

        HttpMethod method = HttpMethod.valueOf(request.getMethod());

        try {
            RestClient client = clientCache.computeIfAbsent(serviceUrl, url ->
                    RestClient.builder().baseUrl(url).build());

            RestClient.RequestBodySpec spec = client.method(method).uri(targetUrl);

            // Forward Authorization header
            String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (auth != null) {
                spec.header(HttpHeaders.AUTHORIZATION, auth);
            }

            // Inject org alias header
            if (orgId != null) {
                spec.header("X-Org-Alias", orgId);
            }

            // Forward Content-Type for requests with body
            String contentType = request.getContentType();
            if (contentType != null) {
                spec.header(HttpHeaders.CONTENT_TYPE, contentType);
            }

            // Forward body for POST/PUT/PATCH
            if (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH) {
                byte[] body = request.getInputStream().readAllBytes();
                spec.body(body);
            }

            ResponseEntity<byte[]> response = spec.retrieve()
                    .onStatus(status -> true, (req, res) -> {
                        // Do not throw — we forward all upstream responses as-is
                    })
                    .toEntity(byte[].class);

            // Return the upstream response as-is
            HttpHeaders responseHeaders = new HttpHeaders();
            String respContentType = response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
            if (respContentType != null) {
                responseHeaders.set(HttpHeaders.CONTENT_TYPE, respContentType);
            }

            return ResponseEntity.status(response.getStatusCode())
                    .headers(responseHeaders)
                    .body(response.getBody());

        } catch (HttpStatusCodeException e) {
            // Forward upstream error response transparently
            HttpHeaders responseHeaders = new HttpHeaders();
            String ct = e.getResponseHeaders() != null ? e.getResponseHeaders().getFirst(HttpHeaders.CONTENT_TYPE) : null;
            if (ct != null) responseHeaders.set(HttpHeaders.CONTENT_TYPE, ct);
            return ResponseEntity.status(e.getStatusCode())
                    .headers(responseHeaders)
                    .body(e.getResponseBodyAsByteArray());
        } catch (Exception e) {
            log.error("Failed to proxy request to app {}: {} {}", appSlug, method, targetUrl, e);
            return ResponseEntity.status(502)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(("{\"error\":\"Upstream service error\",\"app\":\"" + appSlug + "\"}").getBytes());
        }
    }

    /**
     * Resolve the service URL for an app. Tries org-specific installation first,
     * then falls back to any active installation (for infrastructure apps like ciyex-codes
     * that are shared across orgs and don't depend on org context).
     */
    private String resolveServiceUrl(String orgId, String appSlug) {
        // Try org-specific installation first
        if (orgId != null && !orgId.isBlank()) {
            AppInstallationResponse installation = appInstallationService.getInstallation(orgId, appSlug);
            String url = extractServiceUrl(installation);
            if (url != null) return url;
        }

        // Fallback: find any active installation for this app (org-agnostic)
        return appInstallationRepository.findFirstByAppSlugAndStatus(appSlug, "active")
                .map(i -> {
                    Map<String, Object> config = i.getConfig();
                    if (config == null) return null;
                    Object url = config.get("service_url");
                    return url != null && !url.toString().isBlank() ? url.toString() : null;
                })
                .orElse(null);
    }

    private String extractServiceUrl(AppInstallationResponse installation) {
        if (installation == null || installation.getConfig() == null) return null;
        Object url = installation.getConfig().get("service_url");
        return url != null && !url.toString().isBlank() ? url.toString() : null;
    }
}
