package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.GlobalCodeDto;

import java.util.List;
import java.util.Optional;

public interface ExternalGlobalCodeStorage {
    String create(GlobalCodeDto dto);
    void update(String externalId, GlobalCodeDto dto);
    Optional<GlobalCodeDto> get(String externalId);
    void delete(String externalId);

    List<GlobalCodeDto> searchAll(Long orgId, Long patientId, Long encounterId, String codeType, Boolean active, String q);
}
