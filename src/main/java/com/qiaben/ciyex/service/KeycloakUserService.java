package com.qiaben.ciyex.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service to manage users in Keycloak using ciyex-app client credentials
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakUserService {
    
    private final RestClient restClient;
    
    @Value("${keycloak.auth-server-url}")
    private String keycloakUrl;
    
    @Value("${keycloak.realm}")
    private String keycloakRealm;
    
    @Value("${keycloak.resource}")
    private String clientId;
    
    @Value("${keycloak.credentials.secret}")
    private String clientSecret;
    
    /**
     * Create a new user in Keycloak
     */
    public String createUser(String email, String firstName, String lastName, String password, 
                            Map<String, String> attributes) {
        try {
            String accessToken = getAdminAccessToken();
            String usersUrl = keycloakUrl + "/admin/realms/" + keycloakRealm + "/users";
            
            // Build user representation
            Map<String, Object> userRep = new HashMap<>();
            userRep.put("username", email);
            userRep.put("email", email);
            userRep.put("firstName", firstName);
            userRep.put("lastName", lastName);
            userRep.put("enabled", true);
            userRep.put("emailVerified", true);
            
            // Add attributes
            if (attributes != null && !attributes.isEmpty()) {
                Map<String, List<String>> keycloakAttributes = new HashMap<>();
                for (Map.Entry<String, String> entry : attributes.entrySet()) {
                    keycloakAttributes.put(entry.getKey(), List.of(entry.getValue()));
                }
                userRep.put("attributes", keycloakAttributes);
            }
            
            // Add credentials if password provided
            if (password != null && !password.isEmpty()) {
                Map<String, Object> credential = new HashMap<>();
                credential.put("type", "password");
                credential.put("value", password);
                credential.put("temporary", false);
                userRep.put("credentials", List.of(credential));
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            ResponseEntity<Void> response = restClient.post()
                    .uri(usersUrl)
                    .headers(h -> h.addAll(headers))
                    .body(userRep)
                    .retrieve()
                    .toBodilessEntity();
            
            // Extract user ID from Location header
            String location = response.getHeaders().getFirst("Location");
            if (location != null) {
                String userId = location.substring(location.lastIndexOf('/') + 1);
                log.info("Created Keycloak user: {} with ID: {}", email, userId);
                return userId;
            }
            
            return null;
        } catch (Exception e) {
            log.error("Failed to create Keycloak user: {}", email, e);
            throw new RuntimeException("Failed to create user in Keycloak", e);
        }
    }
    
    /**
     * Add user to a group
     */
    public void addUserToGroup(String userId, String groupPath) {
        try {
            String accessToken = getAdminAccessToken();
            String groupId = getGroupIdByPath(groupPath, accessToken);
            
            if (groupId == null) {
                log.warn("Group not found: {}", groupPath);
                return;
            }
            
            String url = keycloakUrl + "/admin/realms/" + keycloakRealm + "/users/" + userId + "/groups/" + groupId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            
            restClient.put()
                    .uri(url)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .toBodilessEntity();
            
            log.info("Added user {} to group {}", userId, groupPath);
        } catch (Exception e) {
            log.error("Failed to add user {} to group {}", userId, groupPath, e);
            throw new RuntimeException("Failed to add user to group", e);
        }
    }
    
    /**
     * Assign roles to user
     */
    public void assignRolesToUser(String userId, List<String> roleNames) {
        try {
            String accessToken = getAdminAccessToken();
            
            // Get available realm roles
            String rolesUrl = keycloakUrl + "/admin/realms/" + keycloakRealm + "/roles";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            
            ResponseEntity<List> rolesResponse = restClient.get()
                    .uri(rolesUrl)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .toEntity(List.class);
            
            // Filter roles to assign
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rolesToAssign = rolesResponse.getBody().stream()
                    .map(role -> (Map<String, Object>) role)
                    .filter(role -> {
                        Map<String, Object> roleMap = (Map<String, Object>) role;
                        return roleNames.contains(roleMap.get("name"));
                    })
                    .toList();
            
            // Assign roles to user
            String userRolesUrl = keycloakUrl + "/admin/realms/" + keycloakRealm + "/users/" + userId + "/role-mappings/realm";
            HttpHeaders assignHeaders = new HttpHeaders();
            assignHeaders.setBearerAuth(accessToken);
            assignHeaders.setContentType(MediaType.APPLICATION_JSON);
            
            restClient.post()
                    .uri(userRolesUrl)
                    .headers(h -> h.addAll(assignHeaders))
                    .body(rolesToAssign)
                    .retrieve()
                    .toBodilessEntity();
            
            log.info("Assigned roles {} to user {}", roleNames, userId);
        } catch (Exception e) {
            log.error("Failed to assign roles to user {}", userId, e);
            throw new RuntimeException("Failed to assign roles", e);
        }
    }
    
    /**
     * Delete user from Keycloak
     */
    public void deleteUser(String userId) {
        try {
            String accessToken = getAdminAccessToken();
            String url = keycloakUrl + "/admin/realms/" + keycloakRealm + "/users/" + userId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            
            restClient.delete()
                    .uri(url)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .toBodilessEntity();
            
            log.info("Deleted Keycloak user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to delete Keycloak user: {}", userId, e);
            throw new RuntimeException("Failed to delete user", e);
        }
    }
    
    // Private helper methods
    
    /**
     * Get access token using client credentials grant
     * The ciyex-app client must have service account enabled and proper roles assigned
     */
    private String getAdminAccessToken() {
        try {
            String tokenUrl = keycloakUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/token";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            // Use client credentials grant type
            String body = "grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret;
            
            ResponseEntity<Map> response = restClient.post()
                    .uri(tokenUrl)
                    .headers(h -> h.addAll(headers))
                    .body(body)
                    .retrieve()
                    .toEntity(Map.class);
            
            if (response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            }
            
            throw new RuntimeException("Failed to get access token");
        } catch (Exception e) {
            log.error("Failed to get access token using client credentials", e);
            throw new RuntimeException("Failed to authenticate with Keycloak", e);
        }
    }
    
    private String getGroupIdByPath(String groupPath, String accessToken) {
        try {
            String groupsUrl = keycloakUrl + "/admin/realms/" + keycloakRealm + "/groups";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            
            ResponseEntity<List> response = restClient.get()
                    .uri(groupsUrl)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .toEntity(List.class);
            
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
}
