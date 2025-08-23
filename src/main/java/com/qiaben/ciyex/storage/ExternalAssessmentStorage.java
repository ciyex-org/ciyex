package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.AssessmentDto;

import java.util.List;
import java.util.Optional;

public interface ExternalAssessmentStorage {

    String create(AssessmentDto dto);

    void update(String externalId, AssessmentDto dto);

    Optional<AssessmentDto> get(String externalId);

    void delete(String externalId);

    List<AssessmentDto> searchAll(Long orgId, Long patientId);

    List<AssessmentDto> searchAll(Long orgId, Long patientId, Long encounterId);
}
