package org.ciyex.ehr.controller;

import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.client.TelehealthServiceClient;
import org.ciyex.ehr.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Telehealth proxy controller — delegates all operations to the centralized
 * ciyex-telehealth microservice (port 8088).
 * Replaces the previous local Twilio/Jitsi/Cloudflare/Telnyx vendor implementations.
 */
@Slf4j
@PreAuthorize("hasAuthority('SCOPE_user/Communication.write')")
@RestController
@RequestMapping("/api/telehealth")
@Validated
@CrossOrigin(
        origins = {"http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3001", "http://127.0.0.1:3001"},
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
        allowCredentials = "true"
)
public class TelehealthController {

    private final TelehealthServiceClient telehealthClient;
    /** Idempotency cache: appointmentId → sessionId, so both provider and patient get the same session. */
    private final Map<String, String> appointmentSessionCache = new ConcurrentHashMap<>();

    /** Browser-facing URL for WebSocket connections (may differ from internal service URL). */
    @Value("${services.telehealth-public-url:${services.telehealth-url:http://localhost:8088}}")
    private String telehealthPublicUrl;

    public TelehealthController(TelehealthServiceClient telehealthClient) {
        this.telehealthClient = telehealthClient;
    }

    /**
     * Enrich a session response with providerType and joinInfo so frontends
     * know how to connect (WebSocket URL, vendor type, etc.).
     */
    @SuppressWarnings("unchecked")
    private Object enrichSessionResponse(Object result) {
        if (!(result instanceof Map)) return result;
        Map<String, Object> session = new HashMap<>((Map<String, Object>) result);

        // Add vendor type — currently mediasoup; future: resolved from marketplace
        session.putIfAbsent("providerType", "mediasoup");

        // Build joinInfo with WebSocket signaling URL (use public URL for browser access)
        String wsUrl = telehealthPublicUrl.replaceFirst("^http", "ws") + "/ws/telehealth";
        Map<String, Object> joinInfo = new HashMap<>();
        joinInfo.put("wsUrl", wsUrl);
        if (session.get("roomName") != null) {
            joinInfo.put("roomName", session.get("roomName"));
        }
        session.putIfAbsent("joinInfo", joinInfo);

        return session;
    }

    // -------------------------------------------------------------------------
    // Session CRUD
    // -------------------------------------------------------------------------

