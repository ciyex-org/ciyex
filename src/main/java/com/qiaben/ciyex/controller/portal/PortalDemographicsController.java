package com.qiaben.ciyex.controller.portal;

import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.portal.PortalDemographicsDto;
import com.qiaben.ciyex.service.portal.PortalDemographicsService;
import com.qiaben.ciyex.repository.portal.PortalUserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portal/patients/me/demographics")
@RequiredArgsConstructor
public class PortalDemographicsController {

    private final PortalDemographicsService demographicsService;
    private final PortalUserRepository portalUserRepository;

    /**
     * ✅ Resolve patientId from the authenticated user's email in JWT
     */
    private Long getCurrentPatientId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails userDetails) {
            String email = userDetails.getUsername();
            return portalUserRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalStateException("No portal user found with email: " + email))
                    .getId();
        }
        throw new IllegalStateException("No authenticated portal user found");
    }

    @GetMapping(produces = "application/json")
    public ApiResponse<PortalDemographicsDto> getMyDemographics() {
        Long patientId = getCurrentPatientId();
        PortalDemographicsDto dto = demographicsService.getMyDemographics(patientId);
        return ApiResponse.success("Fetched demographics successfully", dto);
    }

    @PutMapping(produces = "application/json", consumes = "application/json")
    public ApiResponse<PortalDemographicsDto> updateMyDemographics(
            @RequestBody PortalDemographicsDto demographicsDto) {
        Long patientId = getCurrentPatientId();
        PortalDemographicsDto updated = demographicsService.updateMyDemographics(patientId, demographicsDto);
        return ApiResponse.success("Updated demographics successfully", updated);
    }
}
