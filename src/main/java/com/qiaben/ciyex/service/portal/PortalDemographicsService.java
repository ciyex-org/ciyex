package com.qiaben.ciyex.service.portal;

import com.qiaben.ciyex.dto.portal.PortalDemographicsDto;

import java.util.UUID;

public interface PortalDemographicsService {

    /**
     * Fetch demographics for the given portal user.
     * @param userId logged-in portal user UUID (from JWT)
     * @return demographics dto
     */
    PortalDemographicsDto getMyDemographics(UUID userId);

    /**
     * Update demographics for the given portal user.
     * @param userId logged-in portal user UUID (from JWT)
     * @param dto demographics payload
     * @return updated demographics dto
     */
    PortalDemographicsDto updateMyDemographics(UUID userId, PortalDemographicsDto dto);
}
//