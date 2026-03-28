package org.ciyex.ehr.marketplace.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.marketplace.dto.SmartLaunchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * Generates SMART on FHIR launch contexts for third-party app launches.
 *
 * A launch context is a signed, short-lived token that encodes:
 * - The org (tenant), user, patient, and encounter context
 * - The target app slug
 * - A timestamp for expiry validation
 *
 * The launched app presents this token to exchange for an access token
 * via the standard SMART on FHIR authorization flow.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmartLaunchService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:https://stage.aran.me/realms/master}")
    private String issuerUri;

    /**
     * Create a launch context with a signed token and the full launch URL.
     */
    public Map<String, Object> createLaunchContext(
            String orgId, String appSlug, String userId, SmartLaunchRequest request) {

        String launchToken = generateLaunchToken(orgId, appSlug, userId, request);

        // Build the launch URL with SMART parameters
        String launchUrl = request.getSmartLaunchUrl();
        if (launchUrl != null && !launchUrl.isBlank()) {
            String separator = launchUrl.contains("?") ? "&" : "?";
            launchUrl = launchUrl + separator + "launch=" + launchToken + "&iss=" + getFhirBaseUrl(orgId);
        }

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("launchToken", launchToken);
        context.put("launchUrl", launchUrl);
        context.put("fhirBaseUrl", getFhirBaseUrl(orgId));
        context.put("patientId", request.getPatientId());
        context.put("encounterId", request.getEncounterId());
        context.put("expiresAt", Instant.now().plusSeconds(300).toString());
        return context;
    }

    /**
     * Return SMART on FHIR configuration metadata per the HL7 SMART App Launch spec.
     * Exposed publicly at /api/public/.well-known/smart-configuration
     * and authenticated at /api/smart-launch/metadata.
     * See: https://hl7.org/fhir/smart-app-launch/conformance.html
     */
    public Map<String, Object> getSmartConfiguration() {
        Map<String, Object> config = new LinkedHashMap<>();

        // Keycloak OAuth2 endpoints
        String keycloakBase = issuerUri != null ? issuerUri : "https://stage.aran.me/realms/master";
        config.put("issuer", keycloakBase);
        config.put("authorization_endpoint", keycloakBase + "/protocol/openid-connect/auth");
        config.put("token_endpoint", keycloakBase + "/protocol/openid-connect/token");
        config.put("token_endpoint_auth_methods_supported", List.of(
                "client_secret_basic", "client_secret_post", "private_key_jwt"));
        config.put("registration_endpoint", keycloakBase + "/clients-registrations/openid-connect");
        config.put("introspection_endpoint", keycloakBase + "/protocol/openid-connect/token/introspect");
        config.put("revocation_endpoint", keycloakBase + "/protocol/openid-connect/revoke");

        // SMART-specific fields
        config.put("scopes_supported", List.of(
                "openid", "fhirUser", "launch", "launch/patient", "launch/encounter",
                "patient/*.read", "patient/*.write", "patient/*.*",
                "user/*.read", "user/*.write", "user/*.*",
                "offline_access"));
        config.put("response_types_supported", List.of("code"));
        config.put("grant_types_supported", List.of("authorization_code", "client_credentials"));
        config.put("code_challenge_methods_supported", List.of("S256"));

        config.put("capabilities", List.of(
                "launch-ehr",
                "launch-standalone",
                "client-public",
                "client-confidential-symmetric",
                "client-confidential-asymmetric",
                "context-ehr-patient",
                "context-ehr-encounter",
                "context-standalone-patient",
                "permission-patient",
                "permission-user",
                "permission-offline",
                "sso-openid-connect"
        ));
        return config;
    }

    /**
     * Generate a signed launch token encoding the launch context.
     * Format: base64(orgId:appSlug:userId:patientId:encounterId:timestamp).hmac
     */
    private String generateLaunchToken(String orgId, String appSlug, String userId, SmartLaunchRequest request) {
        String payload = String.join(":",
                orgId,
                appSlug,
                userId,
                request.getPatientId() != null ? request.getPatientId() : "",
                request.getEncounterId() != null ? request.getEncounterId() : "",
                String.valueOf(Instant.now().getEpochSecond())
        );

        String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String signature = computeHmac(encoded);

        return encoded + "." + signature;
    }

    private String getFhirBaseUrl(String orgId) {
        // In a full implementation, this would resolve the org's FHIR base URL
        return "/api/fhir";
    }

    private String computeHmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC for launch token", e);
        }
    }
}
