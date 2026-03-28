package org.ciyex.ehr.controller.portal;

import org.ciyex.ehr.config.KeycloakConfig;
import org.ciyex.ehr.dto.portal.ApiResponse;
import org.ciyex.ehr.dto.portal.PortalLoginRequest;
import org.ciyex.ehr.dto.portal.PortalLoginResponse;
import org.ciyex.ehr.dto.portal.PortalRegisterRequest;
import org.ciyex.ehr.service.portal.PortalAuthService;
import org.hl7.fhir.r4.model.Person;
import org.hl7.fhir.r4.model.ContactPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * ✅ Controller for portal user authentication (registration, login, Keycloak OAuth)
 */
@RestController
@RequestMapping("/api/portal/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(
        origins = {
                "http://localhost:3000",
                "http://127.0.0.1:3000",
                "http://localhost:3001",
                "http://127.0.0.1:3001"
        },
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
        allowCredentials = "true"
)
public class PortalAuthController {

    private final PortalAuthService portalAuthService;
    private final KeycloakConfig keycloakConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * ✅ Register a new portal user — auto-approved by default
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<PortalLoginResponse>> register(@RequestBody PortalRegisterRequest request) {
        log.info("🟢 Portal user registration attempt: {}", request.getEmail());
        return ResponseEntity.ok(portalAuthService.register(request));
    }

    /**
     * ✅ Login portal user using email/password
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<PortalLoginResponse>> login(@RequestBody PortalLoginRequest request) {
        log.info("🟢 Portal user login attempt: {}", request.getEmail());
        return ResponseEntity.ok(portalAuthService.login(request));
    }

    /**
     * ✅ Get user profile by ID
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<PortalLoginResponse>> getProfile(@RequestParam Long userId) {
        return ResponseEntity.ok(portalAuthService.getProfile(userId));
    }

    /**
     * ✅ Keycloak OAuth2 callback for portal users
     *  - Exchanges code for access token
     *  - Auto-creates user if not found in DB
     *  - Returns structured portal response
     */
    @PostMapping("/keycloak-callback")
    public ResponseEntity<?> keycloakCallback(@RequestBody Map<String, String> request) {
        try {
            String code = request.get("code");
            String redirectUri = request.get("redirectUri");
            String codeVerifier = request.get("codeVerifier");

            if (code == null || redirectUri == null || codeVerifier == null) {
                log.error("❌ Missing parameters: code={}, redirectUri={}, verifier={}", code, redirectUri, codeVerifier);
                return ResponseEntity.badRequest().body(Map.of("error", "Missing required parameters"));
            }

            log.info("🔄 Exchanging authorization code for Keycloak token...");

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
            ResponseEntity<Map> response = restTemplate.postForEntity(keycloakConfig.getTokenEndpoint(), requestEntity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("❌ Token exchange failed: {}", response.getStatusCode());
                return ResponseEntity.status(response.getStatusCode())
                        .body(Map.of("success", false, "error", "Token exchange failed"));
            }

            Map<String, Object> tokenData = response.getBody();
            String accessToken = (String) tokenData.get("access_token");

            Map<String, Object> userData = parseJwtPayload(accessToken);
            String email = (String) userData.getOrDefault("email", "");

            if (email.isEmpty()) {
                log.error("❌ No email found in JWT token");
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "Invalid user information in token"));
            }

            // Extract org alias from Keycloak token for FHIR partition
            // Keycloak sends organization as a List, e.g. ["sunrise-family-medicine"]
            // or [{"sunrise-family-medicine": {"id": "..."}}]
            String orgAlias = null;
            Object orgClaim = userData.get("organization");
            log.info("Raw organization claim type: {}, value: {}", orgClaim != null ? orgClaim.getClass().getSimpleName() : "null", orgClaim);
            if (orgClaim instanceof java.util.List<?> orgList && !orgList.isEmpty()) {
                for (Object elem : orgList) {
                    if (elem instanceof String alias && !alias.isBlank()) {
                        orgAlias = alias;
                        break;
                    }
                    if (elem instanceof Map<?, ?> orgDetails) {
                        for (Object key : orgDetails.keySet()) {
                            if (key instanceof String alias && !alias.isBlank()) {
                                orgAlias = alias;
                                break;
                            }
                        }
                        if (orgAlias != null) break;
                    }
                }
            } else if (orgClaim instanceof String alias && !alias.isBlank()) {
                orgAlias = alias;
            } else if (orgClaim instanceof Map<?, ?> orgMap) {
                // Try alias key or first key
                Object alias = orgMap.get("alias");
                if (alias instanceof String a && !a.isBlank()) {
                    orgAlias = a;
                } else if (!orgMap.isEmpty()) {
                    Object firstKey = orgMap.keySet().iterator().next();
                    if (firstKey instanceof String k && !k.isBlank()) {
                        orgAlias = k;
                    }
                }
            }
            if (orgAlias == null || orgAlias.isBlank()) {
                // Try group-based org extraction
                Object groups = userData.get("groups");
                if (groups instanceof java.util.List<?> groupList && !groupList.isEmpty()) {
                    String group = String.valueOf(groupList.get(0));
                    orgAlias = group.startsWith("/") ? group.substring(1) : group;
                }
            }
            log.info("Resolved org alias from Keycloak token: {}", orgAlias);

            // ✅ Ensure portal user exists or auto-create one
            Person person = portalAuthService.ensurePortalUserExistsFromKeycloak(userData, orgAlias);
            log.info("✅ Keycloak user '{}' validated and available in FHIR", email);

            // Extract info from Person resource
            String firstName = person.hasName() ? person.getNameFirstRep().getGivenAsSingleString() : "";
            String lastName = person.hasName() ? person.getNameFirstRep().getFamily() : "";
            String personEmail = person.getTelecom().stream()
                    .filter(cp -> cp.getSystem() == ContactPoint.ContactPointSystem.EMAIL)
                    .map(ContactPoint::getValue)
                    .findFirst()
                    .orElse(email);
            String fhirId = person.getIdElement().getIdPart();

            // ✅ Response for frontend
            Map<String, Object> formattedResponse = Map.of(
                    "success", true,
                    "data", Map.of(
                            "token", accessToken,
                            "refreshToken", tokenData.getOrDefault("refresh_token", ""),
                            "email", personEmail,
                            "firstName", firstName,
                            "lastName", lastName,
                            "userId", fhirId,
                            "fhirId", fhirId,
                            "userType", "PORTAL_USER"
                    )
            );

            return ResponseEntity.ok(formattedResponse);

        } catch (Exception e) {
            log.error("❌ Error during portal Keycloak callback: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Authentication failed: " + e.getMessage()));
        }
    }

    /**
     * ✅ Parse JWT payload without verifying signature (trusted from Keycloak)
     */
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
            log.error("❌ Failed to parse JWT payload", e);
            return Map.of();
        }
    }
}
