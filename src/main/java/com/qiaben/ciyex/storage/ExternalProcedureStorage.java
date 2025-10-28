package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.ProcedureDto;

import java.util.List;
import java.util.Optional;

public interface ExternalProcedureStorage {
    String create(ProcedureDto dto);                   // return FHIR Procedure id
    void update(String externalId, ProcedureDto dto);
    Optional<ProcedureDto> get(String externalId);
    void delete(String externalId);
    List<ProcedureDto> searchAll(Long patientId);
    List<ProcedureDto> searchAll(Long patientId, Long encounterId);
}
