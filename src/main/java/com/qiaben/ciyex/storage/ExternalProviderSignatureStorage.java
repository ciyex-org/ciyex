package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.ProviderSignatureDto;

import java.util.List;
import java.util.Optional;

public interface ExternalProviderSignatureStorage {
    String create(ProviderSignatureDto dto);
    void update(String externalId, ProviderSignatureDto dto);
    Optional<ProviderSignatureDto> get(String externalId);
    void delete(String externalId);
    List<ProviderSignatureDto> searchAll(Long patientId);
    List<ProviderSignatureDto> searchAll(Long patientId, Long encounterId);
}
