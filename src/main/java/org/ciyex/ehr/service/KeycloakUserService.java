package org.ciyex.ehr.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import org.ciyex.ehr.usermgmt.dto.UserResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to manage users in Keycloak using ciyex-app client credentials
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakUserService {
    
    private final RestClient restClient;
    
    @Value("${keycloak.admin.server-url:${keycloak.auth-server-url}}")
    private String keycloakUrl;
    
    @Value("${keycloak.realm}")
    private String keycloakRealm;
    
    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    @Value("${keycloak.admin.username:}")
    private String adminUsername;

    @Value("${keycloak.admin.password:}")
    private String adminPassword;
    
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
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            log.error("Failed to create Keycloak user: {} — HTTP {} — {}", email, e.getStatusCode(), body);
            throw new RuntimeException("Failed to create user in Keycloak: " + e.getStatusCode() + " — " + body, e);
        } catch (Exception e) {
            log.error("Failed to create Keycloak user: {}", email, e);
            throw new RuntimeException("Failed to create user in Keycloak: " + e.getMessage(), e);
        }
    }
    
    /**
     * Add user to a Keycloak organization (KC 24+)
     */
    public void addUserToOrganization(String userId, String orgAlias) {
        try {
            String accessToken = getAdminAccessToken();
            String orgId = getOrganizationIdByAlias(orgAlias, accessToken);

            if (orgId == null) {
                log.warn("Organization not found for alias: {}", orgAlias);
                return;
            }

            String url = keycloakUrl + "/admin/realms/" + keycloakRealm
                    + "/organizations/" + orgId + "/members";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Keycloak 26+ API: POST with user ID as a JSON-quoted string.
            // RestClient uses StringHttpMessageConverter for String bodies (sends raw),
            // so we must manually wrap in quotes to form valid JSON.
            restClient.post()
                    .uri(url)
                    .headers(h -> h.addAll(headers))
                    .body("\"" + userId + "\"")
                    .retrieve()
                    .toBodilessEntity();

            log.info("Added user {} to organization {}", userId, orgAlias);
        } catch (Exception e) {
            log.error("Failed to add user {} to organization {}", userId, orgAlias, e);
            throw new RuntimeException("Failed to add user to organization", e);
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

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> allRealmRoles = rolesResponse.getBody().stream()
                    .map(role -> (Map<String, Object>) role)
                    .toList();

            Set<String> existingRoleNames = allRealmRoles.stream()
                    .map(r -> (String) r.get("name"))
                    .collect(java.util.stream.Collectors.toSet());

            // Auto-create missing realm roles (for custom roles created in the UI)
            for (String roleName : roleNames) {
                if (!existingRoleNames.contains(roleName)) {
                    createRealmRole(accessToken, roleName);
                }
            }

            // Re-fetch roles after creating any new ones
            if (roleNames.stream().anyMatch(r -> !existingRoleNames.contains(r))) {
                rolesResponse = restClient.get()
                        .uri(rolesUrl)
                        .headers(h -> h.addAll(headers))
                        .retrieve()
                        .toEntity(List.class);
                //noinspection unchecked
                allRealmRoles = rolesResponse.getBody().stream()
                        .map(role -> (Map<String, Object>) role)
                        .toList();
            }

            // Filter roles to assign
            List<Map<String, Object>> rolesToAssign = allRealmRoles.stream()
                    .filter(role -> roleNames.contains(role.get("name")))
                    .toList();

            if (rolesToAssign.isEmpty()) {
                log.warn("No matching realm roles found for {}", roleNames);
                return;
            }

            // Remove ALL existing realm role mappings first (clean slate)
            removeAllRealmRoles(accessToken, userId);

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
     * Creates a realm role in Keycloak if it doesn't exist.
     */
    private void createRealmRole(String accessToken, String roleName) {
        try {
            String rolesUrl = keycloakUrl + "/admin/realms/" + keycloakRealm + "/roles";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> roleBody = Map.of(
                    "name", roleName,
                    "description", "Custom EHR role: " + roleName
            );

            restClient.post()
                    .uri(rolesUrl)
                    .headers(h -> h.addAll(headers))
                    .body(roleBody)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Created Keycloak realm role: {}", roleName);
        } catch (Exception e) {
            log.warn("Failed to create Keycloak realm role '{}': {}", roleName, e.getMessage());
        }
    }

    /**
     * Removes all realm role mappings from a user (except Keycloak defaults).
     */
    private void removeAllRealmRoles(String accessToken, String userId) {
        try {
            String userRolesUrl = keycloakUrl + "/admin/realms/" + keycloakRealm
                    + "/users/" + userId + "/role-mappings/realm";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            ResponseEntity<List> currentRoles = restClient.get()
                    .uri(userRolesUrl)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .toEntity(List.class);

            if (currentRoles.getBody() != null && !currentRoles.getBody().isEmpty()) {
                @SuppressWarnings("unchecked")
                List<Object> rawRoles = currentRoles.getBody();
                List<Map<String, Object>> rolesToRemove = rawRoles.stream()
                        .map(r -> (Map<String, Object>) r)
                        .filter(r -> {
                            String name = (String) r.get("name");
                            // Keep Keycloak internal defaults
                            return name != null
                                    && !name.startsWith("default-roles-")
                                    && !name.equals("offline_access")
                                    && !name.equals("uma_authorization");
                        })
                        .toList();

                if (!rolesToRemove.isEmpty()) {
                    HttpHeaders deleteHeaders = new HttpHeaders();
                    deleteHeaders.setBearerAuth(accessToken);
                    deleteHeaders.setContentType(MediaType.APPLICATION_JSON);

                    restClient.method(org.springframework.http.HttpMethod.DELETE)
                            .uri(userRolesUrl)
                            .headers(h -> h.addAll(deleteHeaders))
                            .body(rolesToRemove)
                            .retrieve()
                            .toBodilessEntity();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to remove existing roles for user {}: {}", userId, e.getMessage());
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
    
    // ── List users by organization ──

    @SuppressWarnings("unchecked")
    public List<UserResponse> listUsersByOrg(String orgAlias, int first, int max, String search) {
        try {
            String accessToken = getAdminAccessToken();

            // Use Keycloak Organizations API (KC 24+)
            String orgId = getOrganizationIdByAlias(orgAlias, accessToken);

            List<Map<String, Object>> users;
            if (orgId != null) {
                String url = keycloakUrl + "/admin/realms/" + keycloakRealm
                        + "/organizations/" + orgId + "/members?first=" + first + "&max=" + max;
                if (search != null && !search.isBlank()) {
                    url += "&search=" + search;
                }
                users = fetchUserList(url, accessToken);
            } else {
                log.warn("Organization not found for alias '{}', falling back to realm user search", orgAlias);
                String url = keycloakUrl + "/admin/realms/" + keycloakRealm
                        + "/users?first=" + first + "&max=" + max;
                if (search != null && !search.isBlank()) {
                    url += "&search=" + search;
                }
                users = fetchUserList(url, accessToken);
            }

            List<UserResponse> result = new ArrayList<>();
            for (Map<String, Object> u : users) {
                result.add(mapUserRepresentation(u, accessToken));
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to list users for org {}", orgAlias, e);
            throw new RuntimeException("Failed to list users", e);
        }
    }

    // ── Get user by ID ──

    @SuppressWarnings("unchecked")
    public UserResponse getUserById(String userId) {
        try {
            String accessToken = getAdminAccessToken();
            String url = keycloakUrl + "/admin/realms/" + keycloakRealm + "/users/" + userId;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            ResponseEntity<Map> response = restClient.get()
                    .uri(url)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .toEntity(Map.class);

            return mapUserRepresentation(response.getBody(), accessToken);
        } catch (Exception e) {
            log.error("Failed to get user {}", userId, e);
            throw new RuntimeException("Failed to get user", e);
        }
    }

    // ── Update user ──

    public void updateUser(String userId, String firstName, String lastName, String email,
                           String phone, Boolean enabled) {
        try {
            String accessToken = getAdminAccessToken();
            String url = keycloakUrl + "/admin/realms/" + keycloakRealm + "/users/" + userId;

            // Fetch current user
            HttpHeaders getHeaders = new HttpHeaders();
            getHeaders.setBearerAuth(accessToken);

            ResponseEntity<Map> current = restClient.get()
                    .uri(url)
                    .headers(h -> h.addAll(getHeaders))
                    .retrieve()
                    .toEntity(Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> userRep = new HashMap<>(current.getBody());
            if (firstName != null) userRep.put("firstName", firstName);
            if (lastName != null) userRep.put("lastName", lastName);
            if (email != null) {
                // Update both email and username — email is used as username in this system
                userRep.put("email", email);
                userRep.put("username", email);
            }
            if (enabled != null) userRep.put("enabled", enabled);

            if (phone != null) {
                @SuppressWarnings("unchecked")
                Map<String, List<String>> attrs = (Map<String, List<String>>)
                        userRep.getOrDefault("attributes", new HashMap<>());
                attrs.put("phone", List.of(phone));
                userRep.put("attributes", attrs);
            }

            HttpHeaders putHeaders = new HttpHeaders();
            putHeaders.setBearerAuth(accessToken);
            putHeaders.setContentType(MediaType.APPLICATION_JSON);

            restClient.put()
                    .uri(url)
                    .headers(h -> h.addAll(putHeaders))
                    .body(userRep)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Updated Keycloak user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to update user {}", userId, e);
            throw new RuntimeException("Failed to update user", e);
        }
    }

    /**
     * Update custom attributes on an existing Keycloak user.
     */
    @SuppressWarnings("unchecked")
    public void updateUserAttributes(String userId, Map<String, String> newAttributes) {
        try {
            String accessToken = getAdminAccessToken();
            String url = keycloakUrl + "/admin/realms/" + keycloakRealm + "/users/" + userId;

            HttpHeaders getHeaders = new HttpHeaders();
            getHeaders.setBearerAuth(accessToken);

            ResponseEntity<Map> current = restClient.get()
                    .uri(url)
                    .headers(h -> h.addAll(getHeaders))
                    .retrieve()
                    .toEntity(Map.class);

            Map<String, Object> userRep = new HashMap<>(current.getBody());
            Map<String, List<String>> attrs = (Map<String, List<String>>)
                    userRep.getOrDefault("attributes", new HashMap<>());
            for (var entry : newAttributes.entrySet()) {
                attrs.put(entry.getKey(), List.of(entry.getValue()));
            }
            userRep.put("attributes", attrs);

            HttpHeaders putHeaders = new HttpHeaders();
            putHeaders.setBearerAuth(accessToken);
            putHeaders.setContentType(MediaType.APPLICATION_JSON);

            restClient.put()
                    .uri(url)
                    .headers(h -> h.addAll(putHeaders))
                    .body(userRep)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Updated attributes on Keycloak user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to update attributes for user {}", userId, e);
            throw new RuntimeException("Failed to update user attributes", e);
        }
    }

    // ── Enable / Disable user ──

    public void disableUser(String userId) {
        updateUser(userId, null, null, null, null, false);
        log.info("Disabled Keycloak user: {}", userId);
    }

    public void enableUser(String userId) {
        updateUser(userId, null, null, null, null, true);
        log.info("Enabled Keycloak user: {}", userId);
    }

    // ── Reset password (generate temp) ──

    public String resetPassword(String userId) {
        try {
            String accessToken = getAdminAccessToken();
            String tempPassword = generateTempPassword();

            String url = keycloakUrl + "/admin/realms/" + keycloakRealm + "/users/" + userId + "/reset-password";

            Map<String, Object> credential = new HashMap<>();
            credential.put("type", "password");
            credential.put("value", tempPassword);
            credential.put("temporary", true);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            restClient.put()
                    .uri(url)
                    .headers(h -> h.addAll(headers))
                    .body(credential)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Reset password for user: {}", userId);
            return tempPassword;
        } catch (Exception e) {
            log.error("Failed to reset password for user {}", userId, e);
            throw new RuntimeException("Failed to reset password", e);
        }
    }

    /**
     * Set a user's password with explicit temporary flag.
     * When temporary=false, removes the UPDATE_PASSWORD required action.
     */
    public void setPassword(String userId, String newPassword, boolean temporary) {
        try {
            String accessToken = getAdminAccessToken();
            String url = keycloakUrl + "/admin/realms/" + keycloakRealm + "/users/" + userId + "/reset-password";

            Map<String, Object> credential = new HashMap<>();
            credential.put("type", "password");
            credential.put("value", newPassword);
            credential.put("temporary", temporary);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            restClient.put()
                    .uri(url)
                    .headers(h -> h.addAll(headers))
                    .body(credential)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Set password for user {} (temporary={})", userId, temporary);
        } catch (Exception e) {
            log.error("Failed to set password for user {}", userId, e);
            throw new RuntimeException("Failed to set password", e);
        }
    }

    // ── Send password reset email ──

    public void sendPasswordResetEmail(String userId) {
        try {
            String accessToken = getAdminAccessToken();
            String url = keycloakUrl + "/admin/realms/" + keycloakRealm
                    + "/users/" + userId + "/execute-actions-email";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            restClient.put()
                    .uri(url)
                    .headers(h -> h.addAll(headers))
                    .body(List.of("UPDATE_PASSWORD"))
                    .retrieve()
                    .toBodilessEntity();

            log.info("Sent password reset email for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send reset email for user {}", userId, e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    // ── Set user attributes ──

    @SuppressWarnings("unchecked")
    public void setUserAttributes(String userId, Map<String, String> newAttributes) {
        try {
            String accessToken = getAdminAccessToken();
            String url = keycloakUrl + "/admin/realms/" + keycloakRealm + "/users/" + userId;

            // Fetch current user
            HttpHeaders getHeaders = new HttpHeaders();
            getHeaders.setBearerAuth(accessToken);

            ResponseEntity<Map> current = restClient.get()
                    .uri(url)
                    .headers(h -> h.addAll(getHeaders))
                    .retrieve()
                    .toEntity(Map.class);

            Map<String, Object> userRep = new HashMap<>(current.getBody());

            // Merge new attributes into existing
            Map<String, List<String>> attrs = (Map<String, List<String>>)
                    userRep.getOrDefault("attributes", new HashMap<>());
            for (Map.Entry<String, String> entry : newAttributes.entrySet()) {
                attrs.put(entry.getKey(), List.of(entry.getValue()));
            }
            userRep.put("attributes", attrs);

            HttpHeaders putHeaders = new HttpHeaders();
            putHeaders.setBearerAuth(accessToken);
            putHeaders.setContentType(MediaType.APPLICATION_JSON);

            restClient.put()
                    .uri(url)
                    .headers(h -> h.addAll(putHeaders))
                    .body(userRep)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Updated attributes for user {}: {}", userId, newAttributes.keySet());
        } catch (Exception e) {
            log.error("Failed to set attributes for user {}", userId, e);
            throw new RuntimeException("Failed to set user attributes", e);
        }
    }

    // ── Get user realm roles ──

    @SuppressWarnings("unchecked")
    public List<String> getUserRoles(String userId) {
        try {
            String accessToken = getAdminAccessToken();
            String url = keycloakUrl + "/admin/realms/" + keycloakRealm
                    + "/users/" + userId + "/role-mappings/realm";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            ResponseEntity<List> response = restClient.get()
                    .uri(url)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .toEntity(List.class);

            if (response.getBody() == null) return List.of();

            return response.getBody().stream()
                    .map(r -> (String) ((Map<String, Object>) r).get("name"))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to get roles for user {}", userId, e);
            return List.of();
        }
    }

    // ── Get user groups ──

    @SuppressWarnings("unchecked")
    public List<String> getUserGroups(String userId) {
        try {
            String accessToken = getAdminAccessToken();
            String url = keycloakUrl + "/admin/realms/" + keycloakRealm
                    + "/users/" + userId + "/groups";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            ResponseEntity<List> response = restClient.get()
                    .uri(url)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .toEntity(List.class);

            if (response.getBody() == null) return List.of();

            return response.getBody().stream()
                    .map(g -> (String) ((Map<String, Object>) g).get("path"))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to get groups for user {}", userId, e);
            return List.of();
        }
    }

    // Private helper methods

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchUserList(String url, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<List> response = restClient.get()
                .uri(url)
                .headers(h -> h.addAll(headers))
                .retrieve()
                .toEntity(List.class);

        return response.getBody() != null ? response.getBody() : List.of();
    }

    @SuppressWarnings("unchecked")
    private UserResponse mapUserRepresentation(Map<String, Object> u, String accessToken) {
        String uid = (String) u.get("id");
        List<String> roles = getUserRoles(uid);
        List<String> groups = getUserGroups(uid);

        // Extract attributes
        String phone = null;
        String practitionerFhirId = null;
        String npi = null;
        Map<String, List<String>> attrs = (Map<String, List<String>>) u.get("attributes");
        if (attrs != null) {
            if (attrs.containsKey("phone") && !attrs.get("phone").isEmpty()) {
                phone = attrs.get("phone").get(0);
            }
            if (attrs.containsKey("practitioner_fhir_id") && !attrs.get("practitioner_fhir_id").isEmpty()) {
                practitionerFhirId = attrs.get("practitioner_fhir_id").get(0);
            }
            if (attrs.containsKey("npi") && !attrs.get("npi").isEmpty()) {
                npi = attrs.get("npi").get(0);
            }
        }

        return UserResponse.builder()
                .id(uid)
                .username((String) u.get("username"))
                .email((String) u.get("email"))
                .firstName((String) u.get("firstName"))
                .lastName((String) u.get("lastName"))
                .phone(phone)
                .enabled(Boolean.TRUE.equals(u.get("enabled")))
                .emailVerified(Boolean.TRUE.equals(u.get("emailVerified")))
                .roles(roles)
                .groups(groups)
                .createdTimestamp(u.get("createdTimestamp") instanceof Number n ? n.longValue() : null)
                .practitionerFhirId(practitionerFhirId)
                .npi(npi)
                .build();
    }

    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$";

    public String generateTempPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(TEMP_PASSWORD_CHARS.charAt(random.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * Get admin access token for Keycloak Admin REST API.
     * Uses admin username/password (resource owner password grant) when configured,
     * otherwise falls back to client credentials grant.
     */
    private String getAdminAccessToken() {
        try {
            String tokenUrl = keycloakUrl + "/realms/master/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body;
            if (adminUsername != null && !adminUsername.isBlank()
                    && adminPassword != null && !adminPassword.isBlank()) {
                // Use Keycloak admin credentials (admin-cli client in master realm)
                body = "grant_type=password"
                        + "&client_id=admin-cli"
                        + "&username=" + URLEncoder.encode(adminUsername, StandardCharsets.UTF_8)
                        + "&password=" + URLEncoder.encode(adminPassword, StandardCharsets.UTF_8);
                tokenUrl = keycloakUrl + "/realms/master/protocol/openid-connect/token";
            } else {
                // Fallback: client credentials (requires confidential client with service account)
                body = "grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret;
                tokenUrl = keycloakUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/token";
            }

            ResponseEntity<Map> response = restClient.post()
                    .uri(tokenUrl)
                    .headers(h -> h.addAll(headers))
                    .body(body)
                    .retrieve()
                    .toEntity(Map.class);

            if (response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            }

            throw new RuntimeException("Failed to get access token: empty response");
        } catch (RestClientResponseException e) {
            log.error("Failed to get admin access token — HTTP {} — {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to authenticate with Keycloak: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("Failed to get admin access token", e);
            throw new RuntimeException("Failed to authenticate with Keycloak: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get user attributes by Keycloak user ID (lightweight — no role/group lookup).
     * Returns a map of attribute name → first value.
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> getUserAttributes(String userId) {
        try {
            String accessToken = getAdminAccessToken();
            String url = keycloakUrl + "/admin/realms/" + keycloakRealm + "/users/" + userId;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            ResponseEntity<Map> response = restClient.get()
                    .uri(url)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .toEntity(Map.class);

            Map<String, String> result = new HashMap<>();
            if (response.getBody() != null) {
                Map<String, List<String>> attrs = (Map<String, List<String>>) response.getBody().get("attributes");
                if (attrs != null) {
                    for (Map.Entry<String, List<String>> entry : attrs.entrySet()) {
                        if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                            result.put(entry.getKey(), entry.getValue().get(0));
                        }
                    }
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to get attributes for user {}: {}", userId, e.getMessage());
            return Map.of();
        }
    }

    /**
     * Find a Keycloak user by a custom attribute (e.g. practitioner_fhir_id, patient_fhir_id).
     * Returns the user representation map, or null if not found.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> findUserByAttribute(String attrName, String attrValue) {
        if (attrValue == null || attrValue.isBlank()) return null;
        try {
            String accessToken = getAdminAccessToken();
            // Keycloak supports attribute search via q parameter
            String url = keycloakUrl + "/admin/realms/" + keycloakRealm
                    + "/users?q=" + attrName + ":" + attrValue + "&max=1";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            ResponseEntity<List> response = restClient.get()
                    .uri(url)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .toEntity(List.class);

            if (response.getBody() != null && !response.getBody().isEmpty()) {
                return (Map<String, Object>) response.getBody().get(0);
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to find user by attribute {}={}: {}", attrName, attrValue, e.getMessage());
            return null;
        }
    }

    /**
     * Look up a Keycloak organization by alias and return its ID.
     */
    @SuppressWarnings("unchecked")
    private String getOrganizationIdByAlias(String orgAlias, String accessToken) {
        if (orgAlias == null || orgAlias.isBlank()) return null;
        try {
            // Keycloak 26+ 'search' param matches against org name, not alias.
            // Use fuzzy search (no exact=true) and fetch enough results to find our alias.
            String url = keycloakUrl + "/admin/realms/" + keycloakRealm
                    + "/organizations?first=0&max=100";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            ResponseEntity<List> response = restClient.get()
                    .uri(url)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .toEntity(List.class);

            if (response.getBody() != null) {
                for (Object item : response.getBody()) {
                    Map<String, Object> org = (Map<String, Object>) item;
                    String alias = (String) org.get("alias");
                    if (orgAlias.equals(alias)) {
                        return (String) org.get("id");
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to get organization ID for alias: {}", orgAlias, e);
            return null;
        }
    }
}
