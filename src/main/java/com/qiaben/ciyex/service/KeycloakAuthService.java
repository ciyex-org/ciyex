package com.qiaben.ciyex.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.config.KeycloakConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
@Slf4j
public class KeycloakAuthService {

    @Autowired
    private KeycloakConfig keycloakConfig;

    @Autowired
    private RestClient restClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Authenticate user with Keycloak and get access token
     */
    public Map<String, Object> authenticateWithKeycloak(String username, String password) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "password");
            body.add("client_id", keycloakConfig.getResource());
            body.add("client_secret", keycloakConfig.getClientSecret());
            body.add("username", username);
            body.add("password", password);

            String response = restClient.post()
                    .uri(keycloakConfig.getTokenEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            if (response != null) {
                JsonNode tokenResponse = objectMapper.readTree(response);
                
                Map<String, Object> result = new HashMap<>();
                result.put("access_token", tokenResponse.get("access_token").asText());
                result.put("refresh_token", tokenResponse.get("refresh_token").asText());
                result.put("expires_in", tokenResponse.get("expires_in").asInt());
                result.put("token_type", tokenResponse.get("token_type").asText());
                
                return result;
            } else {
                log.error("Keycloak authentication failed");
                return null;
            }
        } catch (Exception e) {
            log.error("Error authenticating with Keycloak", e);
            return null;
        }
    }

    /**
     * Get user info from Keycloak using access token
     */
    public Map<String, Object> getUserInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            String response = restClient.get()
                    .uri(keycloakConfig.getUserInfoEndpoint())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);

            if (response != null) {
                JsonNode userInfo = objectMapper.readTree(response);
                
                Map<String, Object> result = new HashMap<>();
                result.put("sub", userInfo.get("sub").asText());
                result.put("email", userInfo.has("email") ? userInfo.get("email").asText() : null);
                result.put("preferred_username", userInfo.get("preferred_username").asText());
                result.put("given_name", userInfo.has("given_name") ? userInfo.get("given_name").asText() : null);
                result.put("family_name", userInfo.has("family_name") ? userInfo.get("family_name").asText() : null);
                
                // Extract groups (replacing tenant concept)
                if (userInfo.has("groups")) {
                    List<String> groups = new ArrayList<>();
                    userInfo.get("groups").forEach(group -> groups.add(group.asText()));
                    result.put("groups", groups);
                }
                
                return result;
            } else {
                log.error("Failed to get user info from Keycloak");
                return null;
            }
        } catch (Exception e) {
            log.error("Error getting user info from Keycloak", e);
            return null;
        }
    }

    /**
     * Extract groups from Keycloak token (replaces tenant logic)
     */
    public List<String> extractGroupsFromToken(String accessToken) {
        try {
            // Decode JWT token to extract groups
            String[] parts = accessToken.split("\\.");
            if (parts.length < 2) {
                return Collections.emptyList();
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode claims = objectMapper.readTree(payload);

            List<String> groups = new ArrayList<>();
            
            // Check for groups in token
            if (claims.has("groups")) {
                claims.get("groups").forEach(group -> groups.add(group.asText()));
            }
            
            // Check for realm roles
            if (claims.has("realm_access") && claims.get("realm_access").has("roles")) {
                claims.get("realm_access").get("roles").forEach(role -> groups.add(role.asText()));
            }
            
            return groups;
        } catch (Exception e) {
            log.error("Error extracting groups from token", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Extract email from Keycloak JWT token
     */
    public String extractEmailFromToken(String accessToken) {
        try {
            // Decode JWT token to extract email
            String[] parts = accessToken.split("\\.");
            if (parts.length < 2) {
                return null;
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode claims = objectMapper.readTree(payload);

            // Try to get email from various claims
            if (claims.has("email")) {
                return claims.get("email").asText();
            } else if (claims.has("preferred_username")) {
                return claims.get("preferred_username").asText();
            } else if (claims.has("sub")) {
                return claims.get("sub").asText();
            }
            
            return null;
        } catch (Exception e) {
            log.error("Error extracting email from token", e);
            return null;
        }
    }

    /**
     * Extract group attributes from Keycloak token
     * Returns a map of group path to group attributes
     */
    public Map<String, Map<String, Object>> extractGroupAttributesFromToken(String accessToken) {
        try {
            String[] parts = accessToken.split("\\.");
            if (parts.length < 2) {
                return Collections.emptyMap();
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode claims = objectMapper.readTree(payload);

            Map<String, Map<String, Object>> groupAttributes = new HashMap<>();
            
            // Check for group_attributes in token
            if (claims.has("group_attributes")) {
                JsonNode groupAttrsNode = claims.get("group_attributes");
                groupAttrsNode.fields().forEachRemaining(entry -> {
                    String groupPath = entry.getKey();
                    Map<String, Object> attrs = new HashMap<>();
                    entry.getValue().fields().forEachRemaining(attr -> {
                        attrs.put(attr.getKey(), attr.getValue().asText());
                    });
                    groupAttributes.put(groupPath, attrs);
                });
            }
            
            return groupAttributes;
        } catch (Exception e) {
            log.error("Error extracting group attributes from token", e);
            return Collections.emptyMap();
        }
    }

    /**
     * Get org ID from group attributes
     */
    public Long getOrgIdFromGroupAttributes(Map<String, Map<String, Object>> groupAttributes, String groupPath) {
        if (groupAttributes.containsKey(groupPath)) {
            Map<String, Object> attrs = groupAttributes.get(groupPath);
            if (attrs.containsKey("org_id")) {
                try {
                    return Long.parseLong(attrs.get("org_id").toString());
                } catch (NumberFormatException e) {
                    log.error("Invalid org_id in group attributes: {}", attrs.get("org_id"));
                }
            }
        }
        return null;
    }

    /**
     * Exchange authorization code for access token (OAuth 2.0 Authorization Code Flow with PKCE)
     */
    public Map<String, Object> exchangeCodeForToken(String code, String redirectUri, String codeVerifier) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "authorization_code");
            body.add("client_id", keycloakConfig.getResource());
            body.add("client_secret", keycloakConfig.getClientSecret());
            body.add("code", code);
            body.add("redirect_uri", redirectUri);
            
            // Add PKCE code verifier if provided
            if (codeVerifier != null && !codeVerifier.isEmpty()) {
                body.add("code_verifier", codeVerifier);
            }

            String response = restClient.post()
                    .uri(keycloakConfig.getTokenEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            if (response != null) {
                JsonNode tokenResponse = objectMapper.readTree(response);
                
                Map<String, Object> result = new HashMap<>();
                result.put("access_token", tokenResponse.get("access_token").asText());
                result.put("refresh_token", tokenResponse.has("refresh_token") ? tokenResponse.get("refresh_token").asText() : null);
                result.put("expires_in", tokenResponse.get("expires_in").asInt());
                result.put("token_type", tokenResponse.get("token_type").asText());
                
                return result;
            } else {
                log.error("Token exchange failed");
                return null;
            }
        } catch (Exception e) {
            log.error("Error exchanging code for token", e);
            return null;
        }
    }

    /**
     * Logout user from Keycloak
     */
    public boolean logout(String refreshToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", keycloakConfig.getResource());
            body.add("client_secret", keycloakConfig.getClientSecret());
            body.add("refresh_token", refreshToken);

            restClient.post()
                    .uri(keycloakConfig.getLogoutEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            return true;
        } catch (Exception e) {
            log.error("Error logging out from Keycloak", e);
            return false;
        }
    }
}
