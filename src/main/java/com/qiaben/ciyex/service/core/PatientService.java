package com.qiaben.ciyex.service.core;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.core.*;
import com.qiaben.ciyex.dto.fhir.*;
import com.qiaben.ciyex.mapper.PatientFhirMapper;
import com.qiaben.ciyex.service.fhir.FhirDiagnosticReportService;
import com.qiaben.ciyex.service.fhir.FhirPatientService;
import com.qiaben.ciyex.service.fhir.OpenEmrAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final FhirPatientService fhirPatientService;
    private final FhirDiagnosticReportService diagnosticReportService;
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
    public ApiResponse<FhirDiagnosticReportDTO> saveDiagnosis(DiagnosisDTO diagnosis) {
        try {
            // Convert DiagnosisDTO to FhirDiagnosticReportDTO
            FhirDiagnosticReportDTO fhirDiagnosticReport = PatientFhirMapper.fromDiagnosisDTO(diagnosis);

            // Create diagnostic report in OpenEMR
            ApiResponse<FhirDiagnosticReportDTO> response = diagnosticReportService.createDiagnosticReport(fhirDiagnosticReport);

            // Return response from OpenEMR
            return response;
        } catch (Exception e) {
            return ApiResponse.<FhirDiagnosticReportDTO>builder()
                    .success(false)
                    .message("Failed to save diagnosis: " + e.getMessage())
                    .build();
        }
    }


    public ApiResponse<?> saveVitalSigns(VitalSignsDTO vitals) {
        try {
            FhirVitalSignsDTO fhirVitals = PatientFhirMapper.fromVitalSignsDTO(vitals);
            return fhirPatientService.saveVitalSigns(fhirVitals);
        } catch (Exception e) {
            return ApiResponse.builder().success(false).message("Failed to save vitals").build();
        }
    }

    public ApiResponse<?> createPayment(PaymentDTO payment) {
        try {
            FhirPaymentDTO fhirPayment = PatientFhirMapper.fromPaymentDTO(payment);
            return fhirPatientService.createPayment(fhirPayment);
        } catch (Exception e) {
            return ApiResponse.builder().success(false).message("Failed to create payment").build();
        }
    }

    public ApiResponse<?> createBill(PatientBillDTO bill) {
        try {
            FhirPatientBillDTO fhirBill = PatientFhirMapper.fromPatientBillDTO(bill);
            return fhirPatientService.createPatientBill(fhirBill);
        } catch (Exception e) {
            return ApiResponse.builder().success(false).message("Failed to create bill").build();
        }
    }
}
