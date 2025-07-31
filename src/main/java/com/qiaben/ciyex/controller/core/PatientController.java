package com.qiaben.ciyex.controller.core;

import ca.uhn.fhir.context.FhirContext;
import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.service.core.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;
    private final FhirContext fhirContext = FhirContext.forR4();

    // ✅ 1. Get Simplified Patient List (for table UI)
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getPatientList() {
        return ResponseEntity.ok(patientService.getSimplifiedPatientList());
    }

    // ✅ 2. Get Total Number of Patients
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Integer>> getPatientCount() {
        return ResponseEntity.ok(patientService.getPatientCount());
    }

    // ✅ 3. Get FHIR Patient by UUID (detailed)
    @GetMapping("/{patientId}")
    public ResponseEntity<?> getPatientById(@PathVariable String patientId) {
        try {
            Patient patient = patientService.getPatientById(patientId).getData();
            if (patient == null) {
                return ResponseEntity.badRequest().body("{\"error\":\"Patient not found\"}");
            }
            String json = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(patient);
            return ResponseEntity.ok(json);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"error\":\"Invalid or malformed UUID\"}");
        }
    }
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getRecentPatients(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(patientService.getRecentPatients(limit));
    }


    // ✅ 4. Register a New Patient (FHIR JSON)
    @PostMapping("/register")
    public ResponseEntity<String> registerPatient(@RequestBody String patientJson) {
        Patient patient = fhirContext.newJsonParser().parseResource(Patient.class, patientJson);
        Patient createdPatient = patientService.registerPatient(patient).getData();
        String response = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(createdPatient);
        return ResponseEntity.ok(response);
    }

    // ✅ 5. Record Vital Signs (FHIR Observation)
    @PostMapping("/vitals")
    public ResponseEntity<String> saveVitalSigns(@Valid @RequestBody Observation vitals) {
        Observation savedVitals = patientService.saveVitalSigns(vitals).getData();
        String json = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(savedVitals);
        return ResponseEntity.ok(json);
    }

    // ✅ 6. Create a Payment for a Patient
    @PostMapping("/payment")
    public ResponseEntity<String> createPayment(@Valid @RequestBody PaymentReconciliation payment) {
        PaymentReconciliation savedPayment = patientService.createPayment(payment).getData();
        String json = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(savedPayment);
        return ResponseEntity.ok(json);
    }

    // ✅ 7. Generate a Bill (FHIR Invoice)
    @PostMapping("/patient-bill")
    public ResponseEntity<String> createBill(@Valid @RequestBody Invoice bill) {
        Invoice savedBill = patientService.createBill(bill).getData();
        String json = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(savedBill);
        return ResponseEntity.ok(json);
    }

    // ✅ 8. Raw FHIR Bundle of All Patients
    @GetMapping
    public ResponseEntity<String> getAllPatients(@RequestParam Map<String, String> queryParams) {
        Bundle patientsBundle = patientService.getAllPatients(queryParams).getData();
        String json = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(patientsBundle);
        return ResponseEntity.ok(json);
    }




}

