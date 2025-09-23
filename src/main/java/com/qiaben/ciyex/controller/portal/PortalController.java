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
    @PreAuthorize("hasRole('PATIENT')")
    @GetMapping("/appointments")
    public ResponseEntity<ApiResponse<List<AppointmentDTO>>> getMyAppointments(HttpServletRequest request) {
        try {
            Long patientId = extractPatientIdFromToken(request);
            setRequestContextOrg(request);

            List<AppointmentDTO> appointments = appointmentService.getByPatientId(patientId);

            return ResponseEntity.ok(ApiResponse.<List<AppointmentDTO>>builder()
                    .success(true)
                    .message("Appointments retrieved successfully")
                    .data(appointments)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.<List<AppointmentDTO>>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    // 🔹 POST: Patient requests a new appointment
    @PreAuthorize("hasRole('PATIENT')")
    @PostMapping("/appointments")
    public ResponseEntity<ApiResponse<AppointmentDTO>> createAppointment(
            HttpServletRequest request,
            @RequestBody AppointmentDTO dto) {
        try {
            Long patientId = extractPatientIdFromToken(request);
            setRequestContextOrg(request);

            dto.setPatientId(patientId);
            if (dto.getStatus() == null) dto.setStatus("PENDING");

            AppointmentDTO created = appointmentService.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<AppointmentDTO>builder()
                    .success(true)
                    .message("Appointment request submitted successfully")
                    .data(created)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.<AppointmentDTO>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    // 🔹 GET: Available providers
    @PreAuthorize("hasRole('PATIENT')")
    @GetMapping("/providers")
    public ResponseEntity<ApiResponse<List<ProviderDto>>> getProviders(HttpServletRequest request) {
        try {
            setRequestContextOrg(request);
            List<ProviderDto> providers = providerService.getAllProviders().getData();

            return ResponseEntity.ok(ApiResponse.<List<ProviderDto>>builder()
                    .success(true)
                    .message("Providers retrieved successfully")
                    .data(providers)
                    .build());
        } catch (Exception e) {
            log.error("❌ Failed to retrieve providers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.<List<ProviderDto>>builder()
                    .success(false)
                    .message("Failed to retrieve providers")
                    .build());
        }
    }

    // 🔹 GET: Provider availability for a given date
    @PreAuthorize("hasRole('PATIENT')")
    @GetMapping("/providers/{id}/availability/date")
    public ResponseEntity<ApiResponse<List<AppointmentDTO>>> getProviderAvailabilityForDate(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestParam String date,   // Expect "yyyy-MM-dd"
            @RequestParam(defaultValue = "3") int limit) {

        try {
            setRequestContextOrg(request);

            LocalDate localDate;
            try {
                localDate = LocalDate.parse(date);
            } catch (DateTimeParseException ex) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.<List<AppointmentDTO>>builder()
                                .success(false)
                                .message("Invalid date format. Expected yyyy-MM-dd")
                                .build()
                );
            }

            List<AppointmentDTO> slots = appointmentService.getAvailableSlotsForDate(id, localDate, limit);

            return ResponseEntity.ok(ApiResponse.<List<AppointmentDTO>>builder()
                    .success(true)
                    .message("Available slots retrieved successfully")
                    .data(slots)
                    .build());

        } catch (Exception e) {
            log.error("❌ Failed to retrieve provider availability for date", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<AppointmentDTO>>builder()
                            .success(false)
                            .message("Failed to retrieve provider availability")
                            .build());
        }
    }

    // 🔹 GET: Available locations
    @PreAuthorize("hasRole('PATIENT')")
    @GetMapping("/locations")
    public ResponseEntity<ApiResponse<List<LocationDto>>> getLocations(HttpServletRequest request) {
        try {
            setRequestContextOrg(request);
            List<LocationDto> locations = locationService.getAllLocations().getData();

            return ResponseEntity.ok(ApiResponse.<List<LocationDto>>builder()
                    .success(true)
                    .message("Locations retrieved successfully")
                    .data(locations)
                    .build());
        } catch (Exception e) {
            log.error("❌ Failed to retrieve locations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.<List<LocationDto>>builder()
                    .success(false)
                    .message("Failed to retrieve locations")
                    .build());
        }
    }
}
