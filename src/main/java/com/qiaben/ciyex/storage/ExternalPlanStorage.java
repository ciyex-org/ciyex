package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.PlanDto;
import java.util.List;
import java.util.Optional;

public interface ExternalPlanStorage {
    String create(PlanDto dto);
    void update(String externalId, PlanDto dto);
    Optional<PlanDto> get(String externalId);
    void delete(String externalId);
    List<PlanDto> searchAll(Long orgId, Long patientId);
    List<PlanDto> searchAll(Long orgId, Long patientId, Long encounterId);
}
