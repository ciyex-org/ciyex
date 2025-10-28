package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.FeeScheduleDto;

import java.util.List;
import java.util.Optional;

public interface ExternalEncounterFeeScheduleStorage {
    String create(FeeScheduleDto dto);
    void update(String externalId, FeeScheduleDto dto);
    Optional<FeeScheduleDto> get(String externalId);
    void delete(String externalId);

    List<FeeScheduleDto> searchAll(Long patientId, Long encounterId, String q, String status);
}
