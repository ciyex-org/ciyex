package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.PatientMedicalHistoryDto;

import java.util.List;
import java.util.Optional;

public interface ExternalPatientMedicalHistoryStorage {
    // Create in external store, returns externalId
    String create(PatientMedicalHistoryDto dto);

    void update(String externalId, PatientMedicalHistoryDto dto);

    Optional<PatientMedicalHistoryDto> get(String externalId);

    void delete(String externalId);

    // Search helpers (org + filters)
    List<PatientMedicalHistoryDto> searchAll(Long orgId, Long patientId);

    List<PatientMedicalHistoryDto> searchAll(Long orgId, Long patientId, Long encounterId);
}
