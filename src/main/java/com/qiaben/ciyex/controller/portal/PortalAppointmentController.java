package com.qiaben.ciyex.controller.portal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.qiaben.ciyex.dto.AppointmentDTO;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.portal.AppointmentDto;
import com.qiaben.ciyex.dto.portal.CreateAppointmentRequest;
import com.qiaben.ciyex.dto.SlotDto;
import com.qiaben.ciyex.repository.SlotRepository;

import com.qiaben.ciyex.entity.Location;
import com.qiaben.ciyex.entity.Provider;

import com.qiaben.ciyex.repository.LocationRepository;
import com.qiaben.ciyex.repository.ProviderRepository;
import com.qiaben.ciyex.repository.portal.PortalUserRepository;
import com.qiaben.ciyex.service.AppointmentService;
import com.qiaben.ciyex.service.telehealth.JitsiTelehealthService;
import com.qiaben.ciyex.service.telehealth.TelehealthResolver;
import com.qiaben.ciyex.service.telehealth.TelehealthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/portal/appointments")
@RequiredArgsConstructor
@CrossOrigin(
    origins = { "http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3001", "http://127.0.0.1:3001" },
    allowedHeaders = "*",
    methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS },
    allowCredentials = "true"
)
public class PortalAppointmentController {

    private final PortalUserRepository portalUserRepository;
    private final AppointmentService appointmentService;
    private final ProviderRepository providerRepository;
    private final LocationRepository locationRepository;
    private final SlotRepository slotRepository;
    private final TelehealthResolver telehealthResolver;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AppointmentDto>>> getPatientAppointments(HttpServletRequest request) {
        try {
            log.info("Fetching appointments for portal user");

            setRequestContextOrg(request);
            String tenantName = RequestContext.get().getTenantName();

            // RequestContext already set by interceptor, no need for executeInTenantContext
            List<AppointmentDTO> appointments = appointmentService.getAll(org.springframework.data.domain.PageRequest.of(0, 10)).getContent();
            List<AppointmentDto> appointmentDtos = appointments.stream().map(this::convertToDtoInContext).collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.<List<AppointmentDto>>builder()
                    .success(true)
                    .message("Appointments retrieved successfully")
                    .data(appointmentDtos)
                    .build());

        } catch (Exception e) {
            log.error("Error fetching appointments for portal user", e);
            return ResponseEntity.ok(ApiResponse.<List<AppointmentDto>>builder()
                    .success(false)
                    .message("Failed to fetch appointments: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/available-slots")
    public ResponseEntity<ApiResponse<List<SlotDto>>> getAvailableSlots(
            @RequestParam(value = "provider_id", required = false) Long providerId,
            @RequestParam(value = "location_id", required = false) Long locationId,
            @RequestParam(value = "date", required = false) String date, // Expected format: mm/dd/yy
            HttpServletRequest request) {
        try {
            log.info("Fetching available slots for provider: {}, location: {}, date: {}", providerId, locationId, date);

            setRequestContextOrg(request);
            String tenantName = RequestContext.get().getTenantName();

            // RequestContext already set by interceptor
            List<SlotDto> availableSlots = generateAvailableSlots(tenantName, providerId, locationId, date);

            log.info("Retrieved {} available slots", availableSlots.size());
            return ResponseEntity.ok(ApiResponse.<List<SlotDto>>builder()
                    .success(true)
                    .message("Available slots retrieved")
                    .data(availableSlots)
                    .build());

        } catch (Exception e) {
            log.error("Error fetching available slots", e);
            return ResponseEntity.ok(ApiResponse.<List<SlotDto>>builder()
                    .success(false)
                    .message("Failed to fetch available slots: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentDto>> requestAppointment(@RequestBody CreateAppointmentRequest request, HttpServletRequest httpRequest) {
        try {
            log.info("Processing appointment request");

            setRequestContextOrg(httpRequest);
            String tenantName = RequestContext.get().getTenantName();

            // RequestContext already set by interceptor
            AppointmentDTO appointmentDTO = new AppointmentDTO();
            appointmentDTO.setVisitType(request.getVisitType());
            appointmentDTO.setProviderId(request.getProviderId());
            appointmentDTO.setLocationId(request.getLocationId());
            
            // Parse date from mm/dd/yy format
            LocalDate appointmentDate = parseDateFromMMDDYY(request.getDate());
            appointmentDTO.setAppointmentStartDate(appointmentDate);
            appointmentDTO.setAppointmentEndDate(appointmentDate);
            appointmentDTO.setAppointmentStartTime(java.time.LocalTime.parse(request.getTime()));
            appointmentDTO.setReason(request.getReason());
            appointmentDTO.setPriority(request.getPriority() != null ? request.getPriority() : "Routine");
            appointmentDTO.setStatus("PENDING");
            // orgId deprecated - tenantName used via RequestContext
            appointmentDTO.setPatientId(1L);

            // Generate Jitsi meeting URL for virtual appointments
            if (isVirtualAppointment(request.getVisitType())) {
                try {
                    TelehealthService telehealthService = telehealthResolver.resolve();
                    if (telehealthService instanceof JitsiTelehealthService jitsiService) {
                        String roomName = "apt" + System.currentTimeMillis(); // Generate unique room name
                        JitsiTelehealthService.JoinTokenWithMeetingUrl result =
                            jitsiService.createJoinTokenWithUrl(roomName, "patient-" + appointmentDTO.getPatientId(), 3600);
                        // appointmentDTO.setMeetingUrl(result.meetingUrl()); // Meeting URLs generated dynamically via JOIN API
                        log.info("Generated Jitsi meeting URL for virtual appointment: {}", result.meetingUrl());
                    }
                } catch (Exception e) {
                    log.warn("Failed to generate Jitsi meeting URL for appointment: {}", e.getMessage());
                    // Continue without meeting URL - appointment can still be created
                }
            }

            AppointmentDTO createdAppointment = appointmentService.create(appointmentDTO);

            AppointmentDto responseDto = convertToDtoInContext(createdAppointment);

            return ResponseEntity.ok(ApiResponse.<AppointmentDto>builder()
                    .success(true)
                    .message("Appointment request submitted successfully. You will be notified once it's approved.")
                    .data(responseDto)
                    .build());

        } catch (Exception e) {
            log.error("Error processing appointment request", e);
            return ResponseEntity.ok(ApiResponse.<AppointmentDto>builder()
                    .success(false)
                    .message("Failed to submit appointment request: " + e.getMessage())
                    .build());
        }
    }

    private void setRequestContextOrg(HttpServletRequest request) {
        // RequestContext is now set by TenantContextInterceptor
        // This method is kept for backward compatibility but does nothing
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.valueOf(value.toString());
    }

    private AppointmentDto convertToDtoInContext(AppointmentDTO appointment) {
        AppointmentDto dto = new AppointmentDto();
        dto.setId(appointment.getId());
        dto.setAppointmentStartDate(appointment.getAppointmentStartDate());
        dto.setAppointmentEndDate(appointment.getAppointmentEndDate());
        dto.setAppointmentStartTime(appointment.getAppointmentStartTime());
        dto.setAppointmentEndTime(appointment.getAppointmentEndTime());
        dto.setFormattedDate(appointment.getFormattedDate());
        dto.setFormattedTime(appointment.getFormattedTime());
        dto.setStatus(appointment.getStatus());
        dto.setVisitType(appointment.getVisitType());
        dto.setPriority(appointment.getPriority());
        dto.setReason(appointment.getReason());
        dto.setProviderId(appointment.getProviderId());
        dto.setLocationId(appointment.getLocationId());
        dto.setPatientId(appointment.getPatientId());

        if (appointment.getProviderId() != null) {
            try {
                Provider provider = providerRepository.findById(appointment.getProviderId()).orElse(null);
                if (provider != null) {
                    dto.setProviderName("Dr. " + provider.getFirstName() + " " + provider.getLastName());
                } else {
                    dto.setProviderName("Provider #" + appointment.getProviderId());
                }
            } catch (Exception e) {
                log.warn("Error fetching provider with ID {}: {}", appointment.getProviderId(), e.getMessage());
                dto.setProviderName("Provider #" + appointment.getProviderId());
            }
        }

        if (appointment.getLocationId() != null) {
            try {
                Location location = locationRepository.findById(appointment.getLocationId()).orElse(null);
                if (location != null) {
                    dto.setLocationName(location.getName());
                } else {
                    dto.setLocationName("Location #" + appointment.getLocationId());
                }
            } catch (Exception e) {
                log.warn("Error fetching location with ID {}: {}", appointment.getLocationId(), e.getMessage());
                dto.setLocationName("Location #" + appointment.getLocationId());
            }
        }

        return dto;
    }

    private boolean isVirtualAppointment(String visitType) {
        if (visitType == null) return false;
        String lower = visitType.toLowerCase();
        return lower.contains("virtual") || lower.contains("telehealth") || lower.contains("video") || lower.contains("online");
    }

    private List<SlotDto> generateAvailableSlots(String tenantName, Long providerId, Long locationId, String dateStr) {
        List<SlotDto> availableSlots = new ArrayList<>();

        try {
            // Parse date from mm/dd/yy format
            LocalDate date;
            if (dateStr != null && !dateStr.trim().isEmpty()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yy");
                date = LocalDate.parse(dateStr, formatter);
            } else {
                date = LocalDate.now().plusDays(1); // Default to tomorrow if no date provided
            }

            // Get providers to generate slots for
            List<Provider> providers = new ArrayList<>();
            if (providerId != null) {
                Provider provider = providerRepository.findById(providerId).orElse(null);
                if (provider != null) {
                    providers.add(provider);
                }
            } else {
                // Get all providers if no specific provider requested
                providers = providerRepository.findAll();
            }

            // Generate slots for each provider (max 3 per provider)
            for (Provider provider : providers) {
                List<SlotDto> providerSlots = generateSlotsForProvider(tenantName, provider.getId(), locationId, date);
                availableSlots.addAll(providerSlots);

                // Limit to 3 slots per provider
                if (availableSlots.size() >= 3) {
                    break;
                }
            }

            // If we have more than 3 total slots, limit to 3
            if (availableSlots.size() > 3) {
                availableSlots = availableSlots.subList(0, 3);
            }

        } catch (Exception e) {
            log.warn("Error generating available slots: {}", e.getMessage());
        }

        return availableSlots;
    }

    private List<SlotDto> generateSlotsForProvider(String tenantName, Long providerId, Long locationId, LocalDate date) {
        List<SlotDto> slots = new ArrayList<>();

        // Generate 3 time slots for the day (9 AM, 10 AM, 2 PM)
        LocalTime[] slotTimes = {
            LocalTime.of(9, 0),   // 9:00 AM
            LocalTime.of(10, 0),  // 10:00 AM
            LocalTime.of(14, 0)   // 2:00 PM
        };

        for (int i = 0; i < slotTimes.length; i++) {
            LocalDateTime startDateTime = LocalDateTime.of(date, slotTimes[i]);
            LocalDateTime endDateTime = startDateTime.plusHours(1); // 1 hour slots

            SlotDto slot = new SlotDto();
            slot.setId((long) (providerId * 1000 + i)); // Generate a simple ID
            slot.setTenantName(tenantName);
            slot.setProviderId(providerId);
            slot.setStart(startDateTime.toString());
            slot.setEnd(endDateTime.toString());
            slot.setStatus("free");

            SlotDto.Audit audit = new SlotDto.Audit();
            audit.setCreatedDate(LocalDateTime.now().toString());
            audit.setLastModifiedDate(LocalDateTime.now().toString());
            slot.setAudit(audit);

            slots.add(slot);
        }

        return slots;
    }

    private LocalDate parseDateFromMMDDYY(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return LocalDate.now();
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yy");
            return LocalDate.parse(dateStr, formatter);
        } catch (Exception e) {
            log.warn("Failed to parse date '{}' with mm/dd/yy format, using current date", dateStr);
            return LocalDate.now();
        }
    }
}