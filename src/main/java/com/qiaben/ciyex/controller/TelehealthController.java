 package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.service.telehealth.JitsiTelehealthService;
import com.qiaben.ciyex.service.telehealth.TelehealthGateway;
import com.qiaben.ciyex.service.telehealth.TelehealthResolver;
import com.qiaben.ciyex.service.telehealth.TelehealthService;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;
import com.qiaben.ciyex.util.JwtTokenUtil;
import com.qiaben.ciyex.dto.integration.RequestContext;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/telehealth")
@Validated
@CrossOrigin(
        origins = { "http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3001", "http://127.0.0.1:3001" },
        allowedHeaders = "*",
        methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS },
        allowCredentials = "true"
)
public class TelehealthController {

    private final TelehealthGateway gateway;
    private final TelehealthResolver resolver;
    private final JwtTokenUtil jwtTokenUtil;
    private final ApplicationContext applicationContext;

    public TelehealthController(TelehealthGateway gateway, TelehealthResolver resolver, JwtTokenUtil jwtTokenUtil, ApplicationContext applicationContext) {
        this.gateway = gateway;
        this.resolver = resolver;
        this.jwtTokenUtil = jwtTokenUtil;
        this.applicationContext = applicationContext;
    }

    public record StartCallRequest(
            @NotNull Long providerId,
            @NotNull Long patientId,
            @NotBlank String roomName
    ) {}
    public record StartCallResponse(String roomSid) {}

    public record JoinTokenRequest(
            @NotBlank String roomName,
            @NotBlank String identity,
            Integer ttlSeconds
    ) {}
    public record JoinTokenResponse(
            String token, String roomName, String identity, long expiresIn
    ) {}

    public record JitsiJoinResponse(
            String token, String roomName, String identity, String meetingUrl, long expiresIn
    ) {}

    public record StatusResponse(String status) {}

    // -------- Endpoints --------

    @PostMapping("/rooms")
    @PreAuthorize("hasRole('PATIENT') or hasRole('PROVIDER') or hasRole('ADMIN')")
    public ResponseEntity<StartCallResponse> startCall(@RequestBody StartCallRequest req) {
        String roomSid = gateway.startVideoCall(req.providerId(), req.patientId(), req.roomName());
        return ResponseEntity.ok(new StartCallResponse(roomSid));
    }

    @PostMapping("/token")
    @PreAuthorize("hasRole('PATIENT') or hasRole('PROVIDER') or hasRole('ADMIN')")
    public ResponseEntity<?> joinToken(@RequestBody JoinTokenRequest req, HttpServletRequest request) {
        // Ensure tenant context is set from portal JWT so resolver.resolve() can access org-specific config
        setRequestContextOrg(request);

        int ttl = (req.ttlSeconds() != null && req.ttlSeconds() > 0) ? req.ttlSeconds() : 3600;

        TelehealthService service = resolver.resolve();
        if (service instanceof JitsiTelehealthService jitsiService) {
            JitsiTelehealthService.JoinTokenWithMeetingUrl result = jitsiService.createJoinTokenWithUrl(req.roomName(), req.identity(), ttl);
            return ResponseEntity.ok(new JitsiJoinResponse(result.token(), result.roomName(), result.identity(), result.meetingUrl(), ttl));
        }

        String token = gateway.createJoinToken(req.roomName(), req.identity(), ttl);
        return ResponseEntity.ok(new JoinTokenResponse(token, req.roomName(), req.identity(), ttl));
    }

    @PostMapping("/jitsi/join")
    public ResponseEntity<JitsiJoinResponse> jitsiJoin(@RequestBody JoinTokenRequest req, HttpServletRequest request) {
        // Manual JWT validation for both providers and patients
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(null);
        }

        String token = authHeader.substring(7);

        // Validate token is not expired
        if (!jwtTokenUtil.validateToken(token, jwtTokenUtil.getEmailFromToken(token))) {
            return ResponseEntity.status(401).body(null);
        }

        // Check if user has PROVIDER or PATIENT role
        java.util.List<java.util.Map<String, Object>> orgs = jwtTokenUtil.getOrgsFromToken(token);
        System.out.println("🔍 DEBUG: orgs from token: " + orgs);
        boolean hasValidRole = false;
        if (orgs != null) {
            for (java.util.Map<String, Object> org : orgs) {
                @SuppressWarnings("unchecked")
                java.util.List<String> roles = (java.util.List<String>) org.get("roles");
                System.out.println("🔍 DEBUG: roles for org: " + roles);
                if (roles != null && (roles.contains("ROLE_PATIENT") || roles.contains("ROLE_PROVIDER") || roles.contains("ROLE_ADMIN"))) {
                    hasValidRole = true;
                    break;
                }
            }
        }

        System.out.println("🔍 DEBUG: hasValidRole: " + hasValidRole);

        if (!hasValidRole) {
            return ResponseEntity.status(403).body(null);
        }

        // JitsiTelehealthService works globally without tenant context
        JitsiTelehealthService jitsiService = applicationContext.getBean(JitsiTelehealthService.class);
        int ttl = (req.ttlSeconds() != null && req.ttlSeconds() > 0) ? req.ttlSeconds() : 3600;
        
        try {
            JitsiTelehealthService.JoinTokenWithMeetingUrl result =
                    jitsiService.createJoinTokenWithUrl(req.roomName(), req.identity(), ttl);
            return ResponseEntity.ok(new JitsiJoinResponse(
                    result.token(), result.roomName(), result.identity(), result.meetingUrl(), ttl));
        } catch (Exception e) {
            // If tenant context is needed, try to set it from JWT
            try {
                setRequestContextOrg(request);
                JitsiTelehealthService.JoinTokenWithMeetingUrl result =
                        jitsiService.createJoinTokenWithUrl(req.roomName(), req.identity(), ttl);
                return ResponseEntity.ok(new JitsiJoinResponse(
                        result.token(), result.roomName(), result.identity(), result.meetingUrl(), ttl));
            } catch (Exception tenantException) {
                return ResponseEntity.status(500).body(null);
            }
        }
    }

    // ---- tenant helper ----
    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.valueOf(value.toString());
    }

    private void setRequestContextOrg(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalStateException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        
        // Extract email for logging
        String email = jwtTokenUtil.getEmailFromToken(token);
        System.out.println("✅ Telehealth request from user: " + email);
        
        java.util.List<?> orgIds = jwtTokenUtil.getOrgIdsFromToken(token);
        System.out.println("✅ OrgIds extracted from token: " + orgIds);

        if (orgIds == null || orgIds.isEmpty()) {
            throw new IllegalStateException("No orgId found in patient token");
        }

        Long orgId = toLong(orgIds.get(0));
        System.out.println("✅ Using orgId: " + orgId + " for tenant context in telehealth");
        
        RequestContext ctx = new RequestContext();
        ctx.setOrgId(orgId);
        RequestContext.set(ctx);
    }

    @GetMapping("/rooms/{id}/status")
    @PreAuthorize("hasRole('PATIENT') or hasRole('PROVIDER') or hasRole('ADMIN')")
    public ResponseEntity<StatusResponse> status(@PathVariable(value = "id", required = true) String callId) {
        String status = gateway.getCallStatus(callId);
        return ResponseEntity.ok(new StatusResponse(status));
    }
}
