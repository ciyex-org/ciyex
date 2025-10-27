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
import com.qiaben.ciyex.util.JwtTokenUtil;
import com.qiaben.ciyex.dto.integration.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
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
    private final JwtTokenUtil jwtTokenUtil;
    private final PortalUserRepository portalUserRepository;

    // 🔹 Extract patientId from JWT or PortalUser
    private Long extractPatientIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalStateException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        try {
            Long userId = jwtTokenUtil.getUserIdFromToken(token);
            if (userId != null) return userId;

            String email = jwtTokenUtil.getEmailFromToken(token);
            return portalUserRepository.findByEmail(email)
                    .map(PortalUser::getId)
                    .orElseThrow(() -> new IllegalStateException("No user found for email: " + email));
        } catch (Exception e) {
            log.error("❌ Token validation failed", e);
            throw new IllegalStateException("Invalid or expired token");
        }
    }

    // 🔹 Convert to Long safely
    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.valueOf(value.toString());
    }
//
    // 🔹 Ensure orgId is set in RequestContext
    private void setRequestContextOrg(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        List<?> orgIds = jwtTokenUtil.getOrgIdsFromToken(token);

        if (orgIds == null || orgIds.isEmpty()) {
            throw new IllegalStateException("No orgId found in patient token");
        }

        Long orgId = toLong(orgIds.get(0));
        RequestContext ctx = new RequestContext();
        ctx.setOrgId(orgId);
        RequestContext.set(ctx);
    }

    // 🔹 GET: Patient’s own appointments






    // Provider availability endpoints moved to PortalProviderController


}
