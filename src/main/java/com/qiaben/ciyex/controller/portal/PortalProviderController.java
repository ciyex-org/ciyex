package com.qiaben.ciyex.controller.portal;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.qiaben.ciyex.dto.AppointmentDTO;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.entity.Provider;
import com.qiaben.ciyex.repository.ProviderRepository;
import com.qiaben.ciyex.service.AppointmentService;
import com.qiaben.ciyex.service.TenantAwareService;
import com.qiaben.ciyex.util.JwtTokenUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/portal/providers")
@RequiredArgsConstructor
@CrossOrigin(
    origins = { "http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3001", "http://127.0.0.1:3001" },
    allowedHeaders = "*",
    methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS },
    allowCredentials = "true"
)
public class PortalProviderController {

    private final ProviderRepository providerRepository;
    private final TenantAwareService tenantAwareService;
    private final JwtTokenUtil jwtTokenUtil;
    private final AppointmentService appointmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProviderDto>>> getAllProviders(HttpServletRequest request) {
        try {
            log.info("Fetching all providers for portal");
            
            // Set tenant context from JWT token
            setRequestContextOrg(request);
            Long orgId = RequestContext.get().getOrgId();
            
            // Use TenantAwareService to ensure proper schema switching
            List<Provider> providers = tenantAwareService.executeInTenantContext(orgId, 
                () -> providerRepository.findAll());
            List<ProviderDto> providerDtos = providers.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.<List<ProviderDto>>builder()
                    .success(true)
                    .message("Providers retrieved successfully")
                    .data(providerDtos)
                    .build());

        } catch (Exception e) {
            log.error("Error fetching providers for portal", e);
            return ResponseEntity.ok(ApiResponse.<List<ProviderDto>>builder()
                    .success(false)
                    .message("Failed to fetch providers: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProviderDto>> getProviderById(@PathVariable Long id, HttpServletRequest request) {
        try {
            // Set tenant context from JWT token
            setRequestContextOrg(request);
            Long orgId = RequestContext.get().getOrgId();
            
            // Use TenantAwareService to ensure proper schema switching
            return tenantAwareService.executeInTenantContext(orgId, 
                () -> providerRepository.findById(id))
                    .map(provider -> ResponseEntity.ok(ApiResponse.<ProviderDto>builder()
                            .success(true)
                            .message("Provider found")
                            .data(convertToDto(provider))
                            .build()))
                    .orElse(ResponseEntity.ok(ApiResponse.<ProviderDto>builder()
                            .success(false)
                            .message("Provider not found")
                            .build()));

        } catch (Exception e) {
            log.error("Error fetching provider by id: {}", id, e);
            return ResponseEntity.ok(ApiResponse.<ProviderDto>builder()
                    .success(false)
                    .message("Failed to fetch provider: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<ApiResponse<List<String>>> getProviderAvailability(@PathVariable Long id, HttpServletRequest request) {
        try {
            log.info("Fetching general availability for provider id: {}", id);
            
            // Set tenant context from JWT token
            setRequestContextOrg(request);
            Long orgId = RequestContext.get().getOrgId();
            
            // Use TenantAwareService to ensure proper schema switching
            List<AppointmentDTO> slots = tenantAwareService.executeInTenantContext(orgId, 
                () -> appointmentService.getFirstAvailableSlotsForProvider(id, 6));
            
            List<String> timeSlots = slots.stream()
                    .map(slot -> slot.getFormattedTime() != null ? slot.getFormattedTime() : slot.getAppointmentStartTime().toString())
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.<List<String>>builder()
                    .success(true)
                    .message("Provider availability retrieved successfully")
                    .data(timeSlots)
                    .build());

        } catch (Exception e) {
            log.error("Error fetching provider availability for id: {}", id, e);
            return ResponseEntity.ok(ApiResponse.<List<String>>builder()
                    .success(false)
                    .message("Failed to fetch provider availability: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/{id}/availability/date")
    public ResponseEntity<ApiResponse<List<AppointmentDTO>>> getProviderAvailabilityForDate(
        @PathVariable Long id, 
        @RequestParam String date,
        @RequestParam(defaultValue = "3") int limit,
        HttpServletRequest request) {
        try {
            log.info("Fetching availability for provider id: {} on date: {} with limit: {}", id, date, limit);
            
            // Set tenant context from JWT token
            setRequestContextOrg(request);
            Long orgId = RequestContext.get().getOrgId();
            
            // Parse the date string to LocalDate
            java.time.LocalDate localDate;
            try {
                localDate = java.time.LocalDate.parse(date);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid date format. Expected: yyyy-MM-dd, got: " + date);
            }
            
            // Use TenantAwareService to ensure proper schema switching
            List<AppointmentDTO> slots = tenantAwareService.executeInTenantContext(orgId, 
                () -> appointmentService.getAvailableSlotsForDate(id, localDate, limit));
            
            log.info("Found {} available slots for provider {} on {}", slots.size(), id, date);

            return ResponseEntity.ok(ApiResponse.<List<AppointmentDTO>>builder()
                    .success(true)
                    .message("Provider availability for date retrieved successfully")
                    .data(slots)
                    .build());

        } catch (Exception e) {
            log.error("Error fetching provider availability for id: {} on date: {}", id, date, e);
            return ResponseEntity.ok(ApiResponse.<List<AppointmentDTO>>builder()
                    .success(false)
                    .message("Failed to fetch provider availability: " + e.getMessage())
                    .build());
        }
    }

    private ProviderDto convertToDto(Provider provider) {
        ProviderDto dto = new ProviderDto();
        dto.setId(provider.getId());
        dto.setPhone(provider.getPhoneNumber()); // Use phoneNumber field
        dto.setEmail(provider.getEmail());
        dto.setTitle(provider.getProviderType()); // Use providerType as title
        
        // Set identification details
        ProviderDto.Identification identification = new ProviderDto.Identification();
        identification.setFirstName(provider.getFirstName());
        identification.setLastName(provider.getLastName());
        dto.setIdentification(identification);
        
        // Set professional details
        if (provider.getSpecialty() != null && !provider.getSpecialty().isEmpty()) {
            ProviderDto.ProfessionalDetails professionalDetails = new ProviderDto.ProfessionalDetails();
            professionalDetails.setSpecialty(provider.getSpecialty());
            dto.setProfessionalDetails(professionalDetails);
        }
        
        // Generate full name
        String fullName = "";
        if (provider.getProviderType() != null && !provider.getProviderType().isEmpty()) {
            fullName += provider.getProviderType() + " ";
        }
        if (provider.getFirstName() != null) {
            fullName += provider.getFirstName() + " ";
        }
        if (provider.getLastName() != null) {
            fullName += provider.getLastName();
        }
        dto.setFullName(fullName.trim());
        
        return dto;
    }

    // DTO class for provider data - matches frontend expected structure
    public static class ProviderDto {
        private Long id;
        private String fullName;
        private String title;
        private String phone;
        private String email;
        private Identification identification;
        private ProfessionalDetails professionalDetails;

        // Nested class for identification details
        public static class Identification {
            private String firstName;
            private String lastName;

            public String getFirstName() { return firstName; }
            public void setFirstName(String firstName) { this.firstName = firstName; }

            public String getLastName() { return lastName; }
            public void setLastName(String lastName) { this.lastName = lastName; }
        }

        // Nested class for professional details
        public static class ProfessionalDetails {
            private String specialty;

            public String getSpecialty() { return specialty; }
            public void setSpecialty(String specialty) { this.specialty = specialty; }
        }

        // Main DTO Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public Identification getIdentification() { return identification; }
        public void setIdentification(Identification identification) { this.identification = identification; }

        public ProfessionalDetails getProfessionalDetails() { return professionalDetails; }
        public void setProfessionalDetails(ProfessionalDetails professionalDetails) { this.professionalDetails = professionalDetails; }
    }

    // Helper methods for tenant context
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
        List<?> orgIds = jwtTokenUtil.getOrgIdsFromToken(token);

        if (orgIds == null || orgIds.isEmpty()) {
            throw new IllegalStateException("No orgId found in patient token");
        }

        Long orgId = toLong(orgIds.get(0));
        RequestContext ctx = new RequestContext();
        ctx.setOrgId(orgId);
        RequestContext.set(ctx);
        
        log.debug("Set tenant context with orgId: {}", orgId);
    }
}