package com.qiaben.ciyex.controller.portal;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.entity.Location;
import com.qiaben.ciyex.repository.LocationRepository;
import com.qiaben.ciyex.service.TenantAwareService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/portal/locations")
@RequiredArgsConstructor
@CrossOrigin(
    origins = { "http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3001", "http://127.0.0.1:3001" },
    allowedHeaders = "*",
    methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS },
    allowCredentials = "true"
)
public class PortalLocationController {

    private final LocationRepository locationRepository;
    private final TenantAwareService tenantAwareService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LocationDto>>> getAllLocations(HttpServletRequest request) {
        try {
            log.info("Fetching all locations for portal");
            
            // Set tenant context from JWT token
            setRequestContextOrg(request);
            Long orgId = RequestContext.get().getOrgId();
            
            // Use TenantAwareService to ensure proper schema switching
            List<Location> locations = tenantAwareService.executeInTenantContext(orgId, 
                () -> locationRepository.findAll());
            List<LocationDto> locationDtos = locations.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.<List<LocationDto>>builder()
                    .success(true)
                    .message("Locations retrieved successfully")
                    .data(locationDtos)
                    .build());

        } catch (Exception e) {
            log.error("Error fetching locations for portal", e);
            return ResponseEntity.ok(ApiResponse.<List<LocationDto>>builder()
                    .success(false)
                    .message("Failed to fetch locations: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LocationDto>> getLocationById(@PathVariable Long id, HttpServletRequest request) {
        try {
            // Set tenant context from JWT token
            setRequestContextOrg(request);
            Long orgId = RequestContext.get().getOrgId();
            
            // Use TenantAwareService to ensure proper schema switching
            return tenantAwareService.executeInTenantContext(orgId, 
                () -> locationRepository.findById(id))
                    .map(location -> ResponseEntity.ok(ApiResponse.<LocationDto>builder()
                            .success(true)
                            .message("Location found")
                            .data(convertToDto(location))
                            .build()))
                    .orElse(ResponseEntity.ok(ApiResponse.<LocationDto>builder()
                            .success(false)
                            .message("Location not found")
                            .build()));

        } catch (Exception e) {
            log.error("Error fetching location by id: {}", id, e);
            return ResponseEntity.ok(ApiResponse.<LocationDto>builder()
                    .success(false)
                    .message("Failed to fetch location: " + e.getMessage())
                    .build());
        }
    }

    private LocationDto convertToDto(Location location) {
        LocationDto dto = new LocationDto();
        dto.setId(location.getId());
        dto.setName(location.getName());
        dto.setAddress(location.getAddress());
        dto.setCity(location.getCity());
        dto.setState(location.getState());
        dto.setZipCode(location.getPostalCode()); // Use postalCode field
        dto.setPhone(""); // Location doesn't have phone/email fields - set empty
        dto.setEmail("");
        return dto;
    }

    // Extract orgId from JWT token and set tenant context
    private void setRequestContextOrg(HttpServletRequest request) {
        // RequestContext is now set by TenantContextInterceptor
        // This method is kept for backward compatibility but does nothing
    }

    // Convert to Long safely
    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.valueOf(value.toString());
    }

    // DTO class for location data
    public static class LocationDto {
        private Long id;
        private String name;
        private String address;
        private String city;
        private String state;
        private String zipCode;
        private String phone;
        private String email;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getState() { return state; }
        public void setState(String state) { this.state = state; }

        public String getZipCode() { return zipCode; }
        public void setZipCode(String zipCode) { this.zipCode = zipCode; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}