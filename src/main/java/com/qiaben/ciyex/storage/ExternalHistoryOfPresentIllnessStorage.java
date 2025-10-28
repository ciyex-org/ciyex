package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.HistoryOfPresentIllnessDto;

import java.util.List;
import java.util.Optional;

public interface ExternalHistoryOfPresentIllnessStorage {

    String create(HistoryOfPresentIllnessDto dto);

    void update(String externalId, HistoryOfPresentIllnessDto dto);

    Optional<HistoryOfPresentIllnessDto> get(String externalId);

    void delete(String externalId);

    List<HistoryOfPresentIllnessDto> searchAll(Long patientId);

    List<HistoryOfPresentIllnessDto> searchAll(Long patientId, Long encounterId);
}
