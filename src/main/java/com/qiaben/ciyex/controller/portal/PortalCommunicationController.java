package com.qiaben.ciyex.controller.portal;

import com.qiaben.ciyex.dto.CommunicationDto;
import com.qiaben.ciyex.entity.CommunicationStatus;
import com.qiaben.ciyex.service.CommunicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/portal/communications", "/api/fhir/portal/communications"})
@Slf4j
public class PortalCommunicationController {

    private final CommunicationService communicationService;

    public PortalCommunicationController(CommunicationService communicationService) {
        this.communicationService = communicationService;
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('PATIENT') or hasRole('PATIENT')")
    public ResponseEntity<?> getMyCommunications(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Not authenticated"
                ));
            }

            // ✅ Get Keycloak user UUID from authentication (logged for tracing)
            log.debug("Portal sendMessage invoked by user {}", authentication.getName());

            // ✅ Fetch communications safely
        List<CommunicationDto> communications =
            communicationService.getCommunicationsForPortalUser(authentication.getName());

            // ✅ Prevent NullPointerException
            if (communications == null) {
                communications = Collections.emptyList();
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Patient communications retrieved",
                    "data", communications
            ));

        } catch (Exception e) {
            log.error("Error getting communications for portal user: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Unable to load communications data",
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/send")
    @PreAuthorize("hasAuthority('PATIENT') or hasRole('PATIENT')")
    public ResponseEntity<?> sendMessage(@RequestBody CommunicationDto messageDto, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Not authenticated"
                ));
            }

            // ✅ Get Keycloak user UUID from authentication (used inline below)

            // TODO: Replace static patient ID lookup with proper logic
            Long patientId = 1L;

            // ✅ Validate fields
            if (messageDto.getProviderId() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Provider ID is required"
                ));
            }

            if (messageDto.getPayload() == null || messageDto.getPayload().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Message content is required"
                ));
            }

            // ✅ Build patient-to-provider message
            messageDto.setPatientId(patientId);
            messageDto.setSender("Patient/" + patientId);
            messageDto.setRecipients(List.of("Provider/" + messageDto.getProviderId()));
            messageDto.setMessageType("patient_to_provider");
            messageDto.setStatus(CommunicationStatus.SENT);

            CommunicationDto createdMessage = communicationService.create(messageDto);

            // ✅ Prevent null issue again
            if (createdMessage == null) {
                return ResponseEntity.internalServerError().body(Map.of(
                        "success", false,
                        "message", "Message creation failed"
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Message sent successfully",
                    "data", createdMessage
            ));

        } catch (Exception e) {
            log.error("Error sending message from portal user: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Unable to send message",
                    "error", e.getMessage()
            ));
        }
    }
}
