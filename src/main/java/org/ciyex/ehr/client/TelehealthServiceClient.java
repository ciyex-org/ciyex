package org.ciyex.ehr.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * HTTP client for the centralized ciyex-telehealth service.
 * Replaces local Twilio/Telnyx/Jitsi/Cloudflare vendor implementations.
 */
@Component
@Slf4j
public class TelehealthServiceClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String telehealthBaseUrl;

    public TelehealthServiceClient(
            @Value("${services.telehealth-url:http://localhost:8088}") String telehealthUrl,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper) {
        this.telehealthBaseUrl = telehealthUrl;
        this.restClient = restClientBuilder.baseUrl(telehealthUrl).build();
        this.objectMapper = objectMapper;
    }

    public String getTelehealthBaseUrl() {
        return telehealthBaseUrl;
    }

    // ---- Session CRUD ----

    public Object createSession(Map<String, Object> sessionRequest) {
        return post("/api/telehealth/sessions", sessionRequest);
    }

    public Object createSessionFromAppointment(String appointmentId) {
        return post("/api/telehealth/sessions/from-appointment", Map.of("appointmentId", appointmentId));
    }

    public Object getSession(String sessionId) {
        return get("/api/telehealth/sessions/" + sessionId);
    }

    public Object getSessions(int page, int size) {
        return get("/api/telehealth/sessions?page=" + page + "&size=" + size);
    }

    public Object getSessionsByProvider(String providerId, int page, int size) {
        return get("/api/telehealth/sessions/provider/" + providerId + "?page=" + page + "&size=" + size);
    }

    public Object getSessionsByPatient(String patientId, int page, int size) {
        return get("/api/telehealth/sessions/patient/" + patientId + "?page=" + page + "&size=" + size);
    }

    public Object getUpcomingSessions(int minutesAhead) {
        return get("/api/telehealth/sessions/upcoming?minutesAhead=" + minutesAhead);
    }

    // ---- Session Lifecycle ----

    public Object startSession(String sessionId) {
        return post("/api/telehealth/sessions/" + sessionId + "/start", null);
    }

    public Object endSession(String sessionId) {
        return post("/api/telehealth/sessions/" + sessionId + "/end", null);
    }

    public Object cancelSession(String sessionId, String reason) {
        String uri = "/api/telehealth/sessions/" + sessionId + "/cancel";
        if (reason != null && !reason.isBlank()) {
            uri += "?reason=" + reason;
        }
        return post(uri, null);
    }

    // ---- Participants ----

    public Object getParticipants(String sessionId) {
        return get("/api/telehealth/sessions/" + sessionId + "/participants");
    }

    public Object admitParticipant(String sessionId, String participantId) {
        return post("/api/telehealth/sessions/" + sessionId + "/admit/" + participantId, null);
    }

    public Object removeParticipant(String sessionId, String participantId) {
        return post("/api/telehealth/sessions/" + sessionId + "/participants/" + participantId + "/remove", null);
    }

    // ---- WebRTC / mediasoup ----

    public Object getRouterCapabilities(String sessionId) {
        return get("/api/telehealth/sessions/" + sessionId + "/router-capabilities");
    }

    public Object getIceServers(String sessionId, String peerId) {
        String uri = "/api/telehealth/sessions/" + sessionId + "/ice-servers";
        if (peerId != null && !peerId.isBlank()) {
            uri += "?peerId=" + peerId;
        }
        return get(uri);
    }

    // ---- Recordings ----

    public Object startRecording(String sessionId, String mode) {
        Map<String, Object> body = mode != null ? Map.of("mode", mode) : Map.of();
        return post("/api/telehealth/sessions/" + sessionId + "/recording/start", body);
    }

    public Object stopRecording(String sessionId) {
        return post("/api/telehealth/sessions/" + sessionId + "/recording/stop", null);
    }

    public Object getRecordings(String sessionId) {
        return get("/api/telehealth/sessions/" + sessionId + "/recordings");
    }

    // ---- Chat ----

    public Object getChatMessages(String sessionId) {
        return get("/api/telehealth/sessions/" + sessionId + "/chat");
    }

    // ---- Appointments ----

    public Object getAppointments(Map<String, String> params) {
        StringBuilder uri = new StringBuilder("/api/telehealth/appointments?");
        params.forEach((k, v) -> {
            if (v != null && !v.isBlank()) {
                uri.append(k).append("=").append(v).append("&");
            }
        });
        return get(uri.toString());
    }

    public Object getUpcomingAppointments(String providerId, int minutesAhead) {
        return get("/api/telehealth/appointments/upcoming?providerId=" + providerId + "&minutesAhead=" + minutesAhead);
    }

    public Object getTodayAppointments(String providerId) {
        return get("/api/telehealth/appointments/today?providerId=" + providerId);
    }

    // ---- Clinical Data ----

    public Object getClinicalData(String sessionId) {
        return get("/api/telehealth/sessions/" + sessionId + "/clinical-data");
    }

    public Object getEncounters(String sessionId) {
        return get("/api/telehealth/sessions/" + sessionId + "/encounters");
    }

    // ---- Internal HTTP helpers ----

    /**
     * Parse telehealth service response, unwrap ApiResponse wrapper,
     * and convert to plain Map/List so Jackson serializes it correctly
     * (JsonNode inside ApiResponse serializes as bean metadata, not JSON).
     */
    private Object parseResponse(String response) throws JsonProcessingException {
        JsonNode tree = objectMapper.readTree(response);
        // Unwrap ApiResponse: telehealth returns { success, data, message }
        if (tree.has("success") && tree.has("data")) {
            tree = tree.get("data");
        }
        // Convert JsonNode to Map/List for proper Jackson serialization
        return objectMapper.treeToValue(tree, Object.class);
    }

    private Object get(String uri) {
        RequestContext ctx = RequestContext.get();
        try {
            String response = restClient.get()
                    .uri(uri)
                    .header("X-Org-Alias", ctx.getOrgName() != null ? ctx.getOrgName() : "")
                    .header("Authorization", ctx.getAuthToken() != null ? ctx.getAuthToken() : "")
                    .retrieve()
                    .body(String.class);
            return parseResponse(response);
        } catch (Exception e) {
            log.error("Telehealth service GET {} failed: {}", uri, e.getMessage());
            throw new RuntimeException("Telehealth service call failed: " + e.getMessage(), e);
        }
    }

    private Object post(String uri, Object body) {
        RequestContext ctx = RequestContext.get();
        try {
            var request = restClient.post()
                    .uri(uri)
                    .header("X-Org-Alias", ctx.getOrgName() != null ? ctx.getOrgName() : "")
                    .header("Authorization", ctx.getAuthToken() != null ? ctx.getAuthToken() : "")
                    .contentType(MediaType.APPLICATION_JSON);

            if (body != null) {
                request.body(body);
            }

            String response = request.retrieve().body(String.class);
            return parseResponse(response);
        } catch (Exception e) {
            log.error("Telehealth service POST {} failed: {}", uri, e.getMessage());
            throw new RuntimeException("Telehealth service call failed: " + e.getMessage(), e);
        }
    }
}
