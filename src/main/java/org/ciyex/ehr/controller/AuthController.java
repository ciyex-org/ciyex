package org.ciyex.ehr.controller;

import org.ciyex.ehr.config.KeycloakConfig;
import org.ciyex.ehr.fhir.FhirPartitionService;
import org.ciyex.ehr.service.KeycloakAdminService;
import org.ciyex.ehr.service.KeycloakUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(
    origins = { "http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3001", "http://127.0.0.1:3001" },
    allowedHeaders = "*",
    methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS },
    allowCredentials = "true"
)
public class AuthController {

    private final KeycloakConfig keycloakConfig;
    private final KeycloakAdminService keycloakAdminService;
    private final KeycloakUserService keycloakUserService;
    private final FhirPartitionService fhirPartitionService;
    private final org.ciyex.ehr.marketplace.repository.AppInstallationRepository appInstallationRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/keycloak-callback")
    public ResponseEntity<?> keycloakCallback(@RequestBody Map<String, String> request) {
        try {
            String code = request.get("code");
            String redirectUri = request.get("redirectUri");
            String codeVerifier = request.get("codeVerifier");

            if (code == null || redirectUri == null || codeVerifier == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing required parameters: code, redirectUri, or codeVerifier"));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "authorization_code");
            body.add("client_id", keycloakConfig.getResource());
            body.add("client_secret", keycloakConfig.getClientSecret());
            body.add("code", code);
            body.add("redirect_uri", redirectUri);
            body.add("code_verifier", codeVerifier);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

            String tokenEndpoint = keycloakConfig.getTokenEndpoint();

            ResponseEntity<Map> response = restTemplate.postForEntity(
                tokenEndpoint,
                requestEntity,
                Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> tokenData = response.getBody();
                String accessToken = (String) tokenData.get("access_token");

                Map<String, Object> userData = parseJwtPayload(accessToken);

                Map<String, Object> data = buildAuthResponse(accessToken, tokenData, userData);

                return ResponseEntity.ok(Map.of("success", true, "data", data));
            } else {
                return ResponseEntity.status(response.getStatusCode())
                    .body(Map.of("success", false, "error", "Token exchange failed"));
            }

        } catch (Exception e) {
            log.error("Error during Keycloak callback", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Authentication failed: " + e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        
        if (refreshToken == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "refresh_token required"));
        }

        try {
            String tokenUrl = keycloakConfig.getTokenEndpoint();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "refresh_token");
            params.add("client_id", keycloakConfig.getResource());
            params.add("client_secret", keycloakConfig.getClientSecret());
            params.add("refresh_token", refreshToken);
            
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> tokenData = response.getBody();
                
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                        "token", tokenData.get("access_token"),
                        "refreshToken", tokenData.get("refresh_token")
                    )
                ));
            }
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "error", "invalid_token"));
            
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "error", "refresh_failed"));
        }
    }

    @PostMapping("/discover")
    public ResponseEntity<?> discover(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "email is required"));
        }
        try {
            // Search user by email
            List<Map<String, Object>> users = keycloakAdminService.searchUserByEmail(email);
            if (users.isEmpty()) {
                return ResponseEntity.ok(Map.of("exists", false, "authMethods", List.of(), "idps", List.of()));
            }

            String userId = (String) users.get(0).get("id");

            // Get user's organizations
            List<Map<String, Object>> orgs = keycloakAdminService.getUserOrganizations(userId);
            String orgAlias = "";
            String orgName = "";
            if (!orgs.isEmpty()) {
                orgAlias = (String) orgs.get(0).getOrDefault("alias", "");
                orgName = (String) orgs.get(0).getOrDefault("name", "");
            }

            // Get available IDPs
            List<Map<String, Object>> idps = keycloakAdminService.getRealmIdps();

            // Password is always available for Keycloak users
            List<String> authMethods = new ArrayList<>();
            authMethods.add("password");

            return ResponseEntity.ok(Map.of(
                "exists", true,
                "authMethods", authMethods,
                "idps", idps,
                "orgAlias", orgAlias,
                "orgName", orgName
            ));
        } catch (Exception e) {
            log.error("Discover failed for email {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Discovery failed"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("email");
        if (username == null) username = request.get("username");
        String password = request.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "email and password are required"));
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "password");
            body.add("client_id", keycloakConfig.getResource());
            body.add("client_secret", keycloakConfig.getClientSecret());
            body.add("username", username);
            body.add("password", password);
            body.add("scope", "openid profile email organization");

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(keycloakConfig.getTokenEndpoint(), requestEntity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "Invalid credentials"));
            }

            Map<String, Object> tokenData = response.getBody();
            String accessToken = (String) tokenData.get("access_token");
            Map<String, Object> userData = parseJwtPayload(accessToken);

            Map<String, Object> data = buildAuthResponse(accessToken, tokenData, userData);

            return ResponseEntity.ok(Map.of("success", true, "data", data));
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.warn("Login failed for user {}: {} - {}", username, e.getStatusCode(), e.getResponseBodyAsString());

            // Check if this is a temp password / required action scenario
            String responseBody = e.getResponseBodyAsString();
            if (responseBody != null && responseBody.contains("not fully set up")) {
                // User has required actions (e.g. UPDATE_PASSWORD for temp passwords)
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "requiresPasswordChange", true,
                        "error", "Your password has been reset. Please set a new password."
                ));
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "error", "Invalid username or password"));
        } catch (Exception e) {
            log.error("Login error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Authentication failed"));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null) email = request.get("username");
        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");

        if (email == null || currentPassword == null || newPassword == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "email, currentPassword, and newPassword are required"));
        }

        if (newPassword.length() < 8) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "New password must be at least 8 characters"));
        }

        try {
            // 1. Find the user in Keycloak
            List<Map<String, Object>> users = keycloakAdminService.searchUserByEmail(email);
            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "error", "Invalid credentials"));
            }

            String userId = (String) users.get(0).get("id");

            // 2. Set the new permanent password (removes UPDATE_PASSWORD required action)
            keycloakUserService.setPassword(userId, newPassword, false);

            // 3. Login with the new password to get tokens
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "password");
            body.add("client_id", keycloakConfig.getResource());
            body.add("client_secret", keycloakConfig.getClientSecret());
            body.add("username", email);
            body.add("password", newPassword);
            body.add("scope", "openid profile email organization");

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    keycloakConfig.getTokenEndpoint(), requestEntity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "error", "Password changed but login failed. Please sign in again."));
            }

            Map<String, Object> tokenData = response.getBody();
            String accessToken = (String) tokenData.get("access_token");
            Map<String, Object> userData = parseJwtPayload(accessToken);

            Map<String, Object> data = buildAuthResponse(accessToken, tokenData, userData);

            return ResponseEntity.ok(Map.of("success", true, "data", data));
        } catch (Exception e) {
            log.error("Password change failed for {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Password change failed: " + e.getMessage()));
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> request) {
        String orgName = request.get("orgName");
        String orgAlias = request.get("orgAlias");
        String firstName = request.get("firstName");
        String lastName = request.get("lastName");
        String email = request.get("email");
        String password = request.get("password");

        if (orgName == null || orgAlias == null || firstName == null || lastName == null || email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "All fields are required"));
        }

        // Normalize org alias (lowercase, hyphens only)
        orgAlias = orgAlias.toLowerCase().replaceAll("[^a-z0-9-]", "-").replaceAll("-+", "-").replaceAll("^-|-$", "");

        try {
            // 1. Create Keycloak Organization
            String orgId = keycloakAdminService.createOrganization(orgName, orgAlias);

            // 2. Create FHIR partition for the new org
            try {
                fhirPartitionService.createPartition(orgAlias, orgName);
            } catch (Exception e) {
                log.warn("Failed to create FHIR partition for {}: {}", orgAlias, e.getMessage());
            }

            // 2b. Auto-install infrastructure apps (e.g., ciyex-codes for CPT/ICD lookups)
            try {
                seedInfrastructureApps(orgAlias);
            } catch (Exception e) {
                log.warn("Failed to seed infrastructure apps for {}: {}", orgAlias, e.getMessage());
            }

            // 3. Create Keycloak User
            String userId = keycloakAdminService.createUser(email, firstName, lastName, password);

            // 4. Add user to organization
            keycloakAdminService.addUserToOrganization(orgId, userId);

            // 5. Assign ADMIN role
            keycloakAdminService.assignRealmRole(userId, "ADMIN");

            // 6. Login the user to get tokens
            Map<String, Object> loginResult = keycloakAdminService.directLogin(email, password);
            if (loginResult == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Account created but login failed. Please sign in manually."));
            }

            String accessToken = (String) loginResult.get("access_token");
            Map<String, Object> userData = parseJwtPayload(accessToken);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                    "token", accessToken,
                    "refreshToken", loginResult.getOrDefault("refresh_token", ""),
                    "email", email,
                    "username", email,
                    "firstName", firstName,
                    "lastName", lastName,
                    "userId", userData.getOrDefault("sub", ""),
                    "groups", userData.getOrDefault("groups", List.of()),
                    "orgAlias", orgAlias,
                    "orgName", orgName
                )
            ));
        } catch (Exception e) {
            log.error("Signup failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Signup failed: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "auth"));
    }
    
    /**
     * Build the auth response data map, enriched with FHIR link attributes from Keycloak.
     */
    private Map<String, Object> buildAuthResponse(String accessToken, Map<String, Object> tokenData,
                                                   Map<String, Object> userData) {
        String userId = (String) userData.getOrDefault("sub", "");

        Map<String, Object> data = new HashMap<>();
        data.put("token", accessToken);
        data.put("refreshToken", tokenData.getOrDefault("refresh_token", ""));
        data.put("email", userData.getOrDefault("email", ""));
        data.put("username", userData.getOrDefault("preferred_username", ""));
        data.put("firstName", userData.getOrDefault("given_name", ""));
        data.put("lastName", userData.getOrDefault("family_name", ""));
        data.put("userId", userId);

        // Extract roles: prefer realm_access.roles (standard JWT), fallback to groups key
        List<String> roles = List.of();
        Object realmAccess = userData.get("realm_access");
        if (realmAccess instanceof Map<?,?> ra) {
            Object raRoles = ra.get("roles");
            if (raRoles instanceof List<?> rl) {
                roles = rl.stream().map(Object::toString).toList();
            }
        }
        if (roles.isEmpty()) {
            Object groups = userData.get("groups");
            if (groups instanceof List<?> gl) {
                roles = gl.stream().map(Object::toString).toList();
            }
        }
        data.put("groups", roles);

        // Enrich with FHIR link info from Keycloak user attributes
        try {
            if (!userId.isBlank()) {
                Map<String, String> attrs = keycloakUserService.getUserAttributes(userId);
                data.put("practitionerFhirId", attrs.getOrDefault("practitioner_fhir_id", ""));
                data.put("patientFhirId", attrs.getOrDefault("patient_fhir_id", ""));
            }
        } catch (Exception e) {
            log.warn("Failed to enrich auth response with FHIR info for user {}: {}", userId, e.getMessage());
            data.put("practitionerFhirId", "");
            data.put("patientFhirId", "");
        }

        return data;
    }

    /**
     * Auto-install infrastructure apps (like ciyex-codes) for a new org so that
     * CPT/ICD/HCPCS code lookups work immediately after signup.
     */
    private void seedInfrastructureApps(String orgAlias) {
        if (appInstallationRepository.existsByOrgIdAndAppSlugAndStatus(orgAlias, "ciyex-codes", "active")) {
            return; // Already installed
        }
        var installation = org.ciyex.ehr.marketplace.entity.AppInstallation.builder()
                .orgId(orgAlias)
                .appId(java.util.UUID.fromString("10000000-0000-0000-0000-000000000005"))
                .appSlug("ciyex-codes")
                .appName("Ciyex Codes")
                .appCategory("INFRASTRUCTURE")
                .status("active")
                .config(java.util.Map.of("service_url", "http://ciyex-codes.ciyex-codes.svc.cluster.local:8080"))
                .extensionPoints(java.util.List.of("encounter:sidebar"))
                .installedBy("system-signup")
                .build();
        appInstallationRepository.save(installation);
        log.info("Auto-installed ciyex-codes for new org: {}", orgAlias);
    }

    private Map<String, Object> parseJwtPayload(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) {
                return Map.of();
            }
            
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(payload, Map.class);
        } catch (Exception e) {
            log.error("Failed to parse JWT payload", e);
            return Map.of();
        }
    }
}
