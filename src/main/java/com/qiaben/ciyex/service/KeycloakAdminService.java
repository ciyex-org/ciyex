package com.qiaben.ciyex.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

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
            
            Map<String, Object> realmConfig = Map.of(
                "accessTokenLifespan", seconds,
                "ssoSessionIdleTimeout", seconds,
                "ssoSessionMaxLifespan", seconds * 2,
                "accessTokenLifespanForImplicitFlow", seconds
            );
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(realmConfig, headers);
            
            ResponseEntity<Void> response = restTemplate.exchange(realmUrl, HttpMethod.PUT, request, Void.class);
            
            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                log.info("Successfully updated Keycloak realm token lifespan to {} minutes", minutes);
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to update realm settings", e);
        }
        return false;
    }
}