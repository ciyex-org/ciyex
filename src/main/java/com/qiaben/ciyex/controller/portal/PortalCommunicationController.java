package com.qiaben.ciyex.controller.portal;

import com.qiaben.ciyex.dto.CommunicationDto;
import com.qiaben.ciyex.entity.CommunicationStatus;
import com.qiaben.ciyex.entity.Provider;
import com.qiaben.ciyex.entity.portal.PortalUser;
import com.qiaben.ciyex.repository.ProviderRepository;
import com.qiaben.ciyex.repository.portal.PortalUserRepository;
import com.qiaben.ciyex.service.CommunicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping({"/api/portal/communications", "/api/fhir/portal/communications"})
@Slf4j
public class PortalCommunicationController {

    private final CommunicationService communicationService;
    private final PortalUserRepository portalUserRepository;
    private final ProviderRepository providerRepository;

    public PortalCommunicationController(CommunicationService communicationService,
                                       PortalUserRepository portalUserRepository,
                                       ProviderRepository providerRepository) {
        this.communicationService = communicationService;
        this.portalUserRepository = portalUserRepository;
        this.providerRepository = providerRepository;
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('PATIENT') or hasRole('PATIENT') or hasAuthority('PROVIDER') or hasRole('PROVIDER')")
    public ResponseEntity<?> getMyCommunications(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Not authenticated"
                ));
            }

            // Prefer the JWT email claim (or preferred_username) when available; authentication.getName()
            // can return the subject (UUID) which doesn't match portal user email records.
            String tempEmail = null;
            Object principal = authentication.getPrincipal();
            if (principal instanceof Jwt) {
                Jwt jwt = (Jwt) principal;
                tempEmail = jwt.getClaimAsString("email");
                if (tempEmail == null || tempEmail.isBlank()) {
                    tempEmail = jwt.getClaimAsString("preferred_username");
                }
            }
            if (tempEmail == null || tempEmail.isBlank()) {
                tempEmail = authentication.getName();
            }
            final String userEmail = tempEmail;
            boolean isPatient = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("PATIENT") || auth.getAuthority().equals("ROLE_PATIENT"));
            boolean isProvider = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("PROVIDER") || auth.getAuthority().equals("ROLE_PROVIDER"));

            if (!isPatient && !isProvider) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "User must have PATIENT or PROVIDER role"
                ));
            }

            List<CommunicationDto> communications;

            if (isPatient) {
                // Get communications for patient
                communications = communicationService.getCommunicationsForPortalUser(userEmail);
            } else {
                // For providers, return empty list for now (service layer doesn't support provider communications)
                communications = Collections.emptyList();
            }

            // ✅ Prevent NullPointerException
            if (communications == null) {
                communications = Collections.emptyList();
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Communications retrieved successfully",
                    "data", communications
            ));

        } catch (Exception e) {
            log.error("Error getting communications: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Unable to load communications data",
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/send")
    @PreAuthorize("hasAuthority('PATIENT') or hasRole('PATIENT') or hasAuthority('PROVIDER') or hasRole('PROVIDER')")
    public ResponseEntity<?> sendMessage(@RequestBody CommunicationDto messageDto, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Not authenticated"
                ));
            }

            // Prefer the JWT email claim (or preferred_username) when available; authentication.getName()
            // can return the subject (UUID) which doesn't match portal user email records.
            String tempEmail = null;
            Object principal = authentication.getPrincipal();
            if (principal instanceof Jwt) {
                Jwt jwt = (Jwt) principal;
                tempEmail = jwt.getClaimAsString("email");
                if (tempEmail == null || tempEmail.isBlank()) {
                    tempEmail = jwt.getClaimAsString("preferred_username");
                }
            }
            if (tempEmail == null || tempEmail.isBlank()) {
                tempEmail = authentication.getName();
            }
            final String userEmail = tempEmail;
            boolean isPatient = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("PATIENT") || auth.getAuthority().equals("ROLE_PATIENT"));
            boolean isProvider = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("PROVIDER") || auth.getAuthority().equals("ROLE_PROVIDER"));

            if (!isPatient && !isProvider) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "User must have PATIENT or PROVIDER role"
                ));
            }

            // ✅ Validate fields
            if (messageDto.getPayload() == null || messageDto.getPayload().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Message content is required"
                ));
            }

            if (isPatient) {
                // Patient sending message to provider
                if (messageDto.getProviderId() == null) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "Provider ID is required"
                    ));
                }

        // Get portal user and EHR patient ID
        Optional<PortalUser> optPortalUser = portalUserRepository.findByEmail(userEmail);
        if (optPortalUser.isEmpty()) {
            log.warn("Portal user not found for email: {}", userEmail);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Portal user not found for email: " + userEmail
            ));
        }
        PortalUser portalUser = optPortalUser.get();

                if (portalUser.getPortalPatient() == null || portalUser.getPortalPatient().getEhrPatientId() == null) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "Patient profile not linked to EHR system"
                    ));
                }

                Long ehrPatientId = portalUser.getPortalPatient().getEhrPatientId();

                // Set message fields for patient-to-provider
                messageDto.setPatientId(ehrPatientId);
                messageDto.setFromId(portalUser.getId());
                messageDto.setFromName(portalUser.getFirstName() + " " + portalUser.getLastName());
                messageDto.setSender("Patient/" + portalUser.getId());
                messageDto.setRecipients(List.of("Provider/" + messageDto.getProviderId()));
                messageDto.setMessageType("patient_to_provider");

            } else if (isProvider) {
                // Provider sending message to patient
                if (messageDto.getPatientId() == null) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "Patient ID is required"
                    ));
                }

        // Get provider by email
        Optional<Provider> optProvider = providerRepository.findAll().stream()
            .filter(p -> userEmail.equals(p.getEmail()))
            .findFirst();
        if (optProvider.isEmpty()) {
            log.warn("Provider not found for email: {}", userEmail);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Provider not found for email: " + userEmail
            ));
        }
        Provider provider = optProvider.get();
        String providerName = provider.getFirstName() + " " + provider.getLastName();

                // Set message fields for provider-to-patient
                messageDto.setProviderId(provider.getId());
                messageDto.setFromId(provider.getId());
                messageDto.setFromName(providerName);
                messageDto.setSender("Provider/" + provider.getId());
                messageDto.setRecipients(List.of("Patient/" + messageDto.getPatientId()));
                messageDto.setMessageType("provider_to_patient");
            }

            messageDto.setStatus(CommunicationStatus.SENT);

            CommunicationDto createdMessage = communicationService.create(messageDto);

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
            log.error("Error sending message: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Unable to send message",
                    "error", e.getMessage()
            ));
        }
    }
}
