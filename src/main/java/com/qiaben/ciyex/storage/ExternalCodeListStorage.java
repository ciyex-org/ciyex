package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.PatientCodeListDto;
import com.qiaben.ciyex.entity.PatientCodeList;

public interface ExternalCodeListStorage extends ExternalStorage<PatientCodeListDto> {

    // Legacy methods for compatibility
    void save(PatientCodeList patientCodeList);
    void delete(Long id);
    byte[] print(PatientCodeList patientCodeList);
}