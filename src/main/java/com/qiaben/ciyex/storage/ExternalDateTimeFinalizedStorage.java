package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.DateTimeFinalizedDto;

import java.util.List;
import java.util.Optional;

public interface ExternalDateTimeFinalizedStorage {
    String create(DateTimeFinalizedDto dto);    // return external id (e.g., Provenance/Composition attester)
    void update(String externalId, DateTimeFinalizedDto dto);
    Optional<DateTimeFinalizedDto> get(String externalId);
    void delete(String externalId);
    List<DateTimeFinalizedDto> searchAll(Long patientId);
    List<DateTimeFinalizedDto> searchAll(Long patientId, Long encounterId);
}
