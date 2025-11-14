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
        // For Jitsi, we don't need to create a room via API - rooms are created on-demand
        String tenantName = getCurrentTenantName();
        String uniqueRoomName = generateUniqueRoomName(roomName, tenantName, providerId, patientId);

        log.info("Started Jitsi video call for roomName={}, providerId={}, patientId={}",
                uniqueRoomName, providerId, patientId);

        return uniqueRoomName;
    }

    @Override
    public String getCallStatus(String callId) {
        return "active"; // Jitsi rooms are ephemeral/on-demand
    }

    @Override
    public String createJoinToken(String roomName, String identity, Integer ttlSecs) {
        TelehealthConfig config = null;
        try {
            config = configProvider.get(IntegrationKey.TELEHEALTH);
        } catch (Exception e) {
            log.warn("Failed to get telehealth config, using defaults: {}", e.getMessage());
        }

        TelehealthConfig.Jitsi jitsiConfig = config != null ? config.getJitsi() : null;

        int tokenTtl = ttlSecs != null ? ttlSecs :
                (jitsiConfig != null && jitsiConfig.getDefaultTokenTtl() != null ? jitsiConfig.getDefaultTokenTtl() : 3600);

        return generateJitsiJWT(jitsiConfig, roomName, identity, tokenTtl);
    }

    private String generateJitsiJWT(TelehealthConfig.Jitsi config, String roomName, String identity, int ttlSecs) {
        String appId = (config != null && !isBlank(config.getAppId())) ? config.getAppId() : "ciyex";
        String appSecret = (config != null && !isBlank(config.getAppSecret())) ? config.getAppSecret() : "ciyex-default-secret-key-for-development-only-change-in-production";
        String serverUrl = (config != null && !isBlank(config.getServerUrl())) ? config.getServerUrl() : "https://meet-stg.ciyex.com";

        if (appSecret.length() < 32) {
            appSecret = String.format("%-32s", appSecret).replace(' ', '0');
        }

        Instant now = Instant.now();
        Instant expiration = now.plus(ttlSecs, ChronoUnit.SECONDS);

        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", appId);
        claims.put("sub", serverUrl);
        claims.put("aud", "jitsi");
        claims.put("room", roomName);
        claims.put("exp", expiration.getEpochSecond());
        claims.put("iat", now.getEpochSecond());

        Map<String, Object> context = new HashMap<>();
        Map<String, Object> user = new HashMap<>();
        user.put("name", identity);
        user.put("id", identity);
        context.put("user", user);
        claims.put("context", context);

        try {
            SecretKey key = Keys.hmacShaKeyFor(appSecret.getBytes());
            return Jwts.builder().claims(claims).signWith(key).compact();
        } catch (Exception e) {
            log.error("Failed to generate Jitsi JWT token", e);
            throw new RuntimeException("Failed to generate Jitsi access token", e);
        }
    }

    private String generateUniqueRoomName(String baseRoomName, String tenantName, Long providerId, Long patientId) {
        String sanitizedBase = baseRoomName.replaceAll("[^a-zA-Z0-9-]", "-");
        String sanitizedTenant = tenantName != null ? tenantName.replaceAll("[^a-zA-Z0-9-]", "-") : "default";
        return String.format("%s-%s-p%d-pt%d", sanitizedTenant, sanitizedBase, providerId, patientId);
    }

    public String getMeetingUrl(String roomName) {
        TelehealthConfig config = null;
        try {
            config = configProvider.get(IntegrationKey.TELEHEALTH);
        } catch (Exception e) {
            log.warn("Failed to get telehealth config for meeting URL, using defaults: {}", e.getMessage());
        }

        String serverUrl;
        if (config != null && config.getJitsi() != null && !isBlank(config.getJitsi().getServerUrl())) {
            serverUrl = config.getJitsi().getServerUrl();
        } else {
            serverUrl = "https://meet-stg.ciyex.com";
            log.warn("Jitsi server URL not configured, using default: {}", serverUrl);
        }

        if (serverUrl.endsWith("/")) {
            serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
        }
        return serverUrl + "/" + roomName;
    }

    public JoinTokenWithMeetingUrl createJoinTokenWithUrl(String roomName, String identity, Integer ttlSecs) {
        String token = createJoinToken(roomName, identity, ttlSecs);
        String meetingUrl = getMeetingUrl(roomName);
        if (!token.equals(roomName)) {
            meetingUrl += "?jwt=" + token;
        }
        return new JoinTokenWithMeetingUrl(roomName, identity, token, meetingUrl);
    }

    public record JoinTokenWithMeetingUrl(String roomName, String identity, String token, String meetingUrl) {}

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private String getCurrentTenantName() {
        try {
            RequestContext rc = RequestContext.get();
            return rc != null ? rc.getTenantName() : null;
        } catch (Exception e) {
            log.debug("RequestContext not available, using default tenant for Jitsi configuration");
            return "practice_1";
        }
    }
}
