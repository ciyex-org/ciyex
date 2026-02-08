package org.ciyex.ehr.controller;

import org.ciyex.ehr.dto.PatientDepositDto;
import org.ciyex.ehr.dto.PatientDepositRequest;
import org.ciyex.ehr.service.PatientBillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Component;

import java.util.List;

// Duplicate endpoints moved to PatientBillingController — disabled to avoid ambiguous mappings.
// To re-enable, restore @RestController and @RequestMapping annotations.
@Component
@RequiredArgsConstructor
@Deprecated
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
