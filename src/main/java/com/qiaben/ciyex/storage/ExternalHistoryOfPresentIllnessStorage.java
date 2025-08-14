package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.HistoryOfPresentIllnessDto;

public interface ExternalHistoryOfPresentIllnessStorage {

    // Save History of Present Illness to an external system (e.g., FHIR)
    void saveHistoryOfPresentIllness(HistoryOfPresentIllnessDto historyOfPresentIllnessDto);

    // Get History of Present Illness by ID from an external system (e.g., FHIR)
    HistoryOfPresentIllnessDto getHistoryOfPresentIllnessById(Long id);
}
