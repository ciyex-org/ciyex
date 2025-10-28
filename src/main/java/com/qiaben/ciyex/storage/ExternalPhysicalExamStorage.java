package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.PhysicalExamDto;

import java.util.List;
import java.util.Optional;

public interface ExternalPhysicalExamStorage {

    String create(PhysicalExamDto dto);

    void update(String externalId, PhysicalExamDto dto);

    Optional<PhysicalExamDto> get(String externalId);

    void delete(String externalId);

    List<PhysicalExamDto> searchAll(Long patientId);

    List<PhysicalExamDto> searchAll(Long patientId, Long encounterId);
}
