package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.CodeDto;

import java.util.List;
import java.util.Optional;

public interface ExternalCodeStorage {
    String create(CodeDto dto);
    void update(String externalId, CodeDto dto);
    Optional<CodeDto> get(String externalId);
    void delete(String externalId);

    List<CodeDto> searchAll(Long orgId, Long patientId, Long encounterId, String codeType, Boolean active, String q);
}
