//package com.qiaben.ciyex.controller;
//
//
//
//import com.qiaben.ciyex.dto.*;
//import com.qiaben.ciyex.service.PatientBillingService;
//import com.qiaben.ciyex.dto.ApiResponse;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//import org.springframework.http.MediaType;
//
//import java.util.List;
//
//@Slf4j
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/api/patient-billing/{patientId}")
//public class PatientBillingController {
//
//    private final PatientBillingService service;
//
//    /* ===================== Invoices ===================== */
//
//    @GetMapping("/invoices")
//    public ResponseEntity<ApiResponse<List<PatientInvoiceDto>>> listInvoices(
//            @RequestHeader("x-org-id") Long orgId,
//            @PathVariable Long patientId) {
//
//        var data = service.listInvoices(orgId, patientId);
//        var resp = new ApiResponse.Builder<List<PatientInvoiceDto>>()
//                .success(true)
//                .message("Invoices loaded")
//                .data(data)
//                .build();
//        return ResponseEntity.ok(resp);
//    }
//
//    /** +Add Procedure → create invoice (JSON body) */
//    @PostMapping("/invoices")
//    public ResponseEntity<ApiResponse<PatientInvoiceDto>> createInvoiceFromProcedure(
//            @RequestHeader("x-org-id") Long orgId,
//            @PathVariable Long patientId,
//            @RequestBody PatientBillingService.CreateInvoiceRequest body) {
//
//        var data = service.createInvoiceFromProcedure(orgId, patientId, body);
//        var resp = new ApiResponse.Builder<PatientInvoiceDto>()
//                .success(true)
//                .message("Invoice created")
//                .data(data)
//                .build();
//        return ResponseEntity.ok(resp);
//    }
//
//    /** Edit line amount (re-estimate) — JSON body { newCharge } */
//    @PutMapping("/invoices/{invoiceId}/lines/{lineId}")
//    public ResponseEntity<ApiResponse<PatientInvoiceDto>> updateInvoiceLineAmount(
//            @RequestHeader("x-org-id") Long orgId,
//            @PathVariable Long patientId,
//            @PathVariable Long invoiceId,
//            @PathVariable Long lineId,
//            @RequestBody PatientBillingService.UpdateLineAmountRequest body) {
//
//        var data = service.updateInvoiceLineAmount(orgId, patientId, invoiceId, lineId, body);
//        var resp = new ApiResponse.Builder<PatientInvoiceDto>()
//                .success(true)
//                .message("Line updated")
//                .data(data)
//                .build();
//        return ResponseEntity.ok(resp);
//    }
//
//    /** Percentage adjustment — JSON body { percent } */
//    @PostMapping("/invoices/{invoiceId}/adjustment")
//    public ResponseEntity<ApiResponse<PatientInvoiceDto>> applyInvoicePercentageAdjustment(
//            @RequestHeader("x-org-id") Long orgId,
//            @PathVariable Long patientId,
//            @PathVariable Long invoiceId,
//            @RequestBody PatientBillingService.PercentageAdjustmentRequest body) {
//
//        var data = service.applyInvoicePercentageAdjustment(orgId, patientId, invoiceId, body);
//        var resp = new ApiResponse.Builder<PatientInvoiceDto>()
//                .success(true)
//                .message("Adjustment applied")
//                .data(data)
//                .build();
//        return ResponseEntity.ok(resp);
//    }
//
//    /* ===================== Claims ===================== */
//
//    @PostMapping("/invoices/{invoiceId}/claim/promote")
//    public ResponseEntity<ApiResponse<PatientClaimDto>> promoteClaim(
//            @RequestHeader("x-org-id") Long orgId,
//            @PathVariable Long patientId,
//            @PathVariable Long invoiceId) {
//
//        var data = service.promoteClaim(orgId, patientId, invoiceId);
//        var resp = new ApiResponse.Builder<PatientClaimDto>()
//                .success(true)
//                .message("Claim promoted")
//                .data(data)
//                .build();
//        return ResponseEntity.ok(resp);
//    }
//
//
//
//
//    @PostMapping("/invoices/{invoiceId}/claim/send-to-batch")
//    public ResponseEntity<ApiResponse<PatientClaimDto>> sendClaimToBatch(
//            @RequestHeader("x-org-id") Long orgId,
//            @PathVariable Long patientId,
//            @PathVariable Long invoiceId) {
//
//        var data = service.sendClaimToBatch(orgId, patientId, invoiceId);
//        var resp = new ApiResponse.Builder<PatientClaimDto>()
//                .success(true)
//                .message("Claim moved to batch")
//                .data(data)
//                .build();
//        return ResponseEntity.ok(resp);
//    }
//
//    @PostMapping("/invoices/{invoiceId}/claim/submit")
//    public ResponseEntity<ApiResponse<PatientClaimDto>> submitClaim(
//            @RequestHeader("x-org-id") Long orgId,
//            @PathVariable Long patientId,
//            @PathVariable Long invoiceId) {
//
//        var data = service.submitClaim(orgId, patientId, invoiceId);
//        var resp = new ApiResponse.Builder<PatientClaimDto>()
//                .success(true)
//                .message("Claim submitted")
//                .data(data)
//                .build();
//        return ResponseEntity.ok(resp);
//    }
//
//    @PostMapping("/invoices/{invoiceId}/claim/close")
//    public ResponseEntity<ApiResponse<PatientClaimDto>> closeClaim(
//            @RequestHeader("x-org-id") Long orgId,
//            @PathVariable Long patientId,
//            @PathVariable Long invoiceId) {
//
//        var data = service.closeClaim(orgId, patientId, invoiceId);
//        var resp = new ApiResponse.Builder<PatientClaimDto>()
//                .success(true)
//                .message("Claim closed")
//                .data(data)
//                .build();
//        return ResponseEntity.ok(resp);
//    }
//
//    @PostMapping("/invoices/{invoiceId}/claim/void-recreate")
//    public ResponseEntity<ApiResponse<PatientClaimDto>> voidAndRecreateClaim(
//            @RequestHeader("x-org-id") Long orgId,
//            @PathVariable Long patientId,
//            @PathVariable Long invoiceId) {
//
//        var data = service.voidAndRecreateClaim(orgId, patientId, invoiceId);
//        var resp = new ApiResponse.Builder<PatientClaimDto>()
//                .success(true)
//                .message("Claim voided and recreated")
//                .data(data)
//                .build();
//        return ResponseEntity.ok(resp);
//    }
//
//    /** Compose/Edit core claim fields */
//    @PutMapping("/invoices/{invoiceId}/claim")
//    public ResponseEntity<ApiResponse<PatientClaimDto>> updateClaim(
//            @RequestHeader("x-org-id") Long orgId,
//            @PathVariable Long patientId,
//            @PathVariable Long invoiceId,
//            @RequestBody PatientBillingService.PatientClaimCoreUpdate body) {
//
//        var data = service.updateClaim(orgId, patientId, invoiceId, body);
//        var resp = new ApiResponse.Builder<PatientClaimDto>()
//                .success(true)
//                .message("Claim updated")
//                .data(data)
//                .build();
//        return ResponseEntity.ok(resp);
//    }
//
//
//
//    /* ================= Insurance Payment ================= */
//
//    /** Insurance payment grid → Apply */
//    @PostMapping("/invoices/{invoiceId}/insurance-payments")
//    public ResponseEntity<ApiResponse<PatientInvoiceDto>> applyInsurancePayment(
//            @RequestHeader("x-org-id") Long orgId,
//            @PathVariable Long patientId,
//            @PathVariable Long invoiceId,
//            @RequestBody PatientInsurancePaymentRequestDto body) {
//
//        var data = service.applyInsurancePayment(orgId, patientId, invoiceId, body);
//        var resp = new ApiResponse.Builder<PatientInvoiceDto>()
//                .success(true)
//                .message("Insurance payment applied")
//                .data(data)
//                .build();
//        return ResponseEntity.ok(resp);
//    }
//
//    /* ================= Patient Payment & Credit ================= */
//
//    /** Patient payment → Apply */
//    @PostMapping("/invoices/{invoiceId}/patient-payments")
//    public ResponseEntity<ApiResponse<PatientInvoiceDto>> applyPatientPayment(
//            @RequestHeader("x-org-id") Long orgId,
//            @PathVariable Long patientId,
//            @PathVariable Long invoiceId,
//            @RequestBody PatientPatientPaymentRequestDto body) {
//
//        var data = service.applyPatientPayment(orgId, patientId, invoiceId, body);
//        var resp = new ApiResponse.Builder<PatientInvoiceDto>()
//                .success(true)
//                .message("Patient payment applied")
//                .data(data)
//                .build();
//        return ResponseEntity.ok(resp);
//    }
//
//    @GetMapping("/account-credit")
//    public ResponseEntity<ApiResponse<PatientAccountCreditDto>> getAccountCredit(
//            @RequestHeader("x-org-id") Long orgId,
//            @PathVariable Long patientId) {
//
//        var data = service.getAccountCredit(orgId, patientId);
//        var resp = new ApiResponse.Builder<PatientAccountCreditDto>()
//                .success(true)
//                .message("Account credit loaded")
//                .data(data)
//                .build();
//        return ResponseEntity.ok(resp);
//    }
//
//    /** Apply account credit — JSON body { amount } */
//    @PostMapping("/account-credit/apply")
//    public ResponseEntity<ApiResponse<PatientAccountCreditDto>> applyAccountCredit(
//            @RequestHeader("x-org-id") Long orgId,
//            @PathVariable Long patientId,
//            @RequestBody PatientBillingService.ApplyCreditRequest body) {
//
//        var data = service.applyAccountCredit(orgId, patientId, body);
//        var resp = new ApiResponse.Builder<PatientAccountCreditDto>()
//                .success(true)
//                .message("Credit applied")
//                .data(data)
//                .build();
//        return ResponseEntity.ok(resp);
//    }
//
//    /** Delete an invoice by orgId, patientId, and invoiceId */
//    @DeleteMapping("/invoices/{invoiceId}/delete")
//    public ResponseEntity<Void> deleteInvoice(@PathVariable Long orgId, @PathVariable Long patientId, @PathVariable Long invoiceId) {
//        service.deleteInvoice(orgId, patientId, invoiceId);
//        return ResponseEntity.noContent().build();
//    }
//
//
//}


package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.*;
import com.qiaben.ciyex.service.PatientBillingService;
import com.qiaben.ciyex.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/patient-billing/{patientId}")
public class PatientBillingController {

    private final PatientBillingService service;

    /* ===================== Invoices ===================== */

    @GetMapping("/invoices")
    public ResponseEntity<ApiResponse<List<PatientInvoiceDto>>> listInvoices(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId) {

        var data = service.listInvoices(orgId, patientId);
        var resp = new ApiResponse.Builder<List<PatientInvoiceDto>>()
                .success(true)
                .message("Invoices loaded")
                .data(data)
                .build();
        return ResponseEntity.ok(resp);
    }

    /** +Add Procedure → create invoice (JSON body) */
    @PostMapping("/invoices")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> createInvoiceFromProcedure(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @RequestBody PatientBillingService.CreateInvoiceRequest body) {

        var data = service.createInvoiceFromProcedure(orgId, patientId, body);
        var resp = new ApiResponse.Builder<PatientInvoiceDto>()
                .success(true)
                .message("Invoice created")
                .data(data)
                .build();
        return ResponseEntity.ok(resp);
    }

    /** Edit line amount (re-estimate) — JSON body { newCharge } */
    @PutMapping("/invoices/{invoiceId}/lines/{lineId}")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> updateInvoiceLineAmount(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @PathVariable Long lineId,
            @RequestBody PatientBillingService.UpdateLineAmountRequest body) {

        var data = service.updateInvoiceLineAmount(orgId, patientId, invoiceId, lineId, body);
        var resp = new ApiResponse.Builder<PatientInvoiceDto>()
                .success(true)
                .message("Line updated")
                .data(data)
                .build();
        return ResponseEntity.ok(resp);
    }

    /** Percentage adjustment — JSON body { percent } */
    @PostMapping("/invoices/{invoiceId}/adjustment")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> applyInvoicePercentageAdjustment(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @RequestBody PatientBillingService.PercentageAdjustmentRequest body) {

        var data = service.applyInvoicePercentageAdjustment(orgId, patientId, invoiceId, body);
        var resp = new ApiResponse.Builder<PatientInvoiceDto>()
                .success(true)
                .message("Adjustment applied")
                .data(data)
                .build();
        return ResponseEntity.ok(resp);
    }

    /* ===================== Claims ===================== */

    /** NEW: Get all claims across all invoices for this patient */
    @GetMapping("/claims")
    public ResponseEntity<ApiResponse<List<PatientClaimDto>>> listAllClaimsForPatient(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId) {

        var data = service.listAllClaimsForPatient(orgId, patientId);
        var resp = new ApiResponse.Builder<List<PatientClaimDto>>()
                .success(true)
                .message("Claims loaded")
                .data(data)
                .build();
        return ResponseEntity.ok(resp);
    }

    /** NEW: Get the active/latest claim for a specific invoice */
    @GetMapping("/invoices/{invoiceId}/claim")
    public ResponseEntity<ApiResponse<PatientClaimDto>> getActiveClaimForInvoice(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        var data = service.getActiveClaimForInvoice(orgId, patientId, invoiceId);
        var resp = new ApiResponse.Builder<PatientClaimDto>()
                .success(true)
                .message("Claim loaded")
                .data(data)
                .build();
        return ResponseEntity.ok(resp);
    }

    /** NEW (optional): Get all (including historical) claims for a specific invoice */
    @GetMapping("/invoices/{invoiceId}/claims")
    public ResponseEntity<ApiResponse<List<PatientClaimDto>>> listClaimsForInvoice(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        var data = service.listClaimsForInvoice(orgId, patientId, invoiceId);
        var resp = new ApiResponse.Builder<List<PatientClaimDto>>()
                .success(true)
                .message("Invoice claims loaded")
                .data(data)
                .build();
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/invoices/{invoiceId}/claim/promote")
    public ResponseEntity<ApiResponse<PatientClaimDto>> promoteClaim(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        var data = service.promoteClaim(orgId, patientId, invoiceId);
        var resp = new ApiResponse.Builder<PatientClaimDto>()
                .success(true)
                .message("Claim promoted")
                .data(data)
                .build();
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/invoices/{invoiceId}/claim/send-to-batch")
    public ResponseEntity<ApiResponse<PatientClaimDto>> sendClaimToBatch(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        var data = service.sendClaimToBatch(orgId, patientId, invoiceId);
        var resp = new ApiResponse.Builder<PatientClaimDto>()
                .success(true)
                .message("Claim moved to batch")
                .data(data)
                .build();
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/invoices/{invoiceId}/claim/submit")
    public ResponseEntity<ApiResponse<PatientClaimDto>> submitClaim(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        var data = service.submitClaim(orgId, patientId, invoiceId);
        var resp = new ApiResponse.Builder<PatientClaimDto>()
                .success(true)
                .message("Claim submitted")
                .data(data)
                .build();
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/invoices/{invoiceId}/claim/close")
    public ResponseEntity<ApiResponse<PatientClaimDto>> closeClaim(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        var data = service.closeClaim(orgId, patientId, invoiceId);
        var resp = new ApiResponse.Builder<PatientClaimDto>()
                .success(true)
                .message("Claim closed")
                .data(data)
                .build();
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/invoices/{invoiceId}/claim/void-recreate")
    public ResponseEntity<ApiResponse<PatientClaimDto>> voidAndRecreateClaim(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        var data = service.voidAndRecreateClaim(orgId, patientId, invoiceId);
        var resp = new ApiResponse.Builder<PatientClaimDto>()
                .success(true)
                .message("Claim voided and recreated")
                .data(data)
                .build();
        return ResponseEntity.ok(resp);
    }

    /** Compose/Edit core claim fields */
    @PutMapping("/invoices/{invoiceId}/claim")
    public ResponseEntity<ApiResponse<PatientClaimDto>> updateClaim(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @RequestBody PatientBillingService.PatientClaimCoreUpdate body) {

        var data = service.updateClaim(orgId, patientId, invoiceId, body);
        var resp = new ApiResponse.Builder<PatientClaimDto>()
                .success(true)
                .message("Claim updated")
                .data(data)
                .build();
        return ResponseEntity.ok(resp);
    }


    /* ================= Insurance Payment ================= */
    @GetMapping("/insurance-payments")
    public ResponseEntity<ApiResponse<List<PatientInsuranceRemitLineDto>>> listInsurancePayments(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @RequestParam(required = false) Long invoiceId,
            @RequestParam(required = false) Long claimId,
            @RequestParam(required = false) Long insuranceId) {

        var data = service.listInsurancePayments(orgId, patientId, invoiceId, claimId, insuranceId);
        return ResponseEntity.ok(
                new ApiResponse.Builder<List<PatientInsuranceRemitLineDto>>()
                        .success(true).message("Insurance payments loaded").data(data).build()
        );
    }

    /** Invoice-scope: list insurance payments for one invoice (matches POST invoice path) */
    @GetMapping("/invoices/{invoiceId}/insurance-payments")
    public ResponseEntity<ApiResponse<List<PatientInsuranceRemitLineDto>>> listInsurancePaymentsForInvoice(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        var data = service.listInsurancePayments(orgId, patientId, invoiceId, null, null);
        return ResponseEntity.ok(
                new ApiResponse.Builder<List<PatientInsuranceRemitLineDto>>()
                        .success(true).message("Insurance payments for invoice loaded").data(data).build()
        );
    }


    /** Insurance payment grid → Apply */
    @PostMapping("/invoices/{invoiceId}/insurance-payments")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> applyInsurancePayment(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @RequestBody PatientInsurancePaymentRequestDto body) {

        var data = service.applyInsurancePayment(orgId, patientId, invoiceId, body);
        var resp = new ApiResponse.Builder<PatientInvoiceDto>()
                .success(true)
                .message("Insurance payment applied")
                .data(data)
                .build();
        return ResponseEntity.ok(resp);
    }

    /* ================= Patient Payment & Credit ================= */

//    /** Patient payment → Apply */
//    @PostMapping("/invoices/{invoiceId}/patient-payments")
//    public ResponseEntity<ApiResponse<PatientInvoiceDto>> applyPatientPayment(
//            @RequestHeader("x-org-id") Long orgId,
//            @PathVariable Long patientId,
//            @PathVariable Long invoiceId,
//            @RequestBody PatientPatientPaymentRequestDto body) {
//
//        var data = service.applyPatientPayment(orgId, patientId, invoiceId, body);
//        var resp = new ApiResponse.Builder<PatientInvoiceDto>()
//                .success(true)
//                .message("Patient payment applied")
//                .data(data)
//                .build();
//        return ResponseEntity.ok(resp);
//    }


    /** ✅ Patient payment → Apply (POST) */
    @PostMapping("/invoices/{invoiceId}/patient-payments")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> applyPatientPayment(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @RequestBody PatientPatientPaymentRequestDto body) {

        var data = service.applyPatientPayment(orgId, patientId, invoiceId, body);
        var resp = new ApiResponse.Builder<PatientInvoiceDto>()
                .success(true)
                .message("Patient payment applied")
                .data(data)
                .build();
        return ResponseEntity.ok(resp);
    }

    /** ✅ GET all payments for a patient */
    @GetMapping("/patient-payments")
    public ResponseEntity<ApiResponse<List<PatientPatientPaymentAllocationDto>>> getAllPatientPayments(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId) {

        var data = service.getAllPatientPayments(orgId, patientId);
        var resp = new ApiResponse.Builder<List<PatientPatientPaymentAllocationDto>>()
                .success(true)
                .message("All patient payments fetched successfully")
                .data(data)
                .build();
        return ResponseEntity.ok(resp);
    }

    /** ✅ GET payments by invoice */
    @GetMapping("/invoices/{invoiceId}/patient-payments")
    public ResponseEntity<ApiResponse<List<PatientPatientPaymentAllocationDto>>> getPaymentsByInvoice(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        var data = service.getPatientPaymentsByInvoice(orgId, patientId, invoiceId);
        var resp = new ApiResponse.Builder<List<PatientPatientPaymentAllocationDto>>()
                .success(true)
                .message("Patient payments for invoice fetched successfully")
                .data(data)
                .build();
        return ResponseEntity.ok(resp);
    }
    /* ================= Account credit ================= */

    @GetMapping("/account-credit")
    public ResponseEntity<ApiResponse<PatientAccountCreditDto>> getAccountCredit(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId) {

        var data = service.getAccountCredit(orgId, patientId);
        var resp = new ApiResponse.Builder<PatientAccountCreditDto>()
                .success(true)
                .message("Account credit loaded")
                .data(data)
                .build();
        return ResponseEntity.ok(resp);
    }

    /** Apply account credit — JSON body { amount } */
    @PostMapping("/account-credit/apply")
    public ResponseEntity<ApiResponse<PatientAccountCreditDto>> applyAccountCredit(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @RequestBody PatientBillingService.ApplyCreditRequest body) {

        var data = service.applyAccountCredit(orgId, patientId, body);
        var resp = new ApiResponse.Builder<PatientAccountCreditDto>()
                .success(true)
                .message("Credit applied")
                .data(data)
                .build();
        return ResponseEntity.ok(resp);
    }

    /** FIXED: Read orgId from header (was incorrectly a @PathVariable) */
    @DeleteMapping("/invoices/{invoiceId}/delete")
    public ResponseEntity<Void> deleteInvoice(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {
        service.deleteInvoice(orgId, patientId, invoiceId);
        return ResponseEntity.noContent().build();
    }

    /** Upload claim attachment (image/pdf) */
    @PostMapping(value = "/invoices/{invoiceId}/claim/{claimId}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> uploadClaimAttachment(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @PathVariable Long claimId,
            @RequestParam("file") MultipartFile file) {
        service.uploadClaimAttachment(orgId, patientId, invoiceId, claimId, file);
        var resp = new ApiResponse.Builder<Void>()
                .success(true)
                .message("Attachment uploaded successfully")
                .build();
        return ResponseEntity.ok(resp);
    }





    /** Upload EOB document (image/pdf) */
    @PostMapping(value = "/invoices/{invoiceId}/claim/{claimId}/eob", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> uploadEobDocument(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @PathVariable Long claimId,
            @RequestParam("file") MultipartFile file) {
        service.uploadEobDocument(orgId, patientId, invoiceId, claimId, file);
        var resp = new ApiResponse.Builder<Void>()
                .success(true)
                .message("EOB uploaded successfully")
                .build();
        return ResponseEntity.ok(resp);
    }
}


