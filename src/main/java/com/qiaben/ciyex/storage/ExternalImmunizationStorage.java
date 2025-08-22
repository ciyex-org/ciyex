package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.ImmunizationDto;

import java.util.List;

public interface ExternalImmunizationStorage {

    ImmunizationDto createImmunization(ImmunizationDto dto, Long orgId);

    List<ImmunizationDto> getImmunizationsByOrgId(Long orgId);
}
