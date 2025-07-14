package com.qiaben.ciyex.controller.core;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.core.*;
import com.qiaben.ciyex.service.core.PatientService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patient")
public class PatientController {

    private final PatientService patientService;

    @Autowired
    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerPatient(@Valid @RequestBody PatientFormDTO patientForm) {
        ApiResponse<?> response = patientService.registerPatient(patientForm);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/vitals")
    public ResponseEntity<?> saveVitalSigns(@Valid @RequestBody VitalSignsDTO vitals) {
        // Handle saving logic
        return ResponseEntity.ok("Vitals saved!");
    }

    @PostMapping("/diagnosis")
    public ResponseEntity<?> saveDiagnosis(@Valid @RequestBody DiagnosisDTO diagnosis) {
        // Save logic here
        return ResponseEntity.ok("Diagnosis saved!");
    }

    @PostMapping("/payment")
    public ResponseEntity<?> createPayment(@Valid @RequestBody PaymentDTO payment) {
        // Save payment logic here
        return ResponseEntity.ok("Payment recorded!");
    }

    @PostMapping("/patient-bill")
    public ResponseEntity<?> createBill(@Valid @RequestBody PatientBillDTO bill) {
        // Logic here...
        return ResponseEntity.ok("Bill created!");
    }

}
