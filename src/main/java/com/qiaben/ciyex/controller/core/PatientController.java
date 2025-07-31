package com.qiaben.ciyex.controller.core;

import ca.uhn.fhir.context.FhirContext;
import com.qiaben.ciyex.service.core.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;
    private final FhirContext fhirContext = FhirContext.forR4();

    @GetMapping
    public ResponseEntity<String> getAllPatients(@RequestParam(required = false) Map<String, String> queryParams) {
        Bundle bundle = patientService.getAllPatients(queryParams).getData();
        String json = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle);
        return ResponseEntity.ok(json);
    }

    @GetMapping("/{patientId}")
    public ResponseEntity<String> getPatientById(@PathVariable String patientId) {
        Patient patient = patientService.getPatientById(patientId).getData();
        String json = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(patient);
        return ResponseEntity.ok(json);
    }
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getRecentPatients(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(patientService.getRecentPatients(limit));
    }


    @PostMapping("/register")
    public ResponseEntity<String> registerPatient(@RequestBody String body) {
        Patient patient = fhirContext.newJsonParser().parseResource(Patient.class, body);
        Patient createdPatient = patientService.registerPatient(patient).getData();
        String json = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(createdPatient);
        return ResponseEntity.ok(json);
    }


    @PostMapping("/vitals")
    public ResponseEntity<String> saveVitalSigns(@Valid @RequestBody Observation vitals) {
        Observation savedVitals = patientService.saveVitalSigns(vitals).getData();
        String json = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(savedVitals);
        return ResponseEntity.ok(json);
    }

    @PostMapping("/payment")
    public ResponseEntity<String> createPayment(@Valid @RequestBody PaymentReconciliation payment) {
        PaymentReconciliation savedPayment = patientService.createPayment(payment).getData();
        String json = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(savedPayment);
        return ResponseEntity.ok(json);
    }

    @PostMapping("/patient-bill")
    public ResponseEntity<String> createBill(@Valid @RequestBody Invoice bill) {
        Invoice savedBill = patientService.createBill(bill).getData();
        String json = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(savedBill);
        return ResponseEntity.ok(json);
    }

}

