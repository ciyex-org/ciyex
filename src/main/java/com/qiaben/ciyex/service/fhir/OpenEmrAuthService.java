package com.qiaben.ciyex.service.fhir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.dto.fhir.OpenEmrTokenRequest;
import com.qiaben.ciyex.dto.fhir.OpenEmrTokenResponse;
import com.qiaben.ciyex.properties.OpenEmrOAuthProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OpenEmrAuthService {

    private static final Logger log = LoggerFactory.getLogger(OpenEmrAuthService.class);

    private final OpenEmrOAuthProperties properties;
    private final ResourceLoader resourceLoader;
    private final RestClient restClient;

    private PrivateKey privateKey;

    // --- In-memory token cache ---
    private volatile String cachedAccessToken = null;
    private volatile long cachedTokenExpiryTimeMillis = 0; // Epoch millis

    @PostConstruct
    public void init() throws Exception {
        String pem = IOUtils.toString(
                resourceLoader.getResource(properties.getPrivateKeyPath()).getInputStream(),
                StandardCharsets.UTF_8);
        this.privateKey = loadPrivateKey(pem);

        if (this.privateKey instanceof RSAPrivateCrtKey rsaKey) {
            log.info("Loaded Private Key Modulus: {}", rsaKey.getModulus().toString(16));
        }
    }

    /**
     * Returns a valid access token, requesting a new one if expired/missing.
     * This method is thread-safe.
     */
    public synchronized String getCachedAccessToken() throws Exception {
        long now = System.currentTimeMillis();
        // Refresh if token is expired or about to expire (buffer 30 sec)
        if (cachedAccessToken == null || now > (cachedTokenExpiryTimeMillis - 30_000)) {
            log.info("Cached token is missing/expired, requesting new token...");
            OpenEmrTokenResponse tokenResponse = getAccessToken();

            this.cachedAccessToken = tokenResponse.getAccessToken();
            // expires_in is in seconds, convert to millis and add to now
            this.cachedTokenExpiryTimeMillis = now + tokenResponse.getExpiresIn() * 1000L;
            log.info("Cached new access token, expires in {} seconds", tokenResponse.getExpiresIn());
        } else {
            log.debug("Returning cached OpenEMR access token");
        }
        return this.cachedAccessToken;
    }

    /**
     * Always requests a new access token from OpenEMR.
     */
    public OpenEmrTokenResponse getAccessToken() throws Exception {
        String jwt = createClientAssertion();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        form.add("client_assertion", jwt);
        form.add("scope", properties.getScope());
        // If your server needs client_id as form parameter, uncomment next line
        // form.add("client_id", properties.getClientId());

        // LOG REQUEST DATA
        log.info("Requesting OpenEMR token with:");
        log.info("Token URL: {}", properties.getTokenUrl());
        log.info("Scope: {}", form.getFirst("scope"));
        log.info("Grant type: {}", form.getFirst("grant_type"));
        log.info("Client Assertion Type: {}", form.getFirst("client_assertion_type"));
        log.debug("Client Assertion JWT: {}", jwt);
        log.info("ClientId (iss/sub): {}", properties.getClientId());
        log.info("Audience (aud): {}", properties.getAudience());
        log.info("KID: {}", properties.getKid());

        String responseBody = restClient.post()
                .uri(properties.getTokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(String.class);

        log.info("OpenEMR token endpoint response: {}", responseBody);

        // Parse JSON
        JsonNode node = new ObjectMapper().readTree(responseBody);

        OpenEmrTokenResponse result = new OpenEmrTokenResponse();
        result.setRawResponse(responseBody);
        result.setAccessToken(node.path("access_token").asText());
        result.setTokenType(node.path("token_type").asText());
        result.setExpiresIn(node.path("expires_in").asLong());
        return result;
    }

    private String createClientAssertion() {
        long now = System.currentTimeMillis() / 1000L; // seconds
        long exp = now + 300; // 5 minutes in seconds

        // Build header manually to include "typ": "JWT"
        Map<String, Object> header = new HashMap<>();
        header.put("alg", "RS384");
        header.put("kid", properties.getKid());
        header.put("typ", "JWT");

        String jwt = Jwts.builder()
                .setHeader(header)
                .setIssuer(properties.getClientId())
                .setSubject(properties.getClientId())
                .setAudience(properties.getAudience())
                .setExpiration(new Date(exp * 1000L))
                .setId(UUID.randomUUID().toString())
                .signWith(privateKey, SignatureAlgorithm.RS384)
                .compact();

        log.info("Generated JWT: {}", jwt);
        return jwt;
    }

    private static PrivateKey loadPrivateKey(String keyPem) throws Exception {
        String privateKeyPEM = keyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyPEM);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }
}
