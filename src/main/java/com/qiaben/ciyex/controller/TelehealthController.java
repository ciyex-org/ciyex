package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.service.telehealth.JitsiTelehealthService;
import com.qiaben.ciyex.service.telehealth.TelehealthGateway;
import com.qiaben.ciyex.service.telehealth.TelehealthResolver;
import com.qiaben.ciyex.service.telehealth.TelehealthService;
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

    private final TelehealthGateway gateway;
    private final TelehealthResolver resolver;
    private final ApplicationContext applicationContext;

    public TelehealthController(TelehealthGateway gateway, TelehealthResolver resolver, ApplicationContext applicationContext) {
        this.gateway = gateway;
        this.resolver = resolver;
        this.applicationContext = applicationContext;
    }

    // ----- DTO Records -----
    public record StartCallRequest(
            @NotNull(message = "Provider ID is required.")
            Long providerId,
            @NotNull(message = "Patient ID is required.")
            Long patientId,
            @NotBlank(message = "Room name is required.")
            String roomName
    ) {}

    public record StartCallResponse(String roomSid) {}

    public record JoinTokenRequest(
            @NotBlank(message = "Room name is required.")
            String roomName,
            @NotBlank(message = "Identity is required (e.g., Patient-John or Provider-Dr-Smith).")
            String identity,
            Integer ttlSeconds
    ) {}

    public record JitsiJoinResponse(
            String token, String roomName, String identity, String meetingUrl, long expiresIn
    ) {}

    public record StatusResponse(String status) {}

    // -------------------------------------------------------------------------
    // ✅ 1️⃣ Create Telehealth Room
    // -------------------------------------------------------------------------
    @PostMapping("/rooms")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> startCall(@RequestBody StartCallRequest req, HttpServletRequest request) {
        List<String> missingFields = new ArrayList<>();

        if (req.providerId() == null) missingFields.add("Provider ID");
        if (req.patientId() == null) missingFields.add("Patient ID");
        if (req.roomName() == null || req.roomName().trim().isEmpty()) missingFields.add("Room Name");

        if (!missingFields.isEmpty()) {
            String message = String.join(", ", missingFields)
                    + (missingFields.size() > 1 ? " are required." : " is required.");
            return ResponseEntity.badRequest().body(ApiResponse.error(message));
        }

        try {
            setTenantContextFromJwt(request);
            TelehealthService service = resolver.resolve();

            String roomSid;
            if (service != null) {
                roomSid = service.startVideoCall(req.providerId(), req.patientId(), req.roomName());
            } else {
                roomSid = req.roomName();
            }

            return ResponseEntity.ok(ApiResponse.ok("Telehealth room created successfully",
                    new StartCallResponse(roomSid)));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to create telehealth room: " + e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // ✅ 2️⃣ Generate Token (Tenant-Aware or Fallback)
    // -------------------------------------------------------------------------
    @PostMapping("/token")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> joinToken(@RequestBody JoinTokenRequest req, HttpServletRequest request) {
        List<String> missingFields = new ArrayList<>();

        if (req.roomName() == null || req.roomName().trim().isEmpty()) missingFields.add("Room Name");
        if (req.identity() == null || req.identity().trim().isEmpty()) missingFields.add("Identity");

        if (!missingFields.isEmpty()) {
            String message = String.join(", ", missingFields)
                    + (missingFields.size() > 1 ? " are required." : " is required.");
            return ResponseEntity.badRequest().body(ApiResponse.error(message));
        }

        try {
            int ttl = (req.ttlSeconds() != null && req.ttlSeconds() > 0) ? req.ttlSeconds() : 3600;

            setTenantContextFromJwt(request);
            TelehealthService service = resolver.resolve();

            if (service instanceof JitsiTelehealthService jitsiService) {
                JitsiTelehealthService.JoinTokenWithMeetingUrl result =
                        jitsiService.createJoinTokenWithUrl(req.roomName(), req.identity(), ttl);

                return ResponseEntity.ok(ApiResponse.ok("Telehealth token generated successfully",
                        new JitsiJoinResponse(result.token(), result.roomName(), result.identity(), result.meetingUrl(), ttl)));
            }

            String token = gateway.createJoinToken(req.roomName(), req.identity(), ttl);
            return ResponseEntity.ok(ApiResponse.ok("Telehealth token generated successfully (fallback mode)",
                    new JitsiJoinResponse(token, req.roomName(), req.identity(), null, ttl)));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to generate telehealth token: " + e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // ✅ 3️⃣ Jitsi Join (Generate Token + Meeting URL)
    // -------------------------------------------------------------------------
    @PostMapping("/jitsi/join")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> jitsiJoin(@RequestBody JoinTokenRequest req, HttpServletRequest request) {
        List<String> missingFields = new ArrayList<>();

        if (req.roomName() == null || req.roomName().trim().isEmpty()) missingFields.add("Room Name");
        if (req.identity() == null || req.identity().trim().isEmpty()) missingFields.add("Identity");

        if (!missingFields.isEmpty()) {
            String message = String.join(", ", missingFields)
                    + (missingFields.size() > 1 ? " are required." : " is required.");
            return ResponseEntity.badRequest().body(ApiResponse.error(message));
        }

        try {
            int ttl = (req.ttlSeconds() != null && req.ttlSeconds() > 0) ? req.ttlSeconds() : 3600;
            JitsiTelehealthService jitsiService = applicationContext.getBean(JitsiTelehealthService.class);
            JitsiTelehealthService.JoinTokenWithMeetingUrl result =
                    jitsiService.createJoinTokenWithUrl(req.roomName(), req.identity(), ttl);

            return ResponseEntity.ok(ApiResponse.ok("Telehealth join token generated successfully",
                    new JitsiJoinResponse(result.token(), result.roomName(), result.identity(), result.meetingUrl(), ttl)));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to join telehealth session: " + e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // ✅ 4️⃣ Check Room Status
    // -------------------------------------------------------------------------
    @GetMapping("/rooms/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> status(@PathVariable(value = "id") String callId) {
        try {
            String status = gateway.getCallStatus(callId);
            return ResponseEntity.ok(ApiResponse.ok("Telehealth room status retrieved successfully",
                    new StatusResponse(status)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.ok("Telehealth room status assumed active (default)",
                    new StatusResponse("active")));
        }
    }

    // -------------------------------------------------------------------------
    // 🔹 Helper Method
    // -------------------------------------------------------------------------
    private void setTenantContextFromJwt(HttpServletRequest request) {
        try {
            org.springframework.security.core.Authentication authentication =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
                java.util.List<String> groups = jwt.getClaimAsStringList("groups");
                if (groups != null && !groups.isEmpty()) {
                    String tenantName = groups.get(0);
                    com.qiaben.ciyex.dto.integration.RequestContext ctx = new com.qiaben.ciyex.dto.integration.RequestContext();
                    ctx.setTenantName(tenantName);
                    com.qiaben.ciyex.dto.integration.RequestContext.set(ctx);
                }
            }
        } catch (Exception ignored) {}
    }
}
