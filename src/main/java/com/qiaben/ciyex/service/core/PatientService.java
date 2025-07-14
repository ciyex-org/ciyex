package com.qiaben.ciyex.service.core;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.core.PatientFormDTO;
import com.qiaben.ciyex.dto.fhir.FhirPatientDto;
import com.qiaben.ciyex.dto.fhir.FhirPatientSingleResponseDto;
import com.qiaben.ciyex.mapper.PatientFhirMapper;
import com.qiaben.ciyex.service.fhir.FhirPatientService;
import com.qiaben.ciyex.service.fhir.OpenEmrAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final FhirPatientService fhirPatientService;
    private final OpenEmrAuthService openEmrAuthService;

    public ApiResponse<FhirPatientSingleResponseDto> registerPatient(PatientFormDTO dto) {
        // 1. Map PatientFormDTO to FhirPatientDto
        FhirPatientDto fhirPatient = PatientFhirMapper.fromPatientForm(dto);

        try {
            // 2. Get cached OpenEMR access token
            String token = openEmrAuthService.getCachedAccessToken();

            // 3. Register patient in FHIR (set token dynamically for this request)
            FhirPatientSingleResponseDto response = fhirPatientService.createPatient(fhirPatient, token);

            // 4. Return wrapped API response
            return ApiResponse.<FhirPatientSingleResponseDto>builder()
                    .success(true)
                    .message("Patient registered successfully!")
                    .data(response)
                    .build();
        } catch (Exception e) {
            return ApiResponse.<FhirPatientSingleResponseDto>builder()
                    .success(false)
                    .message("Failed to register patient: " + e.getMessage())
                    .build();
        }
    }
}
