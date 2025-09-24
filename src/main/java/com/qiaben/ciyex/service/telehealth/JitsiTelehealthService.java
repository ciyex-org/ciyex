package com.qiaben.ciyex.service.telehealth;

import com.qiaben.ciyex.dto.integration.IntegrationKey;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.dto.integration.TelehealthConfig;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@TelehealthVendor("jitsi")
@Component
@Slf4j
public class JitsiTelehealthService implements TelehealthService {

    private final OrgIntegrationConfigProvider configProvider;

    @Autowired
    public JitsiTelehealthService(OrgIntegrationConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    @Override
    public String startVideoCall(Long providerId, Long patientId, String roomName) {
        Long orgId = RequestContext.get().getOrgId();
        TelehealthConfig config = configProvider.get(orgId, IntegrationKey.TELEHEALTH);
        ensureJitsiConfigured(config);

        // For Jitsi, we don't need to create a room via API - rooms are created on-demand
        // We just return a unique room identifier that will be used for the meeting URL
        String uniqueRoomName = generateUniqueRoomName(roomName, orgId, providerId, patientId);
        
        log.info("Started Jitsi video call for orgId={}, roomName={}, providerId={}, patientId={}", 
                orgId, uniqueRoomName, providerId, patientId);
        
        return uniqueRoomName;
    }

    @Override
    public String getCallStatus(String callId) {
        // For Jitsi, we assume the room is always "active" since rooms are created on-demand
        // In a more sophisticated implementation, you could track room status via webhooks
        return "active";
    }

    @Override
    public String createJoinToken(String roomName, String identity, Integer ttlSecs) {
        Long orgId = RequestContext.get().getOrgId();
        TelehealthConfig config = configProvider.get(orgId, IntegrationKey.TELEHEALTH);
        ensureJitsiConfigured(config);

        TelehealthConfig.Jitsi jitsiConfig = config.getJitsi();
        
        // Use provided TTL or default from config or fallback to 1 hour
        int tokenTtl = ttlSecs != null ? ttlSecs : 
                      (jitsiConfig.getDefaultTokenTtl() != null ? jitsiConfig.getDefaultTokenTtl() : 3600);

        return generateJitsiJWT(jitsiConfig, roomName, identity, tokenTtl);
    }

    /**
     * Generate a JWT token for Jitsi authentication
     */
    private String generateJitsiJWT(TelehealthConfig.Jitsi config, String roomName, String identity, int ttlSecs) {
        if (isBlank(config.getAppId()) || isBlank(config.getAppSecret())) {
            log.warn("Jitsi app ID or secret not configured, returning room name as token");
            return roomName; // Fallback for development/testing
        }

        Instant now = Instant.now();
        Instant expiration = now.plus(ttlSecs, ChronoUnit.SECONDS);

        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", config.getAppId());
        claims.put("sub", config.getServerUrl());
        claims.put("aud", "jitsi");
        claims.put("room", roomName);
        claims.put("exp", expiration.getEpochSecond());
        claims.put("iat", now.getEpochSecond());
        
        // User context for Jitsi
        Map<String, Object> context = new HashMap<>();
        Map<String, Object> user = new HashMap<>();
        user.put("name", identity);
        user.put("id", identity);
        context.put("user", user);
        claims.put("context", context);

        try {
            SecretKey key = Keys.hmacShaKeyFor(config.getAppSecret().getBytes());
            
            String token = Jwts.builder()
                    .claims(claims)
                    .signWith(key)
                    .compact();

            log.info("Generated Jitsi JWT token for room={}, identity={}, ttl={}s", roomName, identity, ttlSecs);
            return token;
        } catch (Exception e) {
            log.error("Failed to generate Jitsi JWT token", e);
            throw new RuntimeException("Failed to generate Jitsi access token", e);
        }
    }

    /**
     * Generate a unique room name that includes organization context
     */
    private String generateUniqueRoomName(String baseRoomName, Long orgId, Long providerId, Long patientId) {
        // Sanitize room name - Jitsi rooms should only contain alphanumeric characters and hyphens
        String sanitizedBase = baseRoomName.replaceAll("[^a-zA-Z0-9-]", "-");
        return String.format("org%d-%s-p%d-pt%d", orgId, sanitizedBase, providerId, patientId);
    }

    /**
     * Get the full Jitsi meeting URL for a room
     */
    public String getMeetingUrl(String roomName) {
        Long orgId = RequestContext.get().getOrgId();
        TelehealthConfig config = configProvider.get(orgId, IntegrationKey.TELEHEALTH);
        ensureJitsiConfigured(config);

        String serverUrl = config.getJitsi().getServerUrl();
        // Remove trailing slash if present
        if (serverUrl.endsWith("/")) {
            serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
        }
        
        return serverUrl + "/" + roomName;
    }

    /**
     * Create a join token and return both the token and meeting URL
     */
    public JoinTokenWithMeetingUrl createJoinTokenWithUrl(String roomName, String identity, Integer ttlSecs) {
        String token = createJoinToken(roomName, identity, ttlSecs);
        String meetingUrl = getMeetingUrl(roomName);
        
        // For Jitsi, if we have a JWT token, we can append it as a query parameter
        if (!token.equals(roomName)) { // If token is not just the room name (i.e., JWT was generated)
            meetingUrl += "?jwt=" + token;
        }
        
        return new JoinTokenWithMeetingUrl(roomName, identity, token, meetingUrl);
    }

    public record JoinTokenWithMeetingUrl(String roomName, String identity, String token, String meetingUrl) {}

    private static void ensureJitsiConfigured(TelehealthConfig cfg) {
        if (cfg == null || cfg.getJitsi() == null) {
            throw new IllegalStateException("Telehealth Jitsi configuration is missing.");
        }
        
        if (isBlank(cfg.getJitsi().getServerUrl())) {
            throw new IllegalStateException("Jitsi server URL is required but not configured.");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}