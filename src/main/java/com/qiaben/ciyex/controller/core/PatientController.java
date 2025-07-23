package com.qiaben.ciyex.controller.core;

import ca.uhn.fhir.context.FhirContext;
import com.qiaben.ciyex.service.core.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Invoice;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PaymentReconciliation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;
    private final FhirContext fhirContext = FhirContext.forR4();

    @GetMapping("/{patientId}")
    public ResponseEntity<String> getPatientById(@PathVariable String patientId) {
        Patient patient = patientService.getPatientById(patientId).getData();
        String json = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(patient);
        return ResponseEntity.ok(json);
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerPatient(@Valid @RequestBody Patient patient) {
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
