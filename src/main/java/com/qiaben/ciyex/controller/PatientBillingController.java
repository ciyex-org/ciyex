


package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.*;
import com.qiaben.ciyex.service.PatientBillingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/patient-billing/{patientId}")
public class PatientBillingController {

    private final PatientBillingService service;

    /* ===================== Invoices ===================== */


    @PostMapping("/invoices/{invoiceId}/backdate")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> backdateInvoice(
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @RequestBody BackdateRequest body) {
        var data = service.backdateInvoice(patientId, invoiceId,
                new PatientBillingService.BackdateRequest(body.date()));
        return ResponseEntity.ok(ApiResponse.ok("Invoice backdated", data));
    }

    @PostMapping("/account-adjustment")
public ResponseEntity<ApiResponse<PatientAccountCreditDto>> accountAdjustment(
        @PathVariable Long patientId,
        @RequestBody AccountAdjustmentRequest body) {
    var req = new PatientBillingService.AccountAdjustmentRequest(
        body.adjustmentType(),
        body.flatRate(),
        body.specificAmount(),
        body.description(),
        body.includeCourtesyCredit()
    );
    var data = service.accountAdjustment(patientId, req);
    return ResponseEntity.ok(ApiResponse.ok("Account adjusted", data));
}

    /** Adjust specific invoice with percentage discount and adjustment type */
    @PostMapping("/invoices/{invoiceId}/adjust")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> adjustInvoice(
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @RequestBody InvoiceAdjustmentRequest body) {
        var data = service.adjustInvoice(patientId, invoiceId, body);
        return ResponseEntity.ok(ApiResponse.ok("Invoice adjusted", data));
    }

    @GetMapping("/invoices")
    public ResponseEntity<ApiResponse<List<PatientInvoiceDto>>> listInvoices(
            @PathVariable Long patientId) {

        var data = service.listInvoices(patientId);
        return ResponseEntity.ok(ApiResponse.ok("Invoices loaded", data));
    }

    /** +Add Procedure → create invoice (JSON body) */
    @PostMapping("/invoices")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> createInvoiceFromProcedure(
            @PathVariable Long patientId,
            @RequestBody PatientBillingService.CreateInvoiceRequest body) {

        var data = service.createInvoiceFromProcedure(patientId, body);
        return ResponseEntity.ok(ApiResponse.ok("Invoice created", data));
    }

    /** Edit line amount (re-estimate) — JSON body { newCharge } */
    @PutMapping("/invoices/{invoiceId}/lines/{lineId}")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> updateInvoiceLineAmount(
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @PathVariable Long lineId,
            @RequestBody PatientBillingService.UpdateLineAmountRequest body) {

        var data = service.updateInvoiceLineAmount(patientId, invoiceId, lineId, body);
        return ResponseEntity.ok(ApiResponse.ok("Line updated", data));
    }

    /** Percentage adjustment — JSON body { percent } */
    @PostMapping("/invoices/{invoiceId}/adjustment")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> applyInvoicePercentageAdjustment(
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @RequestBody PatientBillingService.PercentageAdjustmentRequest body) {

        var data = service.applyInvoicePercentageAdjustment(patientId, invoiceId, body);
        return ResponseEntity.ok(ApiResponse.ok("Adjustment applied", data));
    }
    /** Delete invoice */
    @DeleteMapping("/invoices/{invoiceId}/delete")
    public ResponseEntity<Void> deleteInvoice(
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        service.deleteInvoice(patientId, invoiceId);
        return ResponseEntity.noContent().build();
    }



    /* ===================== Claims ===================== */

    @GetMapping("/claims")
    public ResponseEntity<ApiResponse<List<PatientClaimDto>>> listAllClaimsForPatient(
            @PathVariable Long patientId) {

        var data = service.listAllClaimsForPatient(patientId);
        return ResponseEntity.ok(ApiResponse.ok("Claims loaded", data));
    }

    @GetMapping("/invoices/{invoiceId}/claim")
    public ResponseEntity<ApiResponse<PatientClaimDto>> getActiveClaimForInvoice(
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        var data = service.getActiveClaimForInvoice(patientId, invoiceId);
        return ResponseEntity.ok(ApiResponse.ok("Claim loaded", data));
    }

    @GetMapping("/invoices/{invoiceId}/claims")
    public ResponseEntity<ApiResponse<List<PatientClaimDto>>> listClaimsForInvoice(
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        var data = service.listClaimsForInvoice(patientId, invoiceId);
        return ResponseEntity.ok(ApiResponse.ok("Invoice claims loaded", data));
    }

    @PostMapping("/invoices/{invoiceId}/claim/promote")
    public ResponseEntity<ApiResponse<PatientClaimDto>> promoteClaim(
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        var data = service.promoteClaim(patientId, invoiceId);
        return ResponseEntity.ok(ApiResponse.ok("Claim promoted", data));
    }

