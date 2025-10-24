package com.qiaben.ciyex.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service to load organization information from Keycloak tenant group attributes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakOrgService {
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${keycloak.auth-server-url}")
    private String keycloakUrl;
    
    @Value("${keycloak.realm}")
    private String keycloakRealm;
    
    @Value("${keycloak.admin.username:aran-admin}")
    private String adminUsername;
    
    @Value("${keycloak.admin.password:}")
    private String adminPassword;
    
    /**
     * Get organization configuration from Keycloak group attributes
     * @param tenantGroupName The tenant group name (e.g., "/Tenants/CareWell")
     * @return Map of organization attributes
     */
    public Map<String, Object> getOrgConfig(String tenantGroupName) {
        try {
            String accessToken = getAdminAccessToken();
            String groupId = getGroupIdByPath(tenantGroupName, accessToken);
            
            if (groupId == null) {
                log.warn("Group not found: {}", tenantGroupName);
                return new HashMap<>();
            }
            
            return getGroupAttributes(groupId, accessToken);
        } catch (Exception e) {
            log.error("Failed to get org config from Keycloak for group: {}", tenantGroupName, e);
            return new HashMap<>();
        }
    }
    
    /**
     * Get organization name from group attributes
     */
    public String getOrgName(String tenantGroupName) {
        Map<String, Object> config = getOrgConfig(tenantGroupName);
        return (String) config.getOrDefault("org_name", extractOrgNameFromPath(tenantGroupName));
    }
    
    /**
     * Get schema name from group attributes
     */
    public String getSchemaName(String tenantGroupName) {
        Map<String, Object> config = getOrgConfig(tenantGroupName);
        return (String) config.get("schema_name");
    }
    
    /**
     * Get storage type from group attributes (e.g., "fhir", "local")
     */
    public String getStorageType(String tenantGroupName) {
        Map<String, Object> config = getOrgConfig(tenantGroupName);
        return (String) config.get("storage_type");
    }
    
    /**
     * Get FHIR server URL from group attributes
     */
    public String getFhirServerUrl(String tenantGroupName) {
        Map<String, Object> config = getOrgConfig(tenantGroupName);
        return (String) config.get("fhir_server_url");
    }
    
    /**
     * Get all organization attributes
     */
    public Map<String, Object> getAllAttributes(String tenantGroupName) {
        return getOrgConfig(tenantGroupName);
    }
    
    /**
     * Update organization attributes in Keycloak
     */
    public void updateOrgAttributes(String tenantGroupName, Map<String, Object> attributes) {
        try {
            String accessToken = getAdminAccessToken();
            String groupId = getGroupIdByPath(tenantGroupName, accessToken);
            
            if (groupId == null) {
                log.warn("Group not found: {}", tenantGroupName);
                return;
            }
            
            updateGroupAttributes(groupId, attributes, accessToken);
            log.info("Updated org attributes for group: {}", tenantGroupName);
        } catch (Exception e) {
            log.error("Failed to update org attributes in Keycloak for group: {}", tenantGroupName, e);
            throw new RuntimeException("Failed to update org attributes", e);
        }
    }
    
    // ===== Private Helper Methods =====
    
    private String getAdminAccessToken() {
        try {
            String tokenUrl = keycloakUrl + "/realms/master/protocol/openid-connect/token";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            String body = "grant_type=password&client_id=admin-cli&username=" + adminUsername + "&password=" + adminPassword;
            
            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
            
            if (response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            }
            
            throw new RuntimeException("Failed to get admin access token");
        } catch (Exception e) {
            log.error("Failed to get admin access token", e);
            throw new RuntimeException("Failed to authenticate with Keycloak", e);
        }
    }
    
    private String getGroupIdByPath(String groupPath, String accessToken) {
        try {
            String groupsUrl = keycloakUrl + "/admin/realms/" + keycloakRealm + "/groups";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<List> response = restTemplate.exchange(groupsUrl, HttpMethod.GET, request, List.class);
            
            if (response.getBody() != null) {
                return findGroupIdByPath(response.getBody(), groupPath);
            }
            
            return null;
        } catch (Exception e) {
            log.error("Failed to get group ID for path: {}", groupPath, e);
            return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    private String findGroupIdByPath(List<Map<String, Object>> groups, String targetPath) {
        for (Map<String, Object> group : groups) {
            String path = (String) group.get("path");
            if (targetPath.equals(path)) {
                return (String) group.get("id");
            }
            
            // Check subgroups
            List<Map<String, Object>> subGroups = (List<Map<String, Object>>) group.get("subGroups");
            if (subGroups != null && !subGroups.isEmpty()) {
                String found = findGroupIdByPath(subGroups, targetPath);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> getGroupAttributes(String groupId, String accessToken) {
        try {
            String groupUrl = keycloakUrl + "/admin/realms/" + keycloakRealm + "/groups/" + groupId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(groupUrl, HttpMethod.GET, request, Map.class);
            
            if (response.getBody() != null) {
                Map<String, Object> attributes = (Map<String, Object>) response.getBody().get("attributes");
                if (attributes != null) {
                    // Flatten list values to single values
                    Map<String, Object> flatAttributes = new HashMap<>();
                    for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                        if (entry.getValue() instanceof List) {
                            List<?> list = (List<?>) entry.getValue();
                            flatAttributes.put(entry.getKey(), list.isEmpty() ? null : list.get(0));
                        } else {
                            flatAttributes.put(entry.getKey(), entry.getValue());
                        }
                    }
                    return flatAttributes;
                }
            }
            
            return new HashMap<>();
        } catch (Exception e) {
            log.error("Failed to get group attributes for groupId: {}", groupId, e);
            return new HashMap<>();
        }
    }
    
    private void updateGroupAttributes(String groupId, Map<String, Object> attributes, String accessToken) {
        try {
            String groupUrl = keycloakUrl + "/admin/realms/" + keycloakRealm + "/groups/" + groupId;
            
            // Convert attributes to Keycloak format (values must be lists)
            Map<String, List<String>> keycloakAttributes = new HashMap<>();
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                keycloakAttributes.put(entry.getKey(), List.of(String.valueOf(entry.getValue())));
            }
            
            Map<String, Object> body = Map.of("attributes", keycloakAttributes);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.exchange(groupUrl, HttpMethod.PUT, request, Void.class);
            
        } catch (Exception e) {
            log.error("Failed to update group attributes for groupId: {}", groupId, e);
            throw new RuntimeException("Failed to update group attributes", e);
        }
    }
    
    private String extractOrgNameFromPath(String groupPath) {
        // Extract org name from path like "/Tenants/CareWell" -> "CareWell"
        if (groupPath != null && groupPath.contains("/")) {
            String[] parts = groupPath.split("/");
            return parts[parts.length - 1];
        }
        return groupPath;
    }
}
