package com.qiaben.ciyex.controller.portal.controller;

import com.qiaben.ciyex.dto.portal.dto.ApiResponse;
import com.qiaben.ciyex.dto.portal.dto.PortalDemographicsDto;
import com.qiaben.ciyex.service.portal.service.PortalDemographicsService;
import com.qiaben.ciyex.service.portal.service.PortalUserDetails;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portal/patients/me/demographics")
@RequiredArgsConstructor
public class PortalDemographicsController {

    private final PortalDemographicsService demographicsService;

    /**
     * ✅ Resolve patientId from the authenticated PortalUserDetails in JWT
     */
    private Long getCurrentPatientId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof PortalUserDetails userDetails) {
            return userDetails.getId();  // this is the PortalUser.id
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
