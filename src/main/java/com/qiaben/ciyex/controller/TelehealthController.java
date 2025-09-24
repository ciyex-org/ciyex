package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.service.telehealth.JitsiTelehealthService;
import com.qiaben.ciyex.service.telehealth.TelehealthGateway;
import com.qiaben.ciyex.service.telehealth.TelehealthResolver;
import com.qiaben.ciyex.service.telehealth.TelehealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telehealth")
@Validated
public class TelehealthController {

    private final TelehealthGateway gateway;
    private final TelehealthResolver resolver;

    public TelehealthController(TelehealthGateway gateway, TelehealthResolver resolver) {
        this.gateway = gateway;
        this.resolver = resolver;
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
    public ResponseEntity<StartCallResponse> startCall(@RequestBody StartCallRequest req) {
        String roomSid = gateway.startVideoCall(req.providerId(), req.patientId(), req.roomName());
        return ResponseEntity.ok(new StartCallResponse(roomSid));
    }

    @PostMapping("/token")
    public ResponseEntity<JoinTokenResponse> joinToken(@RequestBody JoinTokenRequest req) {
        int ttl = (req.ttlSeconds() != null && req.ttlSeconds() > 0) ? req.ttlSeconds() : 3600;
        String token = gateway.createJoinToken(req.roomName(), req.identity(), ttl);
        return ResponseEntity.ok(new JoinTokenResponse(token, req.roomName(), req.identity(), ttl));
    }

    @PostMapping("/jitsi/join")
    public ResponseEntity<JitsiJoinResponse> jitsiJoin(@RequestBody JoinTokenRequest req) {
        TelehealthService service = resolver.resolve();
        if (service instanceof JitsiTelehealthService jitsiService) {
            int ttl = (req.ttlSeconds() != null && req.ttlSeconds() > 0) ? req.ttlSeconds() : 3600;
            JitsiTelehealthService.JoinTokenWithMeetingUrl result = 
                jitsiService.createJoinTokenWithUrl(req.roomName(), req.identity(), ttl);
            return ResponseEntity.ok(new JitsiJoinResponse(
                result.token(), result.roomName(), result.identity(), result.meetingUrl(), ttl));
        } else {
            // Fallback to regular token if not using Jitsi
            int ttl = (req.ttlSeconds() != null && req.ttlSeconds() > 0) ? req.ttlSeconds() : 3600;
            String token = gateway.createJoinToken(req.roomName(), req.identity(), ttl);
            return ResponseEntity.ok(new JitsiJoinResponse(token, req.roomName(), req.identity(), "", ttl));
        }
    }

    @GetMapping("/rooms/{id}/status")
    public ResponseEntity<StatusResponse> status(@PathVariable("id") String callId) {
        String status = gateway.getCallStatus(callId);
        return ResponseEntity.ok(new StatusResponse(status));
    }
}
