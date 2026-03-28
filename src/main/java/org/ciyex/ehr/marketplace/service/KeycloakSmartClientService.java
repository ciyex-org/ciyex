package org.ciyex.ehr.marketplace.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Manages Keycloak OAuth2 client registrations for SMART on FHIR apps.
 *
 * When a third-party SMART app is installed (via marketplace webhook or manual install),
 * this service creates a Keycloak client in the EHR's realm. The client is configured with:
 * - The app's redirect URIs (for OAuth2 callback)
 * - The app's requested FHIR scopes (mapped to Keycloak client scopes)
 * - Public client mode (for SPA-based SMART apps) or confidential (for server-side)
 *
 * Per the SMART on FHIR spec, each app gets a unique client ID: "ciyexhub-app-{appSlug}".
 *
 * @see <a href="https://hl7.org/fhir/smart-app-launch/">SMART App Launch</a>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakSmartClientService {

    private final RestClient restClient;

    @Value("${keycloak.admin.server-url:${keycloak.auth-server-url}}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String keycloakRealm;

    @Value("${keycloak.resource}")
    private String serviceClientId;

    @Value("${keycloak.credentials.secret}")
    private String serviceClientSecret;

    @Value("${keycloak.enabled:false}")
    private boolean keycloakEnabled;

    /**
     * Register a Keycloak OAuth2 client for a SMART on FHIR app.
     *
     * @param appSlug       Unique app identifier (e.g., "smart-vitals")
     * @param appName       Display name for the Keycloak client
     * @param redirectUris  Allowed redirect URIs (OAuth2 callback URLs)
     * @param fhirScopes    Requested FHIR scopes (e.g., ["patient/Patient.r", "patient/Observation.cruds"])
     * @param publicClient  True for SPA apps (PKCE), false for server-side (client_secret)
     * @return The Keycloak client ID string, or null if registration is disabled
     */
    public String registerSmartClient(
            String appSlug,
            String appName,
            List<String> redirectUris,
            List<String> fhirScopes,
            boolean publicClient) {

        if (!keycloakEnabled) {
            log.info("Keycloak disabled — skipping SMART client registration for {}", appSlug);
            return toClientId(appSlug);
        }

        String clientId = toClientId(appSlug);

        try {
            String accessToken = getAdminAccessToken();

            // Check if client already exists
            String existingId = findClientByClientId(clientId, accessToken);
            if (existingId != null) {
                log.info("Keycloak client '{}' already exists, updating", clientId);
                updateClient(existingId, appName, redirectUris, fhirScopes, publicClient, accessToken);
                return clientId;
            }

            // Build client representation per Keycloak Admin REST API
            Map<String, Object> clientRep = buildClientRepresentation(
                    clientId, appName, redirectUris, fhirScopes, publicClient);

            String clientsUrl = keycloakUrl + "/admin/realms/" + keycloakRealm + "/clients";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            restClient.post()
                    .uri(clientsUrl)
                    .headers(h -> h.addAll(headers))
                    .body(clientRep)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Registered Keycloak SMART client '{}' for app '{}'", clientId, appSlug);
            return clientId;

        } catch (Exception e) {
            log.error("Failed to register Keycloak client for app '{}': {}", appSlug, e.getMessage());
            // Return the clientId anyway — the app can still be installed,
            // Keycloak client can be created manually or retried later
            return clientId;
        }
    }

    /**
     * Remove the Keycloak client when a SMART app is uninstalled.
     */
    public void removeSmartClient(String appSlug) {
        if (!keycloakEnabled) {
            return;
        }

        String clientId = toClientId(appSlug);

        try {
            String accessToken = getAdminAccessToken();
            String internalId = findClientByClientId(clientId, accessToken);

            if (internalId == null) {
                log.debug("No Keycloak client found for '{}', nothing to remove", clientId);
                return;
            }

            String clientUrl = keycloakUrl + "/admin/realms/" + keycloakRealm + "/clients/" + internalId;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            restClient.delete()
                    .uri(clientUrl)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .toBodilessEntity();

            log.info("Removed Keycloak SMART client '{}' for app '{}'", clientId, appSlug);

        } catch (Exception e) {
            log.warn("Failed to remove Keycloak client for app '{}': {}", appSlug, e.getMessage());
        }
    }

    /**
     * Get the Keycloak client ID for a given app slug.
     */
    public String toClientId(String appSlug) {
        return "ciyexhub-app-" + appSlug;
    }

    // ── Private Helpers ──────────────────────────────────────

    private Map<String, Object> buildClientRepresentation(
            String clientId,
            String appName,
            List<String> redirectUris,
            List<String> fhirScopes,
            boolean publicClient) {

        Map<String, Object> client = new LinkedHashMap<>();
        client.put("clientId", clientId);
        client.put("name", "Ciyex Hub: " + appName);
        client.put("description", "SMART on FHIR client for " + appName + " (auto-registered by Ciyex Hub)");
        client.put("protocol", "openid-connect");
        client.put("publicClient", publicClient);
        client.put("standardFlowEnabled", true);
        client.put("directAccessGrantsEnabled", false);
        client.put("serviceAccountsEnabled", !publicClient);
        client.put("enabled", true);

        // Redirect URIs
        client.put("redirectUris", redirectUris != null && !redirectUris.isEmpty()
                ? redirectUris
                : List.of("http://localhost:*"));

        // Web origins (derived from redirect URIs)
        List<String> webOrigins = new ArrayList<>();
        if (redirectUris != null) {
            for (String uri : redirectUris) {
                try {
                    java.net.URI parsed = java.net.URI.create(uri);
                    String origin = parsed.getScheme() + "://" + parsed.getHost();
                    if (parsed.getPort() > 0) {
                        origin += ":" + parsed.getPort();
                    }
                    if (!webOrigins.contains(origin)) {
                        webOrigins.add(origin);
                    }
                } catch (Exception ignored) {
                    // Skip malformed URIs
                }
            }
        }
        client.put("webOrigins", webOrigins);

        // SMART on FHIR attributes
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("smart.app", "true");
        attributes.put("smart.app.slug", clientId.replace("ciyexhub-app-", ""));
        if (fhirScopes != null && !fhirScopes.isEmpty()) {
            attributes.put("fhir.scopes", String.join(" ", fhirScopes));
        }
        // PKCE requirement for public clients
        if (publicClient) {
            attributes.put("pkce.code.challenge.method", "S256");
        }
        client.put("attributes", attributes);

        // Default scopes
        client.put("defaultClientScopes", List.of("openid", "profile", "email"));
        client.put("optionalClientScopes", List.of("offline_access"));

        return client;
    }

    private void updateClient(
            String internalId,
            String appName,
            List<String> redirectUris,
            List<String> fhirScopes,
            boolean publicClient,
            String accessToken) {

        Map<String, Object> clientRep = buildClientRepresentation(
                null, appName, redirectUris, fhirScopes, publicClient);
        // Remove clientId from update payload (cannot change)
        clientRep.remove("clientId");

        String clientUrl = keycloakUrl + "/admin/realms/" + keycloakRealm + "/clients/" + internalId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        restClient.put()
                .uri(clientUrl)
                .headers(h -> h.addAll(headers))
                .body(clientRep)
                .retrieve()
                .toBodilessEntity();

        log.info("Updated Keycloak SMART client for app '{}'", appName);
    }

    @SuppressWarnings("unchecked")
    private String findClientByClientId(String clientId, String accessToken) {
        String url = keycloakUrl + "/admin/realms/" + keycloakRealm
                + "/clients?clientId=" + clientId + "&max=1";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<List> response = restClient.get()
                .uri(url)
                .headers(h -> h.addAll(headers))
                .retrieve()
                .toEntity(List.class);

        List<Map<String, Object>> clients = response.getBody();
        if (clients != null && !clients.isEmpty()) {
            return (String) clients.get(0).get("id");
        }
        return null;
    }

    private String getAdminAccessToken() {
        String tokenUrl = keycloakUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "grant_type=client_credentials&client_id=" + serviceClientId
                + "&client_secret=" + serviceClientSecret;

        ResponseEntity<Map> response = restClient.post()
                .uri(tokenUrl)
                .headers(h -> h.addAll(headers))
                .body(body)
                .retrieve()
                .toEntity(Map.class);

        if (response.getBody() != null) {
            return (String) response.getBody().get("access_token");
        }
        throw new RuntimeException("Failed to get Keycloak admin access token");
    }
}
