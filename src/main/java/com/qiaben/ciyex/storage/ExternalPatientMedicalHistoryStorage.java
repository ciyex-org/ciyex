package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.PatientMedicalHistoryDto;
import java.util.List;

public interface ExternalPatientMedicalHistoryStorage {
    List<PatientMedicalHistoryDto> getPatientMedicalHistory(Long patientId);
    PatientMedicalHistoryDto savePatientMedicalHistory(PatientMedicalHistoryDto dto);
}
