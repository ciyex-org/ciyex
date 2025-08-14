package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.MedicationRequestDto;

import java.util.List;

public interface ExternalMedicationRequestStorage {
    String create(MedicationRequestDto dto);
    void update(MedicationRequestDto dto, String externalId);
    MedicationRequestDto get(String externalId);
    void delete(String externalId);
    List<MedicationRequestDto> searchAll();
}
