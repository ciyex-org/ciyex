package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.EncounterDto;

public interface ExternalEncounterStorage {
    void storeEncounter(EncounterDto encounterDto);
}
