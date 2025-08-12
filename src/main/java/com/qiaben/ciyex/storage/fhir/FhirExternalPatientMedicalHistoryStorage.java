package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.PatientMedicalHistoryDto;
import com.qiaben.ciyex.storage.ExternalPatientMedicalHistoryStorage;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FhirExternalPatientMedicalHistoryStorage implements ExternalPatientMedicalHistoryStorage {

    @Override
    public List<PatientMedicalHistoryDto> getPatientMedicalHistory(Long patientId) {
        // Implement logic for retrieving from FHIR or any external source
        // Convert the external system data to PatientMedicalHistoryDto and return
        return null;
    }

    @Override
    public PatientMedicalHistoryDto savePatientMedicalHistory(PatientMedicalHistoryDto dto) {
        // Implement logic for saving data into FHIR or any external system
        return null;
    }
}
