package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.FamilyHistoryDto;

import java.util.List;
import java.util.Optional;

public interface ExternalFamilyHistoryStorage {

    String create(FamilyHistoryDto dto);

    void update(String externalId, FamilyHistoryDto dto);

    Optional<FamilyHistoryDto> get(String externalId);

    void delete(String externalId);

    List<FamilyHistoryDto> searchAll(Long orgId, Long patientId);

    List<FamilyHistoryDto> searchAll(Long orgId, Long patientId, Long encounterId);
}
