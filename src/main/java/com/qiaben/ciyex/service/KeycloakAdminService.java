package com.qiaben.ciyex.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
@ConditionalOnProperty(name = "keycloak.enabled", havingValue = "true", matchIfMissing = true)
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

    public boolean updateClientTokenLifespan(int minutes) {
        try {
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

    private String getAdminToken() {
        try {
            String tokenUrl = keycloakServerUrl + "/realms/master/protocol/openid-connect/token";
            
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
                return (String) response.getBody().get("access_token");
            }
        } catch (Exception e) {
            log.error("Failed to get admin token", e);
        }
        return null;
    }



    private boolean updateRealmSettings(String adminToken, int minutes) {
        try {
            String realmUrl = keycloakServerUrl + "/admin/realms/" + realm;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            int seconds = minutes * 60;
            
            // Update realm-level token settings
            Map<String, Object> realmConfig = Map.of(
                "accessTokenLifespan", seconds,
                "ssoSessionIdleTimeout", seconds,
                "ssoSessionMaxLifespan", seconds * 8,
                "accessTokenLifespanForImplicitFlow", seconds,
                "offlineSessionIdleTimeout", seconds * 12,
                "offlineSessionMaxLifespan", seconds * 24,
                "revokeRefreshToken", false,
                "refreshTokenMaxReuse", 0
            );
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(realmConfig, headers);
            ResponseEntity<Void> response = restTemplate.exchange(realmUrl, HttpMethod.PUT, request, Void.class);
            
            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                log.info("Successfully updated Keycloak realm token lifespan to {} minutes", minutes);
                
                // Also update the specific client settings
                updateClientSettings(adminToken, minutes);
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to update realm settings", e);
        }
        return false;
    }

    private void updateClientSettings(String adminToken, int minutes) {
        try {
            // Get client ID first
            String clientsUrl = keycloakServerUrl + "/admin/realms/" + realm + "/clients?clientId=" + clientId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Void> getRequest = new HttpEntity<>(headers);
            ResponseEntity<Object[]> clientsResponse = restTemplate.exchange(clientsUrl, HttpMethod.GET, getRequest, Object[].class);
            
            if (clientsResponse.getBody() != null && clientsResponse.getBody().length > 0) {
                @SuppressWarnings("unchecked")
                Map<String, Object> client = (Map<String, Object>) clientsResponse.getBody()[0];
                String clientUuid = (String) client.get("id");
                
                // Get full client configuration
                String clientUrl = keycloakServerUrl + "/admin/realms/" + realm + "/clients/" + clientUuid;
                HttpEntity<Void> getClientRequest = new HttpEntity<>(headers);
                ResponseEntity<Map> clientResponse = restTemplate.exchange(clientUrl, HttpMethod.GET, getClientRequest, Map.class);
                
                if (clientResponse.getBody() != null) {
                    Map<String, Object> fullClient = clientResponse.getBody();
                    
                    // Update client with proper session settings
                    int seconds = minutes * 60;
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> attributes = (Map<String, Object>) fullClient.getOrDefault("attributes", new java.util.HashMap<>());
                    
                    attributes.put("access.token.lifespan", String.valueOf(seconds));
                    attributes.put("client.session.idle.timeout", String.valueOf(seconds));
                    attributes.put("client.session.max.lifespan", String.valueOf(seconds * 8));
                    attributes.put("client.offline.session.idle.timeout", String.valueOf(seconds * 12));
                    attributes.put("client.offline.session.max.lifespan", String.valueOf(seconds * 24));
                    
                    fullClient.put("attributes", attributes);
                    
                    // Ensure proper client settings for refresh tokens
                    fullClient.put("publicClient", false);
                    fullClient.put("standardFlowEnabled", true);
                    fullClient.put("directAccessGrantsEnabled", true);
                    fullClient.put("serviceAccountsEnabled", false);
                    fullClient.put("authorizationServicesEnabled", false);
                    
                    // Fix refresh token rotation settings
                    attributes.put("use.refresh.tokens", "true");
                    attributes.put("client_credentials.use_refresh_token", "false");
                    attributes.put("refresh.token.max.reuse", "0");
                    attributes.put("revoke.refresh.token", "false");
                    
                    HttpEntity<Map<String, Object>> updateRequest = new HttpEntity<>(fullClient, headers);
                    restTemplate.exchange(clientUrl, HttpMethod.PUT, updateRequest, Void.class);
                    
                    log.info("Updated client {} with token settings: {} minutes", clientId, minutes);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to update client settings: {}", e.getMessage());
        }
    }
}