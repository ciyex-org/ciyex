package com.qiaben.ciyex.service.telehealth;

import com.qiaben.ciyex.dto.integration.IntegrationKey;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.dto.integration.TelehealthConfig;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@TelehealthVendor("cloudflare")
@Component
@Slf4j
public class CloudflareTelehealthService implements TelehealthService {

    private final OrgIntegrationConfigProvider configProvider;
    private final RestTemplate restTemplate;

    @Autowired
    public CloudflareTelehealthService(OrgIntegrationConfigProvider configProvider) {
        this.configProvider = configProvider;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String startVideoCall(Long providerId, Long patientId, String roomName) {
        String tenantName = getCurrentTenantName();
        String uniqueRoomName = generateUniqueRoomName(roomName, tenantName, providerId, patientId);

        log.info("Started Cloudflare Calls video call for roomName={}, providerId={}, patientId={}",
                uniqueRoomName, providerId, patientId);

        return uniqueRoomName;
    }

    @Override
    public String getCallStatus(String callId) {
        return "active"; // Cloudflare Calls rooms are ephemeral
    }

    @Override
    public String createJoinToken(String roomName, String identity, Integer ttlSecs) {
        TelehealthConfig config = null;
        try {
            config = configProvider.get(IntegrationKey.TELEHEALTH);
        } catch (Exception e) {
            log.warn("Failed to get telehealth config, using defaults: {}", e.getMessage());
        }

        TelehealthConfig.Cloudflare cloudflareConfig = config != null ? config.getCloudflare() : null;

        if (cloudflareConfig == null || isBlank(cloudflareConfig.getAppId()) || isBlank(cloudflareConfig.getAppSecret())) {
            throw new RuntimeException("Cloudflare configuration is missing or incomplete");
        }

        return generateCloudflareToken(cloudflareConfig, roomName, identity, ttlSecs);
    }

    private String generateCloudflareToken(TelehealthConfig.Cloudflare config, String roomName, String identity, Integer ttlSecs) {
        try {
            String apiUrl = "https://rtc.live.cloudflare.com/v1/apps/" + config.getAppId() + "/sessions/new";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiToken());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("sessionDescription", roomName);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("identity", identity);
            metadata.put("room", roomName);
            requestBody.put("metadata", metadata);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.POST, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                return (String) body.get("sessionId");
            }
            
            throw new RuntimeException("Failed to generate Cloudflare token");
            
        } catch (Exception e) {
            log.error("Failed to generate Cloudflare token", e);
            throw new RuntimeException("Failed to generate Cloudflare access token", e);
        }
    }

    private String generateUniqueRoomName(String baseRoomName, String tenantName, Long providerId, Long patientId) {
        String sanitizedBase = baseRoomName.replaceAll("[^a-zA-Z0-9-]", "-");
        String sanitizedTenant = tenantName != null ? tenantName.replaceAll("[^a-zA-Z0-9-]", "-") : "default";
        return String.format("%s-%s-p%d-pt%d", sanitizedTenant, sanitizedBase, providerId, patientId);
    }

    public CloudflareJoinResponse createJoinTokenWithUrl(String roomName, String identity, Integer ttlSecs) {
        String token = createJoinToken(roomName, identity, ttlSecs);
        return new CloudflareJoinResponse(roomName, identity, token);
    }

    public record CloudflareJoinResponse(String roomName, String identity, String sessionId) {}

    private static boolean isBlank(String s) { 
        return s == null || s.trim().isEmpty(); 
    }

    private String getCurrentTenantName() {
        try {
            RequestContext rc = RequestContext.get();
            return rc != null ? rc.getTenantName() : null;
        } catch (Exception e) {
            log.debug("RequestContext not available, using default tenant for Cloudflare configuration");
            return "practice_1";
        }
    }
}
