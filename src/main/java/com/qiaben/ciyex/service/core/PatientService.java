package com.qiaben.ciyex.service.core;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.service.fhir.FhirDiagnosticReportService;
import com.qiaben.ciyex.service.fhir.FhirPatientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;


import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientService {

    private final FhirPatientService fhirPatientService;
    private final FhirDiagnosticReportService diagnosticReportService;

    public ApiResponse<Patient> getPatientById(String patientId) {
        log.info("Fetching patient by ID: {}", patientId);
        try {
            Patient patient = fhirPatientService.getPatientByUuid(patientId);
            log.info("Successfully fetched patient: {}", patient.getIdElement().getIdPart());
            return ApiResponse.<Patient>builder()
                    .success(true)
                    .data(patient)
                    .build();
        } catch (Exception e) {
            log.error("Failed to fetch patient with ID {}: {}", patientId, e.getMessage(), e);
            return ApiResponse.<Patient>builder()
                    .success(false)
                    .message("Failed to fetch patient: " + e.getMessage())
                    .build();
        }
    }
    public ApiResponse<List<Map<String, String>>> getRecentPatients(int limit) {
        try {
            Bundle bundle = fhirPatientService.getAllPatients(); // or use sorted query later
            List<Map<String, String>> recent = bundle.getEntry().stream()
                    .map(entry -> (Patient) entry.getResource())
                    .sorted((a, b) -> {
                        // Compare meta.lastUpdated
                        return b.getMeta().getLastUpdated().compareTo(a.getMeta().getLastUpdated());
                    })
                    .limit(5)
                    .map(p -> {
                        Map<String, String> map = new HashMap<>();
                        map.put("id", p.getIdElement().getIdPart());
                        map.put("name", p.getName().isEmpty() ? "Unnamed" : p.getName().get(0).getNameAsSingleString());
                        map.put("birthDate", p.getBirthDate() != null ? p.getBirthDate().toString() : "");
                        map.put("gender", p.getGender() != null ? p.getGender().toCode() : "unknown");
                        map.put("homePhone", p.getTelecom().stream()
                                .filter(t -> t.getSystem() == ContactPoint.ContactPointSystem.PHONE)
                                .findFirst().map(ContactPoint::getValue).orElse(""));
                        map.put("ssn", p.getIdentifier().stream()
                                .filter(i -> i.getType().getText().equalsIgnoreCase("SSN"))
                                .findFirst().map(Identifier::getValue).orElse(""));
                        map.put("externalId", p.getIdentifier().stream()
                                .filter(i -> i.getType().getText().equalsIgnoreCase("External ID"))
                                .findFirst().map(Identifier::getValue).orElse(""));
                        return map;
                    })
                    .toList();

            return ApiResponse.<List<Map<String, String>>>builder()
                    .success(true)
                    .message("Recent patients fetched")
                    .data(recent)
                    .build();
        } catch (Exception e) {
            log.error("Failed to fetch recent patients: {}", e.getMessage());
            return ApiResponse.<List<Map<String, String>>>builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build();
        }
    }


    public ApiResponse<Patient> registerPatient(Patient patient) {
        log.info("Registering new patient with identifier: {}", patient.getIdentifierFirstRep().getValue());
        try {
            Patient createdPatient = fhirPatientService.createPatient(patient);
            log.info("Patient registered successfully with ID: {}", createdPatient.getIdElement().getIdPart());
            return ApiResponse.<Patient>builder()
                    .success(true)
                    .message("Patient registered successfully!")
                    .data(createdPatient)
                    .build();
        } catch (Exception e) {
            log.error("Failed to register patient: {}", e.getMessage(), e);
            return ApiResponse.<Patient>builder()
                    .success(false)
                    .message("Failed to register patient: " + e.getMessage())
                    .build();
        }
    }

    public ApiResponse<DiagnosticReport> saveDiagnosis(DiagnosticReport diagnosis) {
        log.info("Saving diagnostic report for patient: {}", diagnosis.getSubject().getReference());
        ApiResponse<DiagnosticReport> response = diagnosticReportService.createDiagnosticReport(diagnosis);
        if (response.isSuccess()) {
            log.info("Diagnostic report saved successfully with ID: {}", response.getData().getIdElement().getIdPart());
        } else {
            log.error("Failed to save diagnostic report: {}", response.getMessage());
        }
        return response;
    }

    public ApiResponse<Observation> saveVitalSigns(Observation vitals) {
        log.info("Saving vital signs for patient: {}", vitals.getSubject().getReference());
        ApiResponse<Observation> response = fhirPatientService.saveVitalSigns(vitals);
        if (response.isSuccess()) {
            log.info("Vital signs saved successfully with ID: {}", response.getData().getIdElement().getIdPart());
        } else {
            log.error("Failed to save vital signs: {}", response.getMessage());
        }
        return response;
    }

    public ApiResponse<PaymentReconciliation> createPayment(PaymentReconciliation payment) {
        //log.info("Creating payment for patient: {}", payment.getPatient().getReference());
        ApiResponse<PaymentReconciliation> response = fhirPatientService.createPayment(payment);
        if (response.isSuccess()) {
            log.info("Payment created successfully with ID: {}", response.getData().getIdElement().getIdPart());
        } else {
            log.error("Failed to create payment: {}", response.getMessage());
        }
        return response;
    }

    public ApiResponse<Invoice> createBill(Invoice bill) {
        log.info("Creating bill for patient: {}", bill.getSubject().getReference());
        ApiResponse<Invoice> response = fhirPatientService.createPatientBill(bill);
        if (response.isSuccess()) {
            log.info("Bill created successfully with ID: {}", response.getData().getIdElement().getIdPart());
        } else {
            log.error("Failed to create bill: {}", response.getMessage());
        }
        return response;
    }

    public ApiResponse<Bundle> getAllPatients(Map<String, String> queryParams) {
        try {
            Bundle patientsBundle = fhirPatientService.getPatients(queryParams);
            return ApiResponse.<Bundle>builder()
                    .success(true)
                    .data(patientsBundle)
                    .build();
        } catch (Exception e) {
            log.error("Failed to fetch patients: {}", e.getMessage(), e);
            return ApiResponse.<Bundle>builder()
                    .success(false)
                    .message("Failed to fetch patients: " + e.getMessage())
                    .build();
        }

    }
}
