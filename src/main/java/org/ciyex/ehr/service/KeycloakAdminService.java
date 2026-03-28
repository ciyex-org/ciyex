package org.ciyex.ehr.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.*;

@Service
@Slf4j
public class KeycloakAdminService {

    @Value("${keycloak.admin.server-url:${keycloak.auth-server-url}}")
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
    private final ObjectMapper objectMapper = new ObjectMapper();

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

    /**
     * Search for a user by exact email match.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchUserByEmail(String email) {
        try {
            String adminToken = getAdminToken();
            if (adminToken == null) {
                log.error("Failed to get admin token for user search");
                return List.of();
            }

            String url = keycloakServerUrl + "/admin/realms/" + realm + "/users?email=" + email + "&exact=true";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, request, List.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (List<Map<String, Object>>) response.getBody();
            }
        } catch (Exception e) {
            log.error("Failed to search user by email: {}", email, e);
        }
        return List.of();
    }

    /**
     * Get the organizations a user belongs to.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getUserOrganizations(String userId) {
        try {
            String adminToken = getAdminToken();
            if (adminToken == null) {
                log.error("Failed to get admin token for user organizations");
                return List.of();
            }

            String url = keycloakServerUrl + "/admin/realms/" + realm + "/users/" + userId + "/organizations";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, request, List.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (List<Map<String, Object>>) response.getBody();
            }
        } catch (Exception e) {
            log.error("Failed to get organizations for user: {}", userId, e);
        }
        return List.of();
    }

    /**
     * Get all identity providers configured in the realm.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRealmIdps() {
        try {
            String adminToken = getAdminToken();
            if (adminToken == null) {
                log.error("Failed to get admin token for IDP lookup");
                return List.of();
            }

            String url = keycloakServerUrl + "/admin/realms/" + realm + "/identity-providers/instances";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, request, List.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (List<Map<String, Object>>) response.getBody();
            }
        } catch (Exception e) {
            log.error("Failed to get realm IDPs", e);
        }
        return List.of();
    }

    /**
     * Create an organization in Keycloak. Returns the organization ID from the Location header.
     */
    public String createOrganization(String name, String alias) {
        String adminToken = getAdminToken();
        if (adminToken == null) {
            throw new RuntimeException("Failed to obtain Keycloak admin token — check KEYCLOAK_ADMIN_USERNAME/PASSWORD");
        }

        String url = keycloakServerUrl + "/admin/realms/" + realm + "/organizations";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> orgBody = Map.of(
            "name", name,
            "alias", alias,
            "enabled", true
        );

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(orgBody, headers);
            ResponseEntity<Void> response = restTemplate.postForEntity(url, request, Void.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                URI location = response.getHeaders().getLocation();
                if (location != null) {
                    String path = location.getPath();
                    return path.substring(path.lastIndexOf('/') + 1);
                }
                throw new RuntimeException("Organization created but Keycloak returned no Location header");
            }
            throw new RuntimeException("Unexpected status from Keycloak org creation: " + response.getStatusCode());
        } catch (HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            log.error("Keycloak {} creating org '{}': {}", e.getStatusCode(), alias, body);
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                throw new RuntimeException("Organization alias '" + alias + "' is already taken. Please choose a different one.");
            }
            throw new RuntimeException("Keycloak rejected org creation (" + e.getStatusCode() + "): " + body);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create organization: {}", name, e);
            throw new RuntimeException("Failed to create organization: " + e.getMessage(), e);
        }
    }

    /**
     * Create a user in Keycloak with credentials. Returns the user ID from the Location header.
     */
    public String createUser(String email, String firstName, String lastName, String password) {
        String adminToken = getAdminToken();
        if (adminToken == null) {
            throw new RuntimeException("Failed to obtain Keycloak admin token — check KEYCLOAK_ADMIN_USERNAME/PASSWORD");
        }

        String url = keycloakServerUrl + "/admin/realms/" + realm + "/users";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> userBody = Map.of(
            "username", email,
            "email", email,
            "firstName", firstName,
            "lastName", lastName,
            "enabled", true,
            "emailVerified", true,
            "credentials", List.of(Map.of(
                "type", "password",
                "value", password,
                "temporary", false
            ))
        );

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(userBody, headers);
            ResponseEntity<Void> response = restTemplate.postForEntity(url, request, Void.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                URI location = response.getHeaders().getLocation();
                if (location != null) {
                    String path = location.getPath();
                    return path.substring(path.lastIndexOf('/') + 1);
                }
                throw new RuntimeException("User created but Keycloak returned no Location header");
            }
            throw new RuntimeException("Unexpected status from Keycloak user creation: " + response.getStatusCode());
        } catch (HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            log.error("Keycloak {} creating user '{}': {}", e.getStatusCode(), email, body);
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                throw new RuntimeException("A user with email '" + email + "' already exists.");
            }
            throw new RuntimeException("Keycloak rejected user creation (" + e.getStatusCode() + "): " + body);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create user: {}", email, e);
            throw new RuntimeException("Failed to create user: " + e.getMessage(), e);
        }
    }

    /**
     * Add a user to an organization.
     */
    public void addUserToOrganization(String orgId, String userId) {
        try {
            String adminToken = getAdminToken();
            if (adminToken == null) {
                log.error("Failed to get admin token for org membership");
                return;
            }

            // Keycloak 26+ API: POST /organizations/{orgId}/members with userId as plain string body
            String url = keycloakServerUrl + "/admin/realms/" + realm + "/organizations/" + orgId + "/members";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>("\"" + userId + "\"", headers);
            restTemplate.postForEntity(url, request, Void.class);

            log.info("Added user {} to organization {}", userId, orgId);
        } catch (Exception e) {
            log.error("Failed to add user {} to organization {}", userId, orgId, e);
        }
    }

    /**
     * Assign a realm-level role to a user.
     */
    @SuppressWarnings("unchecked")
    public void assignRealmRole(String userId, String roleName) {
        try {
            String adminToken = getAdminToken();
            if (adminToken == null) {
                log.error("Failed to get admin token for role assignment");
                return;
            }

            // First, look up the role to get its ID and full representation
            String roleUrl = keycloakServerUrl + "/admin/realms/" + realm + "/roles/" + roleName;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> getRequest = new HttpEntity<>(headers);
            ResponseEntity<Map> roleResponse = restTemplate.exchange(roleUrl, HttpMethod.GET, getRequest, Map.class);

            if (!roleResponse.getStatusCode().is2xxSuccessful() || roleResponse.getBody() == null) {
                log.error("Role {} not found in realm", roleName);
                return;
            }

            Map<String, Object> role = roleResponse.getBody();

            // Assign the role to the user
            String assignUrl = keycloakServerUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm";

            HttpEntity<List<Map<String, Object>>> assignRequest = new HttpEntity<>(List.of(role), headers);
            restTemplate.postForEntity(assignUrl, assignRequest, Void.class);

            log.info("Assigned role {} to user {}", roleName, userId);
        } catch (Exception e) {
            log.error("Failed to assign role {} to user {}", roleName, userId, e);
        }
    }

    /**
     * Get all organizations in the realm. Used by ciyex_super_admin for org switching.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllOrganizations() {
        try {
            String adminToken = getAdminToken();
            if (adminToken == null) {
                log.error("Failed to get admin token for listing organizations");
                return List.of();
            }

            String url = keycloakServerUrl + "/admin/realms/" + realm + "/organizations?first=0&max=1000";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, request, List.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (List<Map<String, Object>>) response.getBody();
            }
        } catch (Exception e) {
            log.error("Failed to list all organizations", e);
        }
        return List.of();
    }

    /**
     * Perform a direct access grant (password login) and return the token response.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> directLogin(String username, String password) {
        try {
            String tokenUrl = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "password");
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("username", username);
            params.add("password", password);
            params.add("scope", "openid profile email organization");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (Map<String, Object>) response.getBody();
            }
        } catch (Exception e) {
            log.error("Direct login failed for user: {}", username, e);
        }
        return null;
    }
}