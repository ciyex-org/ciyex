package com.qiaben.ciyex.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.Map;

@Service
@Slf4j
public class KeycloakAdminService {

    @Value("${keycloak.auth-server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    @Value("${keycloak.admin.username}")
    private String adminUsername;

    @Value("${keycloak.admin.password}")
    private String adminPassword;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Updates Keycloak realm token lifespan settings
     * @param minutes Token lifespan in minutes (5-30)
     * @return true if successful, false otherwise
     */
    public boolean updateClientTokenLifespan(int minutes) {
        try {
            log.info("Updating Keycloak token lifespan to {} minutes", minutes);
            
            // Validate input
            if (minutes < 5 || minutes > 30) {
                log.error("Invalid token lifespan: {} minutes. Must be between 5-30", minutes);
                return false;
            }
            
            String adminToken = getAdminToken();
            if (adminToken == null) {
                log.error("Failed to get admin token");
                return false;
            }

            return updateRealmSettings(adminToken, minutes);
        } catch (Exception e) {
            log.error("Failed to update Keycloak token lifespan", e);
            return false;
        }
    }

    /**
     * Gets admin token from Keycloak
     * @return admin access token or null if failed
     */
    private String getAdminToken() {
        try {
            String tokenUrl = keycloakServerUrl + "/realms/master/protocol/openid-connect/token";
            log.debug("Getting admin token from: {}", tokenUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "password");
            params.add("client_id", "admin-cli");
            params.add("username", adminUsername);
            params.add("password", adminPassword);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String token = (String) response.getBody().get("access_token");
                log.debug("Successfully obtained admin token");
                return token;
            } else {
                log.error("Failed to get admin token. Status: {}", response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            log.error("HTTP error getting admin token: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            log.error("Connection error getting admin token: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error getting admin token", e);
        }
        return null;
    }

    /**
     * Updates realm settings with new token lifespan
     * @param adminToken Admin access token
     * @param minutes Token lifespan in minutes
     * @return true if successful, false otherwise
     */
    private boolean updateRealmSettings(String adminToken, int minutes) {
        try {
            String realmUrl = keycloakServerUrl + "/admin/realms/" + realm;
            log.debug("Updating realm settings at: {}", realmUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            int seconds = minutes * 60;
            log.debug("Setting token lifespan to {} seconds", seconds);

            // Update multiple token-related settings for comprehensive coverage
            Map<String, Object> realmConfig = Map.of(
                    "accessTokenLifespan", seconds,                    // Access token lifespan
                    "ssoSessionIdleTimeout", seconds,                 // SSO session idle timeout
                    "ssoSessionMaxLifespan", seconds * 2,             // SSO session max lifespan (2x access token)
                    "accessTokenLifespanForImplicitFlow", seconds,    // Implicit flow token lifespan
                    "offlineSessionIdleTimeout", seconds * 3          // Offline session timeout (3x access token)
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(realmConfig, headers);

            ResponseEntity<Void> response = restTemplate.exchange(realmUrl, HttpMethod.PUT, request, Void.class);

            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                log.info("✅ Successfully updated Keycloak realm '{}' token lifespan to {} minutes", realm, minutes);
                return true;
            } else {
                log.error("Unexpected response status: {}", response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            log.error("HTTP error updating realm settings: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            log.error("Connection error updating realm settings: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error updating realm settings", e);
        }
        return false;
    }
    
    /**
     * Test connection to Keycloak admin API
     * @return true if connection is successful
     */
    public boolean testConnection() {
        try {
            String adminToken = getAdminToken();
            return adminToken != null;
        } catch (Exception e) {
            log.error("Keycloak connection test failed", e);
            return false;
        }
    }
}