package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.service.telehealth.CloudflareTelehealthService;
import com.qiaben.ciyex.service.telehealth.JitsiTelehealthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    private final ApplicationContext applicationContext;
    private final com.qiaben.ciyex.util.OrgIntegrationConfigProvider configProvider;

    public TelehealthController(ApplicationContext applicationContext, com.qiaben.ciyex.util.OrgIntegrationConfigProvider configProvider) {
        this.applicationContext = applicationContext;
        this.configProvider = configProvider;
    }

    // ----- DTO Records -----
    public record CreateMeetingRequest(
            @NotNull(message = "Provider ID is required.")
            Long providerId,
            @NotNull(message = "Patient ID is required.")
            Long patientId,
            @NotBlank(message = "Room name is required.")
            String roomName
    ) {}
    
    public record CloudflareMeetingRequest(
            @NotNull(message = "Provider ID is required.")
            Long providerId,
            @NotNull(message = "Patient ID is required.")
            Long patientId,
            @NotBlank(message = "Start time is required (HH:mm format).")
            String startTime,
            @NotBlank(message = "End time is required (HH:mm format).")
            String endTime,
            @NotBlank(message = "Date is required (yyyy-MM-dd format).")
            String date
    ) {}

    public record JoinMeetingRequest(
            @NotBlank(message = "Room name is required.")
            String roomName,
            @NotBlank(message = "Identity is required (e.g., Patient-John or Provider-Dr-Smith).")
            String identity,
            Integer ttlSeconds
    ) {}

    public record JitsiResponse(
            String token, String roomName, String identity, String meetingUrl, long expiresIn
    ) {}

    public record CloudflareResponse(
            String sessionId, String roomName, String identity, String meetingUrl
    ) {}

    // -------------------------------------------------------------------------
    // ✅ JITSI MODULE
    // -------------------------------------------------------------------------
    @PostMapping("/jitsi/create")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createJitsiMeeting(@RequestBody CreateMeetingRequest req, HttpServletRequest request) {
        if (!validateCreateRequest(req)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Provider ID, Patient ID, and Room Name are required."));
        }

        try {
            setTenantContextFromJwt(request);
            JitsiTelehealthService jitsiService = applicationContext.getBean(JitsiTelehealthService.class);
            String roomSid = jitsiService.startVideoCall(req.providerId(), req.patientId(), req.roomName());

            return ResponseEntity.ok(ApiResponse.ok("Jitsi meeting created successfully",
                    Map.of("roomSid", roomSid, "provider", "jitsi")));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to create Jitsi meeting: " + e.getMessage()));
        }
    }

    @PostMapping("/jitsi/join")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> joinJitsiMeeting(@RequestBody JoinMeetingRequest req, HttpServletRequest request) {
        if (!validateJoinRequest(req)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Room Name and Identity are required."));
        }

        try {
            int ttl = (req.ttlSeconds() != null && req.ttlSeconds() > 0) ? req.ttlSeconds() : 3600;
            JitsiTelehealthService jitsiService = applicationContext.getBean(JitsiTelehealthService.class);
            JitsiTelehealthService.JoinTokenWithMeetingUrl result =
                    jitsiService.createJoinTokenWithUrl(req.roomName(), req.identity(), ttl);

            return ResponseEntity.ok(ApiResponse.ok("Jitsi meeting joined successfully",
                    new JitsiResponse(result.token(), result.roomName(), result.identity(), result.meetingUrl(), ttl)));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to join Jitsi meeting: " + e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // ✅ CLOUDFLARE MODULE
    // -------------------------------------------------------------------------
    @PostMapping("/cloudflare/create")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createCloudflareMeeting(@RequestBody CloudflareMeetingRequest req, HttpServletRequest request) {
        if (!validateCloudflareRequest(req)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Provider ID, Patient ID, Start Time, End Time, and Date are required."));
        }

        try {
            setTenantContextFromJwt(request);
            CloudflareTelehealthService cloudflareService = applicationContext.getBean(CloudflareTelehealthService.class);
            
            CloudflareTelehealthService.CloudflareMeetingRequest serviceRequest = 
                new CloudflareTelehealthService.CloudflareMeetingRequest(
                    req.providerId(), req.patientId(), req.startTime(), req.endTime(), req.date()
                );
            
            CloudflareTelehealthService.CloudflareMeetingResponse result = 
                cloudflareService.createMeetingWithDetails(serviceRequest);

            return ResponseEntity.ok(ApiResponse.ok("Cloudflare meeting created successfully", result));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to create Cloudflare meeting: " + e.getMessage()));
        }
    }

    @PostMapping("/cloudflare/join/{roomId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> joinCloudflareByRoomId(@PathVariable String roomId, HttpServletRequest request) {
        if (roomId == null || roomId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Room ID is required."));
        }

        try {
            setTenantContextFromJwt(request);
            CloudflareTelehealthService cloudflareService = applicationContext.getBean(CloudflareTelehealthService.class);
            CloudflareTelehealthService.CloudflareJoinResponse result = cloudflareService.joinMeetingByRoomId(roomId);

            return ResponseEntity.ok(ApiResponse.ok("Successfully joined Cloudflare meeting", result));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to join Cloudflare meeting: " + e.getMessage()));
        }
    }

    @PostMapping("/cloudflare/join")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> joinCloudflareMeeting(@RequestBody JoinMeetingRequest req, HttpServletRequest request) {
        if (!validateJoinRequest(req)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Room Name and Identity are required."));
        }

        try {
            int ttl = (req.ttlSeconds() != null && req.ttlSeconds() > 0) ? req.ttlSeconds() : 3600;
            CloudflareTelehealthService cloudflareService = applicationContext.getBean(CloudflareTelehealthService.class);
            CloudflareTelehealthService.CloudflareJoinResponse result =
                    cloudflareService.createJoinTokenWithUrl(req.roomName(), req.identity(), ttl);

            return ResponseEntity.ok(ApiResponse.ok("Cloudflare meeting joined successfully",
                    new CloudflareResponse(result.roomId(), result.roomId(), result.identity(), result.meetingUrl())));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to join Cloudflare meeting: " + e.getMessage()));
        }
    }

    @PostMapping("/cloudflare/join/fhir/{fhirId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> joinCloudflareByFhirId(@PathVariable String fhirId, @RequestBody JoinMeetingRequest req, HttpServletRequest request) {
        if (!validateJoinRequest(req)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Room Name and Identity are required."));
        }

        try {
            var cloudflareConfig = configProvider.getCloudflareCredentialsByFhirId(fhirId);
            if (cloudflareConfig == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("Cloudflare credentials not found for FHIR ID: " + fhirId));
            }

            int ttl = (req.ttlSeconds() != null && req.ttlSeconds() > 0) ? req.ttlSeconds() : 
                     (cloudflareConfig.getDefaultTokenTtl() != null ? cloudflareConfig.getDefaultTokenTtl() : 3600);
            
            CloudflareTelehealthService cloudflareService = applicationContext.getBean(CloudflareTelehealthService.class);
            CloudflareTelehealthService.CloudflareJoinResponse result =
                    cloudflareService.createJoinTokenWithUrl(req.roomName(), req.identity(), ttl);

            return ResponseEntity.ok(ApiResponse.ok("Cloudflare meeting joined with FHIR credentials",
                    new CloudflareResponse(result.roomId(), result.roomId(), result.identity(), result.meetingUrl())));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to join Cloudflare meeting with FHIR ID " + fhirId + " : " + e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // 🔹 Helper Methods
    // -------------------------------------------------------------------------
    private boolean validateCreateRequest(CreateMeetingRequest req) {
        return req.providerId() != null && req.patientId() != null && 
               req.roomName() != null && !req.roomName().trim().isEmpty();
    }
    
    private boolean validateCloudflareRequest(CloudflareMeetingRequest req) {
        return req.providerId() != null && req.patientId() != null && 
               req.startTime() != null && !req.startTime().trim().isEmpty() &&
               req.endTime() != null && !req.endTime().trim().isEmpty() &&
               req.date() != null && !req.date().trim().isEmpty();
    }

    private boolean validateJoinRequest(JoinMeetingRequest req) {
        return req.roomName() != null && !req.roomName().trim().isEmpty() &&
               req.identity() != null && !req.identity().trim().isEmpty();
    }

    private void setTenantContextFromJwt(HttpServletRequest request) {
        try {
            com.qiaben.ciyex.dto.integration.RequestContext ctx = new com.qiaben.ciyex.dto.integration.RequestContext();
            com.qiaben.ciyex.dto.integration.RequestContext.set(ctx);
        } catch (Exception ignored) {}
    }
}
