package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.SignoffDto;

import java.util.List;
import java.util.Optional;

public interface ExternalSignoffStorage {
    String create(SignoffDto dto);              // return external id (Provenance/Composition id)
    void update(String externalId, SignoffDto dto);
    Optional<SignoffDto> get(String externalId);
    void delete(String externalId);
    List<SignoffDto> searchAll(Long patientId);
    List<SignoffDto> searchAll(Long patientId, Long encounterId);
}
