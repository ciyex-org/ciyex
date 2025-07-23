package com.qiaben.ciyex.service.fhir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.OpenEmrConfig;
import com.qiaben.ciyex.dto.core.integration.RequestContext;
import com.qiaben.ciyex.dto.openemr.OpenEmrTokenResponse;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OpenEmrAuthService {

    private static final Logger log = LoggerFactory.getLogger(OpenEmrAuthService.class);

    private final ResourceLoader resourceLoader;
    private final RestClient restClient;
    private final OrgIntegrationConfigProvider integrationConfigProvider;

    // --- Caffeine per-org token/key cache ---
    private final Cache<Long, TokenCache> tokenCacheMap = Caffeine.newBuilder()
            .expireAfterWrite(3, TimeUnit.MINUTES) // Cache entries expire 5 min after write
            .maximumSize(1000)
            .build();

    private static class TokenCache {
        String accessToken;
        long expiryMillis;
        PrivateKey privateKey;
        String privateKeyPath;
    }

    /**
     * Returns a valid access token for the current org, requesting a new one if expired/missing.
     */
    public String getCachedAccessToken()  {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        if (orgId == null) throw new IllegalStateException("No orgId in request context");
        OpenEmrConfig config = integrationConfigProvider.getForCurrentOrg(IntegrationKey.OPENEMR);

        TokenCache cache = tokenCacheMap.get(orgId, k -> new TokenCache());
        synchronized (cache) {
            String keyPath = config.getPrivateKeyPath();
            if (keyPath == null || keyPath.isBlank()) {
                keyPath = "classpath:oaprivate.key";
            }

            // Reload key if changed or not loaded yet
            if (cache.privateKey == null || !keyPath.equals(cache.privateKeyPath)) {
                try {
                    cache.privateKey = loadPrivateKeyFromPath(keyPath);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                cache.privateKeyPath = keyPath;
                if (cache.privateKey instanceof RSAPrivateCrtKey rsaKey) {
                    log.info("[Org:{}] Loaded Private Key Modulus: {}", orgId, rsaKey.getModulus().toString(16));
                }
            }

            long now = System.currentTimeMillis();
            // Refresh if token is missing/expired/about to expire (30s buffer)
            if (cache.accessToken == null || now > (cache.expiryMillis - 30_000)) {
                log.info("[Org:{}] Cached token is missing/expired, requesting new token...", orgId);
                OpenEmrTokenResponse tokenResponse = null;
                try {
                    tokenResponse = getAccessToken(config, cache.privateKey);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                cache.accessToken = tokenResponse.getAccessToken();
                cache.expiryMillis = now + tokenResponse.getExpiresIn() * 1000L;
                log.info("[Org:{}] Cached new access token, expires in {} seconds", orgId, tokenResponse.getExpiresIn());
            } else {
                log.debug("[Org:{}] Returning cached OpenEMR access token", orgId);
            }
            return cache.accessToken;
        }
    }

    /**
     * Always requests a new access token from OpenEMR, for given config/key.
     */
    public OpenEmrTokenResponse getAccessToken(OpenEmrConfig config, PrivateKey privateKey) throws Exception {
        String jwt = createClientAssertion(config, privateKey);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        form.add("client_assertion", jwt);
        form.add("scope", config.getScope());

        log.info("Requesting OpenEMR token with:");
        log.info("Token URL: {}", config.getTokenUrl());
        log.info("Scope: {}", form.getFirst("scope"));
        log.info("Grant type: {}", form.getFirst("grant_type"));
        log.info("Client Assertion Type: {}", form.getFirst("client_assertion_type"));
        log.debug("Client Assertion JWT: {}", jwt);
        log.info("ClientId (iss/sub): {}", config.getClientId());
        log.info("Audience (aud): {}", config.getAudience());
        log.info("KID: {}", config.getKid());

        String responseBody = restClient.post()
                .uri(config.getTokenUrl())
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

    private String createClientAssertion(OpenEmrConfig config, PrivateKey privateKey) {
        long now = System.currentTimeMillis() / 1000L; // seconds
        long exp = now + 300; // 5 minutes in seconds

        Map<String, Object> header = new HashMap<>();
        header.put("alg", "RS384");
        header.put("kid", config.getKid());
        header.put("typ", "JWT");

        String jwt = Jwts.builder()
                .setHeader(header)
                .setIssuer(config.getClientId())
                .setSubject(config.getClientId())
                .setAudience(config.getAudience())
                .setExpiration(new Date(exp * 1000L))
                .setId(UUID.randomUUID().toString())
                .signWith(privateKey, SignatureAlgorithm.RS384)
                .compact();

        log.info("Generated JWT: {}", jwt);
        return jwt;
    }

    private PrivateKey loadPrivateKeyFromPath(String privateKeyPath) throws Exception {
        String pem;
        if (privateKeyPath == null || privateKeyPath.isBlank() || privateKeyPath.startsWith("classpath:")) {
            String resource = (privateKeyPath == null || privateKeyPath.isBlank())
                    ? "classpath:oaprivate.key"
                    : privateKeyPath;
            pem = IOUtils.toString(
                    resourceLoader.getResource(resource).getInputStream(),
                    StandardCharsets.UTF_8);
        } else {
            pem = IOUtils.toString(
                    new java.io.FileInputStream(privateKeyPath),
                    StandardCharsets.UTF_8);
        }
        return loadPrivateKey(pem);
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
