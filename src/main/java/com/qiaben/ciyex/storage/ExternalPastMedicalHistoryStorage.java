package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.PastMedicalHistoryDto;

import java.util.List;
import java.util.Optional;

public interface ExternalPastMedicalHistoryStorage {

    String create(PastMedicalHistoryDto dto);

    void update(String externalId, PastMedicalHistoryDto dto);

    Optional<PastMedicalHistoryDto> get(String externalId);

    void delete(String externalId);

    List<PastMedicalHistoryDto> searchAll(Long orgId, Long patientId);

    List<PastMedicalHistoryDto> searchAll(Long orgId, Long patientId, Long encounterId);
}
