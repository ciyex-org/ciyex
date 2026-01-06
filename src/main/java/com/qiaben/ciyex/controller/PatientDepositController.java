package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.PatientDepositDto;
import com.qiaben.ciyex.dto.PatientDepositRequest;
import com.qiaben.ciyex.service.PatientBillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patient-billing/{patientId}")
@RequiredArgsConstructor
public class PatientDepositController {

    private final PatientBillingService billingService;

    @PostMapping("/deposit")
    public ResponseEntity<PatientDepositDto> addDeposit(
            @PathVariable Long patientId,
            @RequestBody PatientDepositRequest request) {
        return ResponseEntity.ok(billingService.addPatientDeposit(patientId, request));
    }

    @GetMapping("/deposit")
    public ResponseEntity<List<PatientDepositDto>> listDeposits(@PathVariable Long patientId) {
        return ResponseEntity.ok(billingService.getPatientDeposits(patientId));
    }

    @GetMapping("/deposit/{depositId}")
    public ResponseEntity<PatientDepositDto> getDeposit(
            @PathVariable Long patientId,
            @PathVariable Long depositId) {
        return ResponseEntity.ok(billingService.getPatientDeposit(patientId, depositId));
    }

    @PutMapping("/deposit/{depositId}")
    public ResponseEntity<PatientDepositDto> updateDeposit(
            @PathVariable Long patientId,
            @PathVariable Long depositId,
            @RequestBody PatientDepositRequest request) {
        return ResponseEntity.ok(billingService.updatePatientDeposit(patientId, depositId, request));
    }

    @DeleteMapping("/deposit/{depositId}")
    public ResponseEntity<Void> deleteDeposit(
            @PathVariable Long patientId,
            @PathVariable Long depositId) {
        billingService.deletePatientDeposit(patientId, depositId);
        return ResponseEntity.noContent().build();
    }
}
