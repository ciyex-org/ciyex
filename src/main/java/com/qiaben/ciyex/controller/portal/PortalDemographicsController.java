package com.qiaben.ciyex.controller.portal;

import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.portal.PortalDemographicsDto;
import com.qiaben.ciyex.service.portal.PortalDemographicsService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/portal/patients/me/demographics")
@RequiredArgsConstructor
public class PortalDemographicsController {

    private final PortalDemographicsService demographicsService;

    /**
     * ✅ Resolve patient UUID from the authenticated user's email in JWT
     */
    private UUID getCurrentPatientUuid() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            String sub = jwt.getClaimAsString("sub");
            if (sub == null || sub.isEmpty()) {
                throw new IllegalStateException("JWT token does not contain sub claim");
            }
            try {
                return UUID.fromString(sub);
            } catch (IllegalArgumentException e) {
                // If sub is not a UUID, use it as-is by creating a deterministic UUID
                return UUID.nameUUIDFromBytes(sub.getBytes());
            }
        }
        throw new IllegalStateException("No authenticated portal user found");
    }

    @GetMapping(produces = "application/json")
    public ApiResponse<PortalDemographicsDto> getMyDemographics() {
        UUID patientUuid = getCurrentPatientUuid();
        PortalDemographicsDto dto = demographicsService.getMyDemographics(patientUuid);
        return ApiResponse.success("Fetched demographics successfully", dto);
    }

    @PutMapping(produces = "application/json", consumes = "application/json")
    public ApiResponse<PortalDemographicsDto> updateMyDemographics(
            @RequestBody PortalDemographicsDto demographicsDto) {
        UUID patientUuid = getCurrentPatientUuid();
        PortalDemographicsDto updated = demographicsService.updateMyDemographics(patientUuid, demographicsDto);
        return ApiResponse.success("Updated demographics successfully", updated);
    }
}
