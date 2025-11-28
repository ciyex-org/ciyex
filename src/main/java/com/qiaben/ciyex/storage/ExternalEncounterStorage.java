package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.EncounterDto;

public interface ExternalEncounterStorage extends ExternalStorage<EncounterDto> {

    // Legacy method for compatibility
    void storeEncounter(EncounterDto encounterDto);
}
