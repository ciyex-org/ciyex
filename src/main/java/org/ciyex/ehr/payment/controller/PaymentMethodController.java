package org.ciyex.ehr.payment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.payment.dto.PatientPaymentMethodDto;
import org.ciyex.ehr.payment.service.PaymentMethodService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.NoSuchElementException;

@PreAuthorize("hasAuthority('SCOPE_user/Claim.read')")
@RestController
@RequestMapping("/api/payments/methods")
@RequiredArgsConstructor
@Slf4j
public class PaymentMethodController {

    private final PaymentMethodService service;

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<PatientPaymentMethodDto>>> getByPatient(@PathVariable Long patientId) {
        try {
            var methods = service.getByPatient(patientId);
            return ResponseEntity.ok(ApiResponse.ok("Payment methods retrieved", methods));
        } catch (Exception e) {
            log.error("Failed to get payment methods for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientPaymentMethodDto>> getById(@PathVariable Long id) {
        try {
            var method = service.getById(id);
            return ResponseEntity.ok(ApiResponse.ok("Payment method retrieved", method));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get payment method {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/patient/{patientId}")
    @PreAuthorize("hasAuthority('SCOPE_user/Claim.write')")
    public ResponseEntity<ApiResponse<PatientPaymentMethodDto>> create(
            @PathVariable Long patientId, @RequestBody PatientPaymentMethodDto dto) {
        try {
            var created = service.create(patientId, dto);
            return ResponseEntity.ok(ApiResponse.ok("Payment method created", created));
        } catch (Exception e) {
            log.error("Failed to create payment method", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Claim.write')")
    public ResponseEntity<ApiResponse<PatientPaymentMethodDto>> update(
            @PathVariable Long id, @RequestBody PatientPaymentMethodDto dto) {
        try {
            var updated = service.update(id, dto);
            return ResponseEntity.ok(ApiResponse.ok("Payment method updated", updated));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update payment method {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Claim.write')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        try {
            service.deactivate(id);
            return ResponseEntity.ok(ApiResponse.ok("Payment method deactivated", null));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to deactivate payment method {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/set-default")
    @PreAuthorize("hasAuthority('SCOPE_user/Claim.write')")
    public ResponseEntity<ApiResponse<PatientPaymentMethodDto>> setDefault(
            @PathVariable Long id, @RequestParam Long patientId) {
        try {
            var method = service.setDefault(patientId, id);
            return ResponseEntity.ok(ApiResponse.ok("Default payment method set", method));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to set default payment method {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }
}