    @PostMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createSession(@RequestBody Map<String, Object> request) {
        try {
            Object result = enrichSessionResponse(telehealthClient.createSession(request));
            return ResponseEntity.ok(ApiResponse.ok("Session created", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to create session: " + e.getMessage()));
        }
    }

    @PostMapping("/sessions/from-appointment")
    @PreAuthorize("isAuthenticated()")
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> createSessionFromAppointment(@RequestBody Map<String, String> request) {
        try {
            String appointmentId = request.get("appointmentId");
            if (appointmentId == null || appointmentId.isBlank()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("appointmentId is required"));
            }
            // Idempotency: return existing active session for this appointment if one exists
            String cachedSessionId = appointmentSessionCache.get(appointmentId);
            if (cachedSessionId != null) {
                try {
                    Object existing = telehealthClient.getSession(cachedSessionId);
                    if (existing instanceof Map<?, ?> s) {
                        String status = String.valueOf(((Map<String, Object>) s).getOrDefault("status", ""));
                        if (!status.equalsIgnoreCase("ENDED") && !status.equalsIgnoreCase("CANCELLED") && !status.equalsIgnoreCase("COMPLETED")) {
                            log.info("Returning existing session {} for appointment {}", cachedSessionId, appointmentId);
                            return ResponseEntity.ok(ApiResponse.ok("Session retrieved for appointment", enrichSessionResponse(existing)));
                        }
                    }
                } catch (Exception ignored) {
                    // Session no longer valid, create a new one
                }
                appointmentSessionCache.remove(appointmentId);
            }
            Object result = enrichSessionResponse(telehealthClient.createSessionFromAppointment(appointmentId));
            // Cache the new session ID for idempotency
            if (result instanceof Map<?, ?> s) {
                Object sid = ((Map<?, ?>) s).get("id");
                if (sid != null) appointmentSessionCache.put(appointmentId, sid.toString());
            }
            return ResponseEntity.ok(ApiResponse.ok("Session created from appointment", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to create session from appointment: " + e.getMessage()));
        }
    }

    @GetMapping("/sessions/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getSession(@PathVariable String id) {
        try {
            Object result = enrichSessionResponse(telehealthClient.getSession(id));
            return ResponseEntity.ok(ApiResponse.ok("Session retrieved", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to get session: " + e.getMessage()));
        }
    }

    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Object result = telehealthClient.getSessions(page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to list sessions: " + e.getMessage()));
        }
    }

    @GetMapping("/sessions/provider/{providerId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getSessionsByProvider(
            @PathVariable String providerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Object result = telehealthClient.getSessionsByProvider(providerId, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to list provider sessions: " + e.getMessage()));
        }
    }

    @GetMapping("/sessions/patient/{patientId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getSessionsByPatient(
            @PathVariable String patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Object result = telehealthClient.getSessionsByPatient(patientId, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to list patient sessions: " + e.getMessage()));
        }
    }

    @GetMapping("/sessions/upcoming")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUpcomingSessions(@RequestParam(defaultValue = "30") int minutesAhead) {
        try {
            Object result = telehealthClient.getUpcomingSessions(minutesAhead);
            return ResponseEntity.ok(ApiResponse.ok("Upcoming sessions", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to get upcoming sessions: " + e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // Session Lifecycle
    // -------------------------------------------------------------------------

    @PostMapping("/sessions/{id}/start")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> startSession(@PathVariable String id) {
        try {
            Object result = enrichSessionResponse(telehealthClient.startSession(id));
            return ResponseEntity.ok(ApiResponse.ok("Session started", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to start session: " + e.getMessage()));
        }
    }

    @PostMapping("/sessions/{id}/end")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> endSession(@PathVariable String id) {
        try {
            Object result = telehealthClient.endSession(id);
            return ResponseEntity.ok(ApiResponse.ok("Session ended", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to end session: " + e.getMessage()));
        }
    }

    @PostMapping("/sessions/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cancelSession(
            @PathVariable String id,
            @RequestParam(required = false) String reason) {
        try {
            Object result = telehealthClient.cancelSession(id, reason);
            return ResponseEntity.ok(ApiResponse.ok("Session cancelled", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to cancel session: " + e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // Participants
    // -------------------------------------------------------------------------

    @GetMapping("/sessions/{sessionId}/participants")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getParticipants(@PathVariable String sessionId) {
        try {
            Object result = telehealthClient.getParticipants(sessionId);
            return ResponseEntity.ok(ApiResponse.ok("Participants retrieved", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to get participants: " + e.getMessage()));
        }
    }

    @PostMapping("/sessions/{sessionId}/admit/{participantId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> admitParticipant(
            @PathVariable String sessionId, @PathVariable String participantId) {
        try {
            Object result = telehealthClient.admitParticipant(sessionId, participantId);
            return ResponseEntity.ok(ApiResponse.ok("Participant admitted", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to admit participant: " + e.getMessage()));
        }
    }

    @PostMapping("/sessions/{sessionId}/participants/{participantId}/remove")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> removeParticipant(
            @PathVariable String sessionId, @PathVariable String participantId) {
        try {
            Object result = telehealthClient.removeParticipant(sessionId, participantId);
            return ResponseEntity.ok(ApiResponse.ok("Participant removed", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to remove participant: " + e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // WebRTC / mediasoup
    // -------------------------------------------------------------------------

    @GetMapping("/sessions/{id}/router-capabilities")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getRouterCapabilities(@PathVariable String id) {
        try {
            Object result = telehealthClient.getRouterCapabilities(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to get router capabilities: " + e.getMessage()));
        }
    }

    @GetMapping("/sessions/{id}/ice-servers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getIceServers(
            @PathVariable String id,
            @RequestParam(required = false) String peerId) {
        try {
            Object result = telehealthClient.getIceServers(id, peerId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to get ICE servers: " + e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // Recordings
    // -------------------------------------------------------------------------

    @PostMapping("/sessions/{sessionId}/recording/start")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> startRecording(
            @PathVariable String sessionId,
            @RequestBody(required = false) Map<String, String> request) {
        try {
            String mode = request != null ? request.get("mode") : null;
            Object result = telehealthClient.startRecording(sessionId, mode);
            return ResponseEntity.ok(ApiResponse.ok("Recording started", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to start recording: " + e.getMessage()));
        }
    }

    @PostMapping("/sessions/{sessionId}/recording/stop")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> stopRecording(@PathVariable String sessionId) {
        try {
            Object result = telehealthClient.stopRecording(sessionId);
            return ResponseEntity.ok(ApiResponse.ok("Recording stopped", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to stop recording: " + e.getMessage()));
        }
    }

    @GetMapping("/sessions/{sessionId}/recordings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getRecordings(@PathVariable String sessionId) {
        try {
            Object result = telehealthClient.getRecordings(sessionId);
            return ResponseEntity.ok(ApiResponse.ok("Recordings retrieved", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to get recordings: " + e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // Chat
    // -------------------------------------------------------------------------

    @GetMapping("/sessions/{sessionId}/chat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getChatMessages(@PathVariable String sessionId) {
        try {
            Object result = telehealthClient.getChatMessages(sessionId);
            return ResponseEntity.ok(ApiResponse.ok("Chat messages retrieved", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to get chat messages: " + e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // Appointments (FHIR)
    // -------------------------------------------------------------------------

    @GetMapping("/appointments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAppointments(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String providerId,
            @RequestParam(required = false) String status) {
        try {
            Map<String, String> params = new LinkedHashMap<>();
            if (dateFrom != null) params.put("dateFrom", dateFrom);
            if (dateTo != null) params.put("dateTo", dateTo);
            if (patientId != null) params.put("patientId", patientId);
            if (providerId != null) params.put("providerId", providerId);
            if (status != null) params.put("status", status);
            Object result = telehealthClient.getAppointments(params);
            return ResponseEntity.ok(ApiResponse.ok("Appointments retrieved", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to get appointments: " + e.getMessage()));
        }
    }

    @GetMapping("/appointments/upcoming")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUpcomingAppointments(
            @RequestParam String providerId,
            @RequestParam(defaultValue = "60") int minutesAhead) {
        try {
            Object result = telehealthClient.getUpcomingAppointments(providerId, minutesAhead);
            return ResponseEntity.ok(ApiResponse.ok("Upcoming appointments", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to get upcoming appointments: " + e.getMessage()));
        }
    }

    @GetMapping("/appointments/today")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getTodayAppointments(@RequestParam String providerId) {
        try {
            Object result = telehealthClient.getTodayAppointments(providerId);
            return ResponseEntity.ok(ApiResponse.ok("Today's appointments", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to get today's appointments: " + e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // Jitsi / Portal Join
    // -------------------------------------------------------------------------

    /**
     * POST /api/telehealth/jitsi/join
     *
     * Called by the patient portal when joining a telehealth session.
     * Accepts { roomName, identity, ttlSeconds } and delegates to the telehealth
     * microservice to obtain (or create) the session and return a meeting URL.
     */
    @PostMapping("/jitsi/join")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> jitsiJoin(@RequestBody Map<String, Object> request) {
        try {
            String roomName = (String) request.get("roomName");
            if (roomName == null || roomName.isBlank()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("roomName is required"));
            }

            // Derive appointmentId from roomName pattern "apt{id}"
            String appointmentId = roomName.startsWith("apt") ? roomName.substring(3) : roomName;

            // Attempt to create/retrieve a session for this appointment
            Map<String, Object> sessionRequest = new HashMap<>();
            sessionRequest.put("appointmentId", appointmentId);
            sessionRequest.put("roomName", roomName);
            if (request.containsKey("identity")) {
                sessionRequest.put("participantIdentity", request.get("identity"));
            }
            if (request.containsKey("ttlSeconds")) {
                sessionRequest.put("ttlSeconds", request.get("ttlSeconds"));
            }

            Object result = telehealthClient.createSessionFromAppointment(appointmentId);

            // Extract meeting URL from the telehealth service response (now a Map)
            String meetingUrl = null;
            Map<?, ?> resultMap = result instanceof Map ? (Map<?, ?>) result : null;
            if (resultMap != null) {
                if (resultMap.containsKey("meetingUrl")) meetingUrl = String.valueOf(resultMap.get("meetingUrl"));
                else if (resultMap.containsKey("url")) meetingUrl = String.valueOf(resultMap.get("url"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("meetingUrl", meetingUrl);
            response.put("roomName", roomName);
            if (resultMap != null && resultMap.containsKey("id")) {
                response.put("sessionId", String.valueOf(resultMap.get("id")));
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("Jitsi join failed for request {}: {}", request.get("roomName"), e.getMessage());
            return ResponseEntity.status(404).body(
                    ApiResponse.error("Video call room not found or not yet started: " + e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // Clinical Data
    // -------------------------------------------------------------------------

    @GetMapping("/sessions/{sessionId}/clinical-data")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getClinicalData(@PathVariable String sessionId) {
        try {
            Object result = telehealthClient.getClinicalData(sessionId);
            return ResponseEntity.ok(ApiResponse.ok("Clinical data retrieved", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to get clinical data: " + e.getMessage()));
        }
    }

    @GetMapping("/sessions/{sessionId}/encounters")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getEncounters(@PathVariable String sessionId) {
        try {
            Object result = telehealthClient.getEncounters(sessionId);
            return ResponseEntity.ok(ApiResponse.ok("Encounters retrieved", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to get encounters: " + e.getMessage()));
        }
    }
}
