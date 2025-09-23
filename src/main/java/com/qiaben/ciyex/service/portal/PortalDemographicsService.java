package com.qiaben.ciyex.service.portal;

import com.qiaben.ciyex.dto.portal.PortalDemographicsDto;

public interface PortalDemographicsService {

    /**
     * Fetch demographics for the given portal user.
     * @param userId logged-in portal userId (from JWT)
     * @return demographics dto
     */
    PortalDemographicsDto getMyDemographics(Long userId);

    /**
     * Update demographics for the given portal user.
     * @param userId logged-in portal userId (from JWT)
     * @param dto demographics payload
     * @return updated demographics dto
     */
    PortalDemographicsDto updateMyDemographics(Long userId, PortalDemographicsDto dto);
}
