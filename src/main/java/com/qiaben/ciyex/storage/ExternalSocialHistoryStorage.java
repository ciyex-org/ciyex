package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.SocialHistoryDto;

import java.util.List;
import java.util.Optional;

public interface ExternalSocialHistoryStorage {

    String create(SocialHistoryDto dto);

    void update(String externalId, SocialHistoryDto dto);

    Optional<SocialHistoryDto> get(String externalId);

    void delete(String externalId);

    List<SocialHistoryDto> searchAll(Long patientId);

    List<SocialHistoryDto> searchAll(Long patientId, Long encounterId);
}