    @PostMapping("/invoices/{invoiceId}/claim/send-to-batch")
    public ResponseEntity<ApiResponse<PatientClaimDto>> sendClaimToBatch(
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        var data = service.sendClaimToBatch(patientId, invoiceId);
        return ResponseEntity.ok(ApiResponse.ok("Claim moved to batch", data));
    }

    @PostMapping("/invoices/{invoiceId}/claim/submit")
    public ResponseEntity<ApiResponse<PatientClaimDto>> submitClaim(
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        var data = service.submitClaim(patientId, invoiceId);
        return ResponseEntity.ok(ApiResponse.ok("Claim submitted", data));
    }

    @PostMapping("/invoices/{invoiceId}/claim/close")
    public ResponseEntity<ApiResponse<PatientClaimDto>> closeClaim(
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        var data = service.closeClaim(patientId, invoiceId);
        return ResponseEntity.ok(ApiResponse.ok("Claim closed", data));
    }

    @PostMapping("/invoices/{invoiceId}/claim/void-recreate")
    public ResponseEntity<ApiResponse<PatientClaimDto>> voidAndRecreateClaim(
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        var data = service.voidAndRecreateClaim(patientId, invoiceId);
        return ResponseEntity.ok(ApiResponse.ok("Claim voided and recreated", data));
    }

    /** Compose/Edit core claim fields */
    @PutMapping("/invoices/{invoiceId}/claim")
    public ResponseEntity<ApiResponse<PatientClaimDto>> updateClaim(
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @RequestBody PatientBillingService.PatientClaimCoreUpdate body) {

        var data = service.updateClaim(patientId, invoiceId, body);
        return ResponseEntity.ok(ApiResponse.ok("Claim updated", data));
    }
// --- Attachment & EOB endpoints ---
    @PostMapping(value = "/claims/{claimId}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> uploadClaimAttachment(
            @PathVariable Long patientId,
            @PathVariable Long claimId,
            @RequestParam("file") MultipartFile file) throws Exception {
        service.uploadClaimAttachment(patientId, claimId, file);
        return ResponseEntity.ok(ApiResponse.ok("Attachment uploaded", null));
    }

    @GetMapping("/claims/{claimId}/attachment")
    public ResponseEntity<byte[]> getClaimAttachment(
            @PathVariable Long patientId,
            @PathVariable Long claimId) {
        byte[] data = service.getClaimAttachment(patientId, claimId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @PostMapping(value = "/claims/{claimId}/eob", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> uploadClaimEob(
            @PathVariable Long patientId,
            @PathVariable Long claimId,
            @RequestParam("file") MultipartFile file) throws Exception {
        service.uploadClaimEob(patientId, claimId, file);
        return ResponseEntity.ok(ApiResponse.ok("EOB uploaded", null));
    }

    @GetMapping("/claims/{claimId}/eob")
    public ResponseEntity<byte[]> getClaimEob(
            @PathVariable Long patientId,
            @PathVariable Long claimId) {
        byte[] data = service.getClaimEob(patientId, claimId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }


    /* ================= Insurance Payment ================= */

    @GetMapping("/insurance-payments")
    public ResponseEntity<ApiResponse<List<PatientInsuranceRemitLineDto>>> listInsurancePayments(
            @PathVariable Long patientId,
            @RequestParam(required = false) Long invoiceId,
            @RequestParam(required = false) Long claimId,
            @RequestParam(required = false) Long insuranceId) {

        var data = service.listInsurancePayments(patientId, invoiceId, claimId, insuranceId);
        return ResponseEntity.ok(ApiResponse.ok("Insurance payments loaded", data));
    }

    @GetMapping("/invoices/{invoiceId}/insurance-payments")
    public ResponseEntity<ApiResponse<List<PatientInsuranceRemitLineDto>>> listInsurancePaymentsForInvoice(
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        var data = service.listInsurancePayments(patientId, invoiceId, null, null);
        return ResponseEntity.ok(ApiResponse.ok("Insurance payments for invoice loaded", data));
    }

    /** Apply insurance EOB grid */
    @PostMapping("/invoices/{invoiceId}/insurance-payments")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> applyInsurancePayment(
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @RequestBody PatientInsurancePaymentRequestDto body) {

        var data = service.applyInsurancePayment(patientId, invoiceId, body);
        return ResponseEntity.ok(ApiResponse.ok("Insurance payment applied", data));
    }

    /** EDIT insurance remit line */
    @PutMapping("/invoices/{invoiceId}/insurance-payments/{remitId}")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> editInsuranceRemitLine(
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @PathVariable Long remitId,
            @RequestBody PatientInsuranceRemitLineDto body) {

        var data = service.editInsuranceRemitLine(patientId, invoiceId, remitId, body);
        return ResponseEntity.ok(ApiResponse.ok("Insurance payment updated", data));
    }

    /** VOID insurance payment = hard delete remit line */
    @PostMapping("/invoices/{invoiceId}/insurance-payments/{remitId}/void")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> voidInsurancePayment(
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @PathVariable Long remitId,
            @RequestBody(required = false) PatientBillingService.VoidReason reason) {

        var data = service.voidInsurancePayment(patientId, invoiceId, remitId, reason);
        return ResponseEntity.ok(ApiResponse.ok("Insurance payment voided (deleted)", data));
    }

    /** REFUND insurance → increase invoice insurance balance */
    @PostMapping("/invoices/{invoiceId}/insurance-payments/{remitId}/refund")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> refundInsurancePayment(
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @PathVariable Long remitId,
            @RequestBody PatientBillingService.RefundRequest body) {

        var data = service.refundInsurancePayment(patientId, invoiceId, remitId, body);
        return ResponseEntity.ok(ApiResponse.ok("Insurance payment refunded to insurance balance", data));
    }

    /** TRANSFER (insurance balance → patient account credit) */
    @PostMapping("/invoices/{invoiceId}/insurance-payments/{remitId}/transfer-credit-to-patient")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> transferInsuranceCreditToPatient(
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @PathVariable Long remitId,
            @RequestBody PatientBillingService.TransferCreditRequest body) {

        var data = service.transferInsuranceCreditToPatient(patientId, invoiceId, remitId, body);
        return ResponseEntity.ok(ApiResponse.ok("Insurance credit transferred to patient credit", data));
    }

    /* ================= Patient Payment & Credit ================= */

    /** Patient payment → Apply */
    @PostMapping("/invoices/{invoiceId}/patient-payments")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> applyPatientPayment(
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @RequestBody PatientPatientPaymentRequestDto body) {

        var data = service.applyPatientPayment(patientId, invoiceId, body);
        return ResponseEntity.ok(ApiResponse.ok("Patient payment applied", data));
    }

    /** GET all patient payment allocations for a patient */
    @GetMapping("/patient-payments")
    public ResponseEntity<ApiResponse<List<PatientPatientPaymentAllocationDto>>> getAllPatientPayments(
            @PathVariable Long patientId) {

        var data = service.getAllPatientPayments(patientId);
        return ResponseEntity.ok(ApiResponse.ok("All patient payments fetched", data));
    }

    /** GET allocations by invoice */
    @GetMapping("/invoices/{invoiceId}/patient-payments")
    public ResponseEntity<ApiResponse<List<PatientPatientPaymentAllocationDto>>> getPaymentsByInvoice(
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        var data = service.getPatientPaymentsByInvoice(patientId, invoiceId);
        return ResponseEntity.ok(ApiResponse.ok("Patient payments for invoice fetched", data));
    }

    /** EDIT patient payment */
    @PutMapping("/invoices/{invoiceId}/patient-payments/{paymentId}")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> editPatientPayment(
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @PathVariable Long paymentId,
            @RequestBody PatientPaymentDto body) {

        var data = service.editPatientPayment(patientId, invoiceId, paymentId, body);
        return ResponseEntity.ok(ApiResponse.ok("Patient payment updated", data));
    }

    /** VOID patient payment = delete payment + allocations */
    @PostMapping("/invoices/{invoiceId}/patient-payments/{paymentId}/void")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> voidPatientPayment(
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @PathVariable Long paymentId,
            @RequestBody(required = false) PatientBillingService.VoidReason reason) {

        var data = service.voidPatientPayment(patientId, invoiceId, paymentId, reason);
        return ResponseEntity.ok(ApiResponse.ok("Patient payment voided (deleted)", data));
    }

    /** REFUND patient payment → move to account credit */
    @PostMapping("/invoices/{invoiceId}/patient-payments/{paymentId}/refund")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> refundPatientPayment(
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @PathVariable Long paymentId,
            @RequestBody PatientBillingService.RefundRequest body) {

        var data = service.refundPatientPayment(patientId, invoiceId, paymentId, body);
        return ResponseEntity.ok(ApiResponse.ok("Patient payment refunded to patient credit", data));
    }

    /** Transfer patient credit between patients */
    @PostMapping("/patients/{fromPatientId}/transfer-credit/{toPatientId}")
    public ResponseEntity<ApiResponse<PatientAccountCreditDto[]>> transferPatientCreditToPatient(
            @PathVariable Long fromPatientId,
            @PathVariable Long toPatientId,
            @RequestBody PatientBillingService.TransferCreditRequest body) {

        var data = service.transferPatientCreditToPatient(fromPatientId, toPatientId, body.amount());
        return ResponseEntity.ok(ApiResponse.ok("Patient credit transferred", data));
    }

    /* ================= Account credit ================= */

    @GetMapping("/account-credit")
    public ResponseEntity<ApiResponse<PatientAccountCreditDto>> getAccountCredit(
            @PathVariable Long patientId) {

        var data = service.getAccountCredit(patientId);
        return ResponseEntity.ok(ApiResponse.ok("Account credit loaded", data));
    }

    /** Apply account credit — JSON body { amount } */
    @PostMapping("/account-credit/apply")
    public ResponseEntity<ApiResponse<PatientAccountCreditDto>> applyAccountCredit(
            @PathVariable Long patientId,
            @RequestBody PatientBillingService.ApplyCreditRequest body) {

        var data = service.applyAccountCredit(patientId, body);
        return ResponseEntity.ok(ApiResponse.ok("Credit applied", data));
    }



    /**
     * List notes for a specific invoice (+ symbol in UI triggers this)
     */
    @GetMapping("/invoices/{invoiceId}/notes")
    public ResponseEntity<ApiResponse<List<PatientBillingNoteDto>>> listInvoiceNotes(
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        var data = service.listInvoiceNotes(patientId, invoiceId);
        var resp = new ApiResponse.Builder<List<PatientBillingNoteDto>>()
                .success(true)
                .message("Invoice notes loaded")
                .data(data)
                .build();
        return ResponseEntity.ok(resp);
    }

    /**
     * Create a new note for an invoice (Save button in "Enter note..." modal)
     */
    @PostMapping("/invoices/{invoiceId}/notes")
    public ResponseEntity<ApiResponse<PatientBillingNoteDto>> createInvoiceNote(
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @RequestBody @Valid PatientBillingNoteDto body) { // Add @Valid if validation annotations are added to DTO

        var data = service.createInvoiceNote(patientId, invoiceId, body);
        var resp = new ApiResponse.Builder<PatientBillingNoteDto>()
                .success(true)
                .message("Invoice note created")
                .data(data)
                .build();
        return ResponseEntity.ok(resp);
    }

    /**
     * Update an existing note for an invoice (Edit icon in notes list)
     */
    @PutMapping("/invoices/{invoiceId}/notes/{noteId}")
    public ResponseEntity<ApiResponse<PatientBillingNoteDto>> updateInvoiceNote(
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @PathVariable Long noteId,
            @RequestBody @Valid PatientBillingNoteDto body) { // Add @Valid if validation annotations are added to DTO

        var data = service.updateInvoiceNote(patientId, invoiceId, noteId, body);
        var resp = new ApiResponse.Builder<PatientBillingNoteDto>()
                .success(true)
                .message("Invoice note updated")
                .data(data)
                .build();
        return ResponseEntity.ok(resp);
    }

    /**
     * Delete a note for an invoice (Trash icon in notes list)
     */
    @DeleteMapping("/invoices/{invoiceId}/notes/{noteId}")
    public ResponseEntity<ApiResponse<Void>> deleteInvoiceNote(
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @PathVariable Long noteId) {

        service.deleteInvoiceNote(patientId, invoiceId, noteId);
        var resp = new ApiResponse.Builder<Void>()
                .success(true)
                .message("Invoice note deleted")
                .build();
        return ResponseEntity.ok(resp);
    }

    /** Patient Deposit: Add deposit and update account credit */
    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<PatientAccountCreditDto>> addPatientDeposit(
            @PathVariable Long patientId,
            @RequestBody PatientDepositRequest request
    ) {
        var data = service.addPatientDeposit(patientId, request);
        return ResponseEntity.ok(ApiResponse.ok("Deposit added and account credit updated", data));
    }

    /** Insurance Deposit: Add insurance deposit and update account credit */
    @PostMapping("/insurance-deposit")
    public ResponseEntity<ApiResponse<PatientAccountCreditDto>> addInsuranceDeposit(
            @PathVariable Long patientId,
            @RequestBody InsuranceDepositRequest request
    ) {
        var data = service.addInsuranceDeposit(patientId, request);
        return ResponseEntity.ok(ApiResponse.ok("Insurance deposit added and account credit updated", data));
    }

    /** Courtesy Credit: Add courtesy credit and update account credit */
    @PostMapping("/courtesy-credit")
    public ResponseEntity<ApiResponse<PatientAccountCreditDto>> addCourtesyCredit(
            @PathVariable Long patientId,
            @RequestBody CourtesyCreditRequest request
    ) {
        var data = service.addCourtesyCredit(patientId, request);
        return ResponseEntity.ok(ApiResponse.ok("Courtesy credit added and account credit updated", data));
    }
}
