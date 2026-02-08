package org.ciyex.ehr.controller;

import org.ciyex.ehr.config.KeycloakConfig;
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
    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/keycloak-callback")
    public ResponseEntity<?> keycloakCallback(@RequestBody Map<String, String> request) {
        try {
            String code = request.get("code");
            String redirectUri = request.get("redirectUri");
            String codeVerifier = request.get("codeVerifier");

            log.info("Received Keycloak callback - code: {}, redirectUri: {}", 
                code != null ? code.substring(0, Math.min(20, code.length())) + "..." : "null", 
                redirectUri);

            if (code == null || redirectUri == null || codeVerifier == null) {
                log.error("Missing required parameters");
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
            log.info("Exchanging code for token at: {}", tokenEndpoint);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                tokenEndpoint,
                requestEntity,
                Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully exchanged code for token");
                
                Map<String, Object> tokenData = response.getBody();
                String accessToken = (String) tokenData.get("access_token");
                
                // Decode JWT to get user info (basic parsing without validation since it's from Keycloak)
                Map<String, Object> userData = parseJwtPayload(accessToken);
                
                // Format response for frontend
                Map<String, Object> formattedResponse = Map.of(
                    "success", true,
                    "data", Map.of(
                        "token", accessToken,
                        "refreshToken", tokenData.getOrDefault("refresh_token", ""),
                        "email", userData.getOrDefault("email", ""),
                        "username", userData.getOrDefault("preferred_username", ""),
                        "firstName", userData.getOrDefault("given_name", ""),
                        "lastName", userData.getOrDefault("family_name", ""),
                        "userId", userData.getOrDefault("sub", ""),
                        "groups", userData.getOrDefault("groups", java.util.List.of())
                    )
                );
                
                return ResponseEntity.ok(formattedResponse);
            } else {
                log.error("Token exchange failed with status: {}", response.getStatusCode());
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

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "auth"));
    }
    
    /**
     * Parse JWT payload without validation (since token is from trusted Keycloak)
     */
    private Map<String, Object> parseJwtPayload(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) {
                return Map.of();
            }
            
            // Decode base64 payload
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            
            // Parse JSON
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(payload, Map.class);
        } catch (Exception e) {
            log.error("Failed to parse JWT payload", e);
            return Map.of();
        }
    }
}
