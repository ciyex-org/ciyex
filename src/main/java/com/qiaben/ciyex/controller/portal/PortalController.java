package com.qiaben.ciyex.controller.portal;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.AppointmentDTO;
import com.qiaben.ciyex.dto.LocationDto;
import com.qiaben.ciyex.dto.ProviderDto;
import com.qiaben.ciyex.entity.portal.PortalUser;
import com.qiaben.ciyex.repository.portal.PortalUserRepository;
import com.qiaben.ciyex.service.AppointmentService;
import com.qiaben.ciyex.service.LocationService;
import com.qiaben.ciyex.service.ProviderService;
import org.springframework.security.core.Authentication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
@Slf4j
public class PortalController {

    private final AppointmentService appointmentService;
    private final ProviderService providerService;
    private final LocationService locationService;
    private final PortalUserRepository portalUserRepository;

    // 🔹 Extract patientId from Authentication
    private Long extractPatientIdFromAuth(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated");
        }
        
        try {
            String email = authentication.getName();
            return portalUserRepository.findByEmail(email)
                    .map(PortalUser::getId)
                    .orElseThrow(() -> new IllegalStateException("No user found for email: " + email));
        } catch (Exception e) {
            log.error("❌ Failed to extract user from authentication", e);
            throw new IllegalStateException("Failed to get user information");
        }
    }

    // 🔹 Convert to Long safely
    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.valueOf(value.toString());
    }
//
    // 🔹 GET: Patient’s own appointments






    // Provider availability endpoints moved to PortalProviderController


}
