package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.ProviderNoteDto;

import java.util.List;
import java.util.Optional;

public interface ExternalProviderNoteStorage {
    String create(ProviderNoteDto dto);                    // return Composition id
    void update(String externalId, ProviderNoteDto dto);   // update Composition
    Optional<ProviderNoteDto> get(String externalId);      // read Composition -> DTO
    void delete(String externalId);                        // delete Composition
    List<ProviderNoteDto> searchAll(Long orgId, Long patientId);
    List<ProviderNoteDto> searchAll(Long orgId, Long patientId, Long encounterId);
}
