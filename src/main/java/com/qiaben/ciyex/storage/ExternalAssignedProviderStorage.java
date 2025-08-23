package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.AssignedProviderDto;

import java.util.List;
import java.util.Optional;

public interface ExternalAssignedProviderStorage {
    String create(AssignedProviderDto dto);                 // return external id (e.g., CareTeam/Encounter participant)
    void update(String externalId, AssignedProviderDto dto);
    Optional<AssignedProviderDto> get(String externalId);
    void delete(String externalId);
    List<AssignedProviderDto> searchAll(Long orgId, Long patientId);
    List<AssignedProviderDto> searchAll(Long orgId, Long patientId, Long encounterId);
}
