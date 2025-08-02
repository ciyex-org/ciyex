package com.qiaben.ciyex.controller.core;

import ca.uhn.fhir.context.FhirContext;
import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.service.core.PatientService;
import com.qiaben.ciyex.storage.fhir.FhirPatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;
    private final FhirContext fhirContext = FhirContext.forR4();
    private final FhirPatientService fhirPatientService;

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

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Integer>> getPatientCount() {
        return ResponseEntity.ok(patientService.getPatientCount());
    }

    @GetMapping("/list")
    public ResponseEntity<?> getPatients() {
        Bundle bundle = fhirPatientService.getAllPatients();

        List<Map<String, String>> patients = bundle.getEntry().stream()
                .map(entry -> {
                    Patient patient = (Patient) entry.getResource();
                    return Map.of(
                            "id", String.valueOf(patient.getIdElement().getIdPart()),
                            "fullName", patient.getName().isEmpty() ? "N/A" : patient.getName().get(0).getNameAsSingleString(),
                            "homePhone", patient.getTelecom().isEmpty() ? "N/A" : patient.getTelecom().get(0).getValue(),
                            "dob", patient.hasBirthDate() ? patient.getBirthDate().toString() : "N/A",
                            "externalId", String.valueOf(patient.getIdElement().getIdPart())
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("patients", patients));
    }

}

