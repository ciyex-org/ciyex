package org.ciyex.ehr.client;

import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP client for the centralized ciyex-comm service.
 * Delegates email/SMS sending to ciyex-comm instead of direct SMTP/Twilio.
 */
@Component
@Slf4j
public class CommServiceClient {

    private final RestClient restClient;

    public CommServiceClient(
            @Value("${services.comm-url:http://localhost:8085}") String commUrl,
            RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl(commUrl).build();
    }

    /**
     * Send an email via ciyex-comm.
     */
    public void sendEmail(String to, String subject, String body) {
        RequestContext ctx = RequestContext.get();
        String orgAlias = ctx.getOrgName();
        String authToken = ctx.getAuthToken();

        Map<String, Object> request = new HashMap<>();
        request.put("channelType", "EMAIL");
        request.put("recipientAddress", to);
        request.put("subject", subject);
        request.put("bodyText", body);
        request.put("category", "GENERAL");
        request.put("priority", "NORMAL");

        sendMessage(orgAlias, authToken, request);
        log.info("Email sent via comm-service to {}", to);
    }

    /**
     * Send an SMS via ciyex-comm.
     */
    public void sendSms(String to, String body) {
        RequestContext ctx = RequestContext.get();
        String orgAlias = ctx.getOrgName();
        String authToken = ctx.getAuthToken();

        Map<String, Object> request = new HashMap<>();
        request.put("channelType", "SMS");
        request.put("recipientAddress", to);
        request.put("bodyText", body);
        request.put("category", "GENERAL");
        request.put("priority", "NORMAL");

        sendMessage(orgAlias, authToken, request);
        log.info("SMS sent via comm-service to {}", to);
    }

    @SuppressWarnings("unchecked")
    private void sendMessage(String orgAlias, String authToken, Map<String, Object> request) {
        try {
            restClient.post()
                    .uri("/api/comm/messages")
                    .header("X-Org-Alias", orgAlias != null ? orgAlias : "")
                    .header("Authorization", authToken != null ? authToken : "")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new RuntimeException("Failed to send message via comm-service: " + e.getMessage(), e);
        }
    }
}
