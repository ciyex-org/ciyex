package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.OpenEmrTokenRequest;
import com.qiaben.ciyex.dto.OpenEmrTokenResponse;
import com.qiaben.ciyex.properties.OpenEmrOAuthProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OpenEmrAuthService {

    private final OpenEmrOAuthProperties properties;
    private final ResourceLoader resourceLoader;
    private final RestClient restClient;

    private PrivateKey privateKey;

    @PostConstruct
    public void init() throws Exception {
        String pem = IOUtils.toString(
                resourceLoader.getResource(properties.getPrivateKeyPath()).getInputStream(),
                StandardCharsets.UTF_8);
        this.privateKey = loadPrivateKey(pem);
    }

    public OpenEmrTokenResponse getAccessToken(OpenEmrTokenRequest req) throws Exception {
        String jwt = createClientAssertion();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        form.add("client_assertion", jwt);
        form.add("scope", req.getScope() != null ? req.getScope() : properties.getScope());

        String responseBody = restClient.post()
                .uri(properties.getTokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(String.class);

        // Parse JSON
        com.fasterxml.jackson.databind.JsonNode node =
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(responseBody);

        OpenEmrTokenResponse result = new OpenEmrTokenResponse();
        result.setRawResponse(responseBody);
        result.setAccessToken(node.path("access_token").asText());
        result.setTokenType(node.path("token_type").asText());
        result.setExpiresIn(node.path("expires_in").asLong());
        return result;
    }

    private String createClientAssertion() {
        long now = System.currentTimeMillis();
        long exp = now + 300_000; // 5 min

        return Jwts.builder()
                .setIssuer(properties.getClientId())
                .setSubject(properties.getClientId())
                .setAudience(properties.getAudience())
                .setExpiration(new Date(exp))
                .setNotBefore(new Date(now))
                .setIssuedAt(new Date(now))
                .setId(UUID.randomUUID().toString())
                .setHeaderParam("kid", properties.getKid())
                .signWith(privateKey, SignatureAlgorithm.RS384)
                .compact();
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
