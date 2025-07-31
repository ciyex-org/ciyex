package com.qiaben.ciyex.service.core;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.service.fhir.FhirDiagnosticReportService;
import com.qiaben.ciyex.service.fhir.FhirPatientService;
import com.qiaben.ciyex.service.fhir.OpenEmrAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientService {

    private final FhirPatientService fhirPatientService;
    private final FhirDiagnosticReportService diagnosticReportService;
    private final OpenEmrAuthService openEmrAuthService;

    // ✅ Fetch patient by UUID
    public ApiResponse<Patient> getPatientById(String patientId) {
        log.info("Fetching patient by ID: {}", patientId);
        try {
            Patient patient = fhirPatientService.getPatientByUuid(patientId);
            return ApiResponse.<Patient>builder()
                    .success(true)
                    .data(patient)
                    .build();
        } catch (Exception e) {
            log.error("Failed to fetch patient with ID {}: {}", patientId, e.getMessage());
            return ApiResponse.<Patient>builder()
                    .success(false)
                    .message("Invalid or missing patient UUID")
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


    // ✅ Register a new patient
    public ApiResponse<Patient> registerPatient(Patient patient) {
        try {
            Patient created = fhirPatientService.createPatient(patient);
            return ApiResponse.<Patient>builder()
                    .success(true)
                    .message("Patient registered successfully")
                    .data(created)
                    .build();
        } catch (Exception e) {
            log.error("Registration failed: {}", e.getMessage());
            return ApiResponse.<Patient>builder()
                    .success(false)
                    .message("Registration failed: " + e.getMessage())
                    .build();
        }
    }

    // ✅ Save diagnostic report
    public ApiResponse<DiagnosticReport> saveDiagnosis(DiagnosticReport report) {
        ApiResponse<DiagnosticReport> res = diagnosticReportService.createDiagnosticReport(report);
        if (!res.isSuccess()) log.error("Diagnosis save failed: {}", res.getMessage());
        return res;
    }

    // ✅ Save vital signs
    public ApiResponse<Observation> saveVitalSigns(Observation vitals) {
        ApiResponse<Observation> res = fhirPatientService.saveVitalSigns(vitals);
        if (!res.isSuccess()) log.error("Vital signs save failed: {}", res.getMessage());
        return res;
    }

    // ✅ Create payment
    public ApiResponse<PaymentReconciliation> createPayment(PaymentReconciliation payment) {
        ApiResponse<PaymentReconciliation> res = fhirPatientService.createPayment(payment);
        if (!res.isSuccess()) log.error("Payment failed: {}", res.getMessage());
        return res;
    }

    // ✅ Create bill
    public ApiResponse<Invoice> createBill(Invoice bill) {
        ApiResponse<Invoice> res = fhirPatientService.createPatientBill(bill);
        if (!res.isSuccess()) log.error("Bill creation failed: {}", res.getMessage());
        return res;
    }

    // ✅ Get all raw FHIR patients (Bundle)
    public ApiResponse<Bundle> getAllPatients(Map<String, String> queryParams) {
        try {
            Bundle bundle = fhirPatientService.getPatients(queryParams);
            return ApiResponse.<Bundle>builder()
                    .success(true)
                    .data(bundle)
                    .build();
        } catch (Exception e) {
            log.error("Error fetching patients: {}", e.getMessage());
            return ApiResponse.<Bundle>builder()
                    .success(false)
                    .message("Fetch error: " + e.getMessage())
                    .build();
        }
    }

    // ✅ Simplified List
    public ApiResponse<List<Map<String, String>>> getSimplifiedPatientList() {
        try {
            List<Map<String, String>> list = fhirPatientService.getSimplifiedPatients();
            return ApiResponse.<List<Map<String, String>>>builder()
                    .success(true)
                    .message("Simplified patient list fetched")
                    .data(list)
                    .build();
        } catch (Exception e) {
            log.error("Failed to fetch simplified list: {}", e.getMessage());
            return ApiResponse.<List<Map<String, String>>>builder()
                    .success(false)
                    .message("Fetch failed: " + e.getMessage())
                    .build();
        }
    }

    // ✅ Count total patients
    public ApiResponse<Integer> getPatientCount() {
        try {
            int count = fhirPatientService.getAllPatients().getEntry().size();
            return ApiResponse.<Integer>builder()
                    .success(true)
                    .message("Patient count retrieved")
                    .data(count)
                    .build();
        } catch (Exception e) {
            log.error("Count failed: {}", e.getMessage());
            return ApiResponse.<Integer>builder()
                    .success(false)
                    .message("Count fetch error: " + e.getMessage())
                    .build();
        }

    }
}
