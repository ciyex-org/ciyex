package com.qiaben.ciyex.controller.portal;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.CommunicationDto;
import com.qiaben.ciyex.entity.CommunicationStatus;
import com.qiaben.ciyex.service.CommunicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/portal/communications")
@Slf4j
public class PortalCommunicationController {

    private final CommunicationService communicationService;

    public PortalCommunicationController(CommunicationService communicationService) {
        this.communicationService = communicationService;
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('PATIENT') or hasRole('PATIENT')")
    public ResponseEntity<?> getMyCommunications(HttpServletRequest request) {
        try {
            // Extract JWT token from Authorization header
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Authorization token missing"
                ));
            }

            String token = authHeader.substring(7);

            // Use the new tenant-aware method to get communications
            List<CommunicationDto> communications = communicationService.getCommunicationsForPortalUser(token);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Patient communications retrieved",
                "data", communications
            ));

        } catch (Exception e) {
            log.error("Error getting communications for portal user: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Unable to load communications data",
                "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/send")
    @PreAuthorize("hasAuthority('PATIENT') or hasRole('PATIENT')")
    public ResponseEntity<?> sendMessage(@RequestBody CommunicationDto messageDto, HttpServletRequest request) {
        try {
            // Extract JWT token from Authorization header
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Authorization token missing"
                ));
            }

            String token = authHeader.substring(7);

            // Get patient information from token
            Map<String, Object> userInfo = communicationService.extractUserInfoFromToken(token);
            String userEmail = (String) userInfo.get("email");
            @SuppressWarnings("unchecked")
            List<Long> orgIds = (List<Long>) userInfo.get("orgIds");

            if (orgIds == null || orgIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "No organization found in token"
                ));
            }

            Long orgId = ((Number) orgIds.get(0)).longValue();

            // Get the EHR patient ID for this portal user
            Long patientId = communicationService.getEhrPatientIdFromPortalUserEmail(userEmail, orgId);
            if (patientId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Patient profile not found"
                ));
            }

            // Validate required fields
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

            // Set up the message for patient-to-provider communication
            messageDto.setPatientId(patientId);
            messageDto.setOrgId(orgId);
            messageDto.setSender("Patient/" + patientId);
            messageDto.setRecipients(List.of("Provider/" + messageDto.getProviderId()));
            messageDto.setMessageType("patient_to_provider");
            messageDto.setStatus(CommunicationStatus.SENT);

            // Create the communication
            CommunicationDto createdMessage = communicationService.create(messageDto);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Message sent successfully",
                "data", createdMessage
            ));

        } catch (Exception e) {
            log.error("Error sending message from portal user: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Unable to send message",
                "error", e.getMessage()
            ));
        }
    }
}