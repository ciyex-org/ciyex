package com.qiaben.ciyex.storage.fhir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.qiaben.ciyex.dto.integration.FhirConfig;
import com.qiaben.ciyex.dto.integration.IntegrationKey;
import com.qiaben.ciyex.dto.FhirTokenResponse;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class FhirAuthService {
    private static final Logger log = LoggerFactory.getLogger(FhirAuthService.class);

    private final RestClient restClient;
    private final OrgIntegrationConfigProvider integrationConfigProvider;

    // --- Caffeine per-tenant token cache ---
    private final Cache<String, TokenCache> tokenCacheMap = Caffeine.newBuilder()
            .expireAfterWrite(3, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    private static class TokenCache {
        String accessToken;
        long expiryMillis;
    }

    public String getCachedAccessToken()  {
        // Use a constant key for single-tenant mode
        String cacheKey = "single-tenant";
        FhirConfig config = integrationConfigProvider.getForCurrentTenant(IntegrationKey.FHIR);

        TokenCache cache = tokenCacheMap.get(cacheKey, k -> new TokenCache());
        synchronized (cache) {
            long now = System.currentTimeMillis();
            // Refresh if token is missing/expired/about to expire (30s buffer)
            if (cache.accessToken == null || now > (cache.expiryMillis - 30_000)) {
                log.info("FHIR cached token is missing/expired, requesting new token...");
                FhirTokenResponse tokenResponse = null;
                try {
                    tokenResponse = getAccessToken(config);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                cache.accessToken = tokenResponse.getAccessToken();
                cache.expiryMillis = now + tokenResponse.getExpiresIn() * 1000L;
                log.info("Cached new FHIR access token, expires in {} seconds", tokenResponse.getExpiresIn());
            } else {
                log.debug("Returning cached FHIR access token");
            }
            return cache.accessToken;
        }
    }

    /**
     * Always requests a new access token from FHIR Azure API, for given config.
     */
    public FhirTokenResponse getAccessToken(FhirConfig config) throws Exception {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", config.getClientId());
        if (config.getClientSecret() != null) {
            form.add("client_secret", config.getClientSecret());
        }
        form.add("scope", config.getScope());

        log.info("Requesting FHIR token with:");
        log.info("Token URL: {}", config.getTokenUrl());
        log.info("Scope: {}", form.getFirst("scope"));
        log.info("Grant type: {}", form.getFirst("grant_type"));
        log.info("ClientId: {}", config.getClientId());

        String responseBody = restClient.post()
                .uri(config.getTokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(String.class);

        log.info("FHIR token endpoint response: {}", responseBody);

        // Parse JSON
        JsonNode node = new ObjectMapper().readTree(responseBody);

        FhirTokenResponse result = new FhirTokenResponse();
        result.setRawResponse(responseBody);
        result.setAccessToken(node.path("access_token").asText());
        result.setTokenType(node.path("token_type").asText());
        result.setExpiresIn(node.path("expires_in").asLong());
        return result;
    }
}
