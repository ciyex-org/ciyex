package org.ciyex.ehr.payment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.payment.dto.PatientLedgerDto;
import org.ciyex.ehr.payment.dto.PaymentPlanDto;
import org.ciyex.ehr.payment.dto.PaymentTransactionDto;
import org.ciyex.ehr.payment.service.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@PreAuthorize("hasAuthority('SCOPE_user/Claim.read')")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService service;

    // ── Stripe PaymentIntent ──────────────────────────────────────────────

    /**
     * Create a Stripe PaymentIntent for collecting payment.
     * Returns clientSecret for frontend Stripe Elements + publishableKey.
     * If Stripe is not configured, returns a mock intent for demo mode.
     */
    @PostMapping("/create-intent")
    @PreAuthorize("hasAuthority('SCOPE_user/Claim.write')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPaymentIntent(@RequestBody Map<String, Object> body) {
        try {
            var result = service.createPaymentIntent(body);
            return ResponseEntity.ok(ApiResponse.ok("Payment intent created", result));
        } catch (Exception e) {
            log.error("Failed to create payment intent", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    /**
     * Get Stripe publishable key for the current org.
     * Returns null/empty if Stripe is not configured (demo mode).
     */
    @GetMapping("/stripe-config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStripePublishableKey() {
        try {
            var config = service.getStripePublishableConfig();
            return ResponseEntity.ok(ApiResponse.ok("Stripe config retrieved", config));
        } catch (Exception e) {
            log.error("Failed to get Stripe config", e);
            return ResponseEntity.ok(ApiResponse.ok("Stripe not configured", Map.of("configured", false)));
        }
    }

    // ── Transactions ─────────────────────────────────────────────────────

    @PostMapping("/collect")
    @PreAuthorize("hasAuthority('SCOPE_user/Claim.write')")
    public ResponseEntity<ApiResponse<PaymentTransactionDto>> collectPayment(@RequestBody PaymentTransactionDto dto) {
        try {
            var txn = service.collectPayment(dto);
            return ResponseEntity.ok(ApiResponse.ok("Payment collected", txn));
        } catch (Exception e) {
            log.error("Failed to collect payment", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<Page<PaymentTransactionDto>>> listTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            var results = service.listTransactions(PageRequest.of(page, size));
            return ResponseEntity.ok(ApiResponse.ok("Transactions retrieved", results));
        } catch (Exception e) {
            log.error("Failed to list transactions", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<ApiResponse<PaymentTransactionDto>> getTransaction(@PathVariable Long id) {
        try {
            var txn = service.getTransaction(id);
            return ResponseEntity.ok(ApiResponse.ok("Transaction retrieved", txn));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get transaction {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/transactions/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<PaymentTransactionDto>>> getByPatient(@PathVariable Long patientId) {
        try {
            var txns = service.getByPatient(patientId);
            return ResponseEntity.ok(ApiResponse.ok("Patient transactions retrieved", txns));
        } catch (Exception e) {
            log.error("Failed to get transactions for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @RequestMapping(value = "/transactions/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @PreAuthorize("hasAuthority('SCOPE_user/Claim.write')")
    public ResponseEntity<ApiResponse<PaymentTransactionDto>> updateTransaction(
            @PathVariable Long id, @RequestBody PaymentTransactionDto dto) {
        try {
            var txn = service.updateTransaction(id, dto);
            return ResponseEntity.ok(ApiResponse.ok("Transaction updated", txn));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update transaction {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/transactions/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Claim.write')")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(@PathVariable Long id) {
        try {
            service.voidTransaction(id);
            return ResponseEntity.ok(ApiResponse.ok("Transaction deleted", null));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete transaction {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/transactions/{id}/refund")
    @PreAuthorize("hasAuthority('SCOPE_user/Claim.write')")
    public ResponseEntity<ApiResponse<PaymentTransactionDto>> refund(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            BigDecimal amount = new BigDecimal(body.get("amount").toString());
            String reason = body.get("reason") != null ? body.get("reason").toString() : null;
            var txn = service.refund(id, amount, reason);
            return ResponseEntity.ok(ApiResponse.ok("Refund processed", txn));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to refund transaction {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/transactions/{id}/void")
    @PreAuthorize("hasAuthority('SCOPE_user/Claim.write')")
    public ResponseEntity<ApiResponse<PaymentTransactionDto>> voidTransaction(@PathVariable Long id) {
        try {
            var txn = service.voidTransaction(id);
            return ResponseEntity.ok(ApiResponse.ok("Transaction voided", txn));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to void transaction {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stats() {
        try {
            var stats = service.transactionStats();
            return ResponseEntity.ok(ApiResponse.ok("Payment stats retrieved", stats));
        } catch (Exception e) {
            log.error("Failed to get payment stats", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    // ── Payment Plans ────────────────────────────────────────────────────

    @GetMapping("/plans/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<PaymentPlanDto>>> listPlans(@PathVariable Long patientId) {
        try {
            var plans = service.listPlans(patientId);
            return ResponseEntity.ok(ApiResponse.ok("Payment plans retrieved", plans));
        } catch (Exception e) {
            log.error("Failed to list payment plans for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/plans")
    @PreAuthorize("hasAuthority('SCOPE_user/Claim.write')")
    public ResponseEntity<ApiResponse<PaymentPlanDto>> createPlan(@RequestBody PaymentPlanDto dto) {
        try {
            var plan = service.createPlan(dto);
            return ResponseEntity.ok(ApiResponse.ok("Payment plan created", plan));
        } catch (Exception e) {
            log.error("Failed to create payment plan", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PutMapping("/plans/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Claim.write')")
    public ResponseEntity<ApiResponse<PaymentPlanDto>> updatePlan(
            @PathVariable Long id, @RequestBody PaymentPlanDto dto) {
        try {
            var plan = service.updatePlan(id, dto);
            return ResponseEntity.ok(ApiResponse.ok("Payment plan updated", plan));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update payment plan {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/plans/{id}/cancel")
    @PreAuthorize("hasAuthority('SCOPE_user/Claim.write')")
    public ResponseEntity<ApiResponse<PaymentPlanDto>> cancelPlan(@PathVariable Long id) {
        try {
            var plan = service.cancelPlan(id);
            return ResponseEntity.ok(ApiResponse.ok("Payment plan cancelled", plan));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to cancel payment plan {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    // ── Ledger ───────────────────────────────────────────────────────────

    @GetMapping("/ledger/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<PatientLedgerDto>>> getLedger(@PathVariable Long patientId) {
        try {
            var ledger = service.getLedger(patientId);
            return ResponseEntity.ok(ApiResponse.ok("Patient ledger retrieved", ledger));
        } catch (Exception e) {
            log.error("Failed to get ledger for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/balance/patient/{patientId}")
    public ResponseEntity<ApiResponse<BigDecimal>> getBalance(@PathVariable Long patientId) {
        try {
            var balance = service.getBalance(patientId);
            return ResponseEntity.ok(ApiResponse.ok("Patient balance retrieved", balance));
        } catch (Exception e) {
            log.error("Failed to get balance for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/ledger/charge")
    @PreAuthorize("hasAuthority('SCOPE_user/Claim.write')")
    public ResponseEntity<ApiResponse<PatientLedgerDto>> postCharge(@RequestBody Map<String, Object> body) {
        try {
            Long patientId = Long.valueOf(body.get("patientId").toString());
            BigDecimal amount = new BigDecimal(body.get("amount").toString());
            String description = body.get("description") != null ? body.get("description").toString() : null;
            String referenceType = body.get("referenceType") != null ? body.get("referenceType").toString() : null;
            Long referenceId = body.get("referenceId") != null ? Long.valueOf(body.get("referenceId").toString()) : null;

            var entry = service.postCharge(patientId, amount, description, referenceType, referenceId);
            return ResponseEntity.ok(ApiResponse.ok("Charge posted", entry));
        } catch (Exception e) {
            log.error("Failed to post charge", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }
}
