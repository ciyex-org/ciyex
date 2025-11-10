package com.qiaben.ciyex.controller.portal;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.qiaben.ciyex.dto.AppointmentDTO;
import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.entity.Provider;
import com.qiaben.ciyex.repository.ProviderRepository;
import com.qiaben.ciyex.service.AppointmentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping({"/api/portal/providers", "/api/fhir/portal/providers"})
@RequiredArgsConstructor
@CrossOrigin(
    origins = { "http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3001", "http://127.0.0.1:3001" },
    allowedHeaders = "*",
    methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS },
    allowCredentials = "true"
)
public class PortalProviderController {

    private final ProviderRepository providerRepository;
    private final AppointmentService appointmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProviderDto>>> getAllProviders(HttpServletRequest request) {
        try {
            log.info("Fetching all providers for portal");

            // Set tenant context from JWT token (no-op here; interceptor populates RequestContext)
            setRequestContextOrg(request);

            List<Provider> providers = providerRepository.findAll();
            if (providers == null || providers.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.<List<ProviderDto>>builder()
                    .success(true)
                    .message("No providers available")
                    .data(java.util.Collections.emptyList())
                    .build());
            }

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
    public ResponseEntity<ApiResponse<ProviderDto>> getProviderById(@PathVariable("id") Long id, HttpServletRequest request) {
        try {
            setRequestContextOrg(request);
            return providerRepository.findById(id)
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
    public ResponseEntity<ApiResponse<List<String>>> getProviderAvailability(@PathVariable("id") Long id, HttpServletRequest request) {
        try {
            log.info("Fetching general availability for provider id: {}", id);
            List<AppointmentDTO> slots = appointmentService.getFirstAvailableSlotsForProvider(id, 6);
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
        @PathVariable("id") Long id, 
        @RequestParam String date,
        @RequestParam(defaultValue = "3") int limit,
        HttpServletRequest request) {
        try {
            log.info("Fetching availability for provider id: {} on date: {} with limit: {}", id, date, limit);
            java.time.LocalDate localDate;
            try {
                localDate = java.time.LocalDate.parse(date);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid date format. Expected: yyyy-MM-dd, got: " + date);
            }
            List<AppointmentDTO> slots =  appointmentService.getAvailableSlotsForDate(id, localDate, limit);
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
        dto.setPhone(provider.getPhoneNumber());
        dto.setEmail(provider.getEmail());
        dto.setTitle(provider.getProviderType());
        ProviderDto.Identification identification = new ProviderDto.Identification();
        identification.setFirstName(provider.getFirstName());
        identification.setLastName(provider.getLastName());
        dto.setIdentification(identification);

        ProviderDto.ProfessionalDetails professionalDetails = new ProviderDto.ProfessionalDetails();
        professionalDetails.setSpecialty(provider.getSpecialty());
        professionalDetails.setLocation(provider.getAddress());
        professionalDetails.setWorkingHours("9:00 AM - 6:00 PM");
        professionalDetails.setExperience("5+ years");
        professionalDetails.setLanguages(java.util.Arrays.asList("English", "Spanish"));
        dto.setProfessionalDetails(professionalDetails);

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

    public static class ProviderDto {
        private Long id;
        private String fullName;
        private String title;
        private String phone;
        private String email;
        private Identification identification;
        private ProfessionalDetails professionalDetails;

        public static class Identification {
            private String firstName;
            private String lastName;
            public String getFirstName() { return firstName; }
            public void setFirstName(String firstName) { this.firstName = firstName; }
            public String getLastName() { return lastName; }
            public void setLastName(String lastName) { this.lastName = lastName; }
        }

        public static class ProfessionalDetails {
            private String specialty;
            private String location;
            private String workingHours;
            private String experience;
            private java.util.List<String> languages;
            public String getSpecialty() { return specialty; }
            public void setSpecialty(String specialty) { this.specialty = specialty; }
            public String getLocation() { return location; }
            public void setLocation(String location) { this.location = location; }
            public String getWorkingHours() { return workingHours; }
            public void setWorkingHours(String workingHours) { this.workingHours = workingHours; }
            public String getExperience() { return experience; }
            public void setExperience(String experience) { this.experience = experience; }
            public java.util.List<String> getLanguages() { return languages; }
            public void setLanguages(java.util.List<String> languages) { this.languages = languages; }
        }

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

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.valueOf(value.toString());
    }

    private void setRequestContextOrg(HttpServletRequest request) {
        // RequestContext is now set by TenantContextInterceptor
    }
}