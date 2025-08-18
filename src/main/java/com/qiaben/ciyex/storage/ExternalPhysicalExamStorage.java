package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.PhysicalExamDto;
import java.util.List;

public interface ExternalPhysicalExamStorage {
    List<PhysicalExamDto> getAllByPatientId(Long patientId);
    List<PhysicalExamDto> getAllByPatientIdAndEncounterId(Long patientId, Long encounterId);
    PhysicalExamDto getById(Long patientId, Long encounterId, Long examId);
    PhysicalExamDto create(PhysicalExamDto dto);
    PhysicalExamDto update(PhysicalExamDto dto);
    void delete(Long patientId, Long encounterId, Long examId);
}
