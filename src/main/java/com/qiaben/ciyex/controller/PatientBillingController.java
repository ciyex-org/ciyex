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

    /** Transfer INS balance to PT balance */
    @PostMapping("/invoices/{invoiceId}/transfer-outstanding-to-patient")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> transferOutstandingToPatient(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @RequestBody TransferRequest body) {
        PatientInvoiceDto updated = service.transferOutstandingToPatient(orgId, patientId, invoiceId, body.amount);
       return ResponseEntity.ok(ApiResponse.ok("Transfer successful", updated));
    }

    /** Transfer PT balance to INS balance */
    @PostMapping("/invoices/{invoiceId}/transfer-outstanding-to-insurance")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> transferOutstandingToInsurance(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @RequestBody TransferRequest body) {
        PatientInvoiceDto updated = service.transferOutstandingToInsurance(orgId, patientId, invoiceId, body.amount);
        return ResponseEntity.ok(ApiResponse.ok("Transfer successful", updated));
    }

    public static class TransferRequest {
        public Double amount;
    }


    private final PatientBillingService service;
    
        
    /* ===================== Invoices ===================== */

      


    @PostMapping("/invoices/{invoiceId}/backdate")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> backdateInvoice(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @RequestBody BackdateRequest body) {
        var data = service.backdateInvoice(orgId, patientId, invoiceId,
                new PatientBillingService.BackdateRequest(body.date()));
        return ResponseEntity.ok(ApiResponse.ok("Invoice backdated", data));
    }

    @PostMapping("/account-adjustment")
public ResponseEntity<ApiResponse<PatientAccountCreditDto>> accountAdjustment(
        @RequestHeader("x-org-id") Long orgId,
        @PathVariable Long patientId,
        @RequestBody AccountAdjustmentRequest body) {
    var req = new PatientBillingService.AccountAdjustmentRequest(
        body.adjustmentType(),
        body.flatRate(),
        body.specificAmount(),
        body.description(),
        body.includeCourtesyCredit()
    );
    var data = service.accountAdjustment(orgId, patientId, req);
    return ResponseEntity.ok(ApiResponse.ok("Account adjusted", data));

}

    /** Adjust specific invoice with percentage discount and adjustment type */
    @PostMapping("/invoices/{invoiceId}/adjust")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> adjustInvoice(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @RequestBody InvoiceAdjustmentRequest body) {
        var data = service.adjustInvoice(orgId, patientId, invoiceId, body);
        return ResponseEntity.ok(ApiResponse.ok("Invoice adjusted", data));
    }

    @GetMapping("/invoices")
    public ResponseEntity<ApiResponse<List<PatientInvoiceDto>>> listInvoices(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId) {

        var data = service.listInvoices(orgId, patientId);
        return ResponseEntity.ok(ApiResponse.ok("Invoices loaded", data));
    }

    /** +Add Procedure → create invoice (JSON body) */
    @PostMapping("/invoices")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> createInvoiceFromProcedure(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @RequestBody PatientBillingService.CreateInvoiceRequest body) {

        var data = service.createInvoiceFromProcedure(orgId, patientId, body);
        return ResponseEntity.ok(ApiResponse.ok("Invoice created", data));
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
        return ResponseEntity.ok(ApiResponse.ok("Line updated", data));
    }

    /** Percentage adjustment — JSON body { percent } */
    @PostMapping("/invoices/{invoiceId}/adjustment")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> applyInvoicePercentageAdjustment(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @RequestBody PatientBillingService.PercentageAdjustmentRequest body) {

        var data = service.applyInvoicePercentageAdjustment(orgId, patientId, invoiceId, body);
        return ResponseEntity.ok(ApiResponse.ok("Adjustment applied", data));
    }
    /** Delete invoice */
    @DeleteMapping("/invoices/{invoiceId}/delete")
    public ResponseEntity<Void> deleteInvoice(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        service.deleteInvoice(orgId, patientId, invoiceId);
        return ResponseEntity.noContent().build();
    }



    /* ===================== Claims ===================== */
   


    /** Fetch all claims for all patients in the org (for All Claims view) */
    @GetMapping("/all-claims")
    public ResponseEntity<ApiResponse<List<PatientClaimDto>>> listAllClaims(
            @RequestHeader("x-org-id") Long orgId) {
        var data = service.listAllClaims(orgId);
        return ResponseEntity.ok(ApiResponse.ok("All claims loaded", data));
    }

    @GetMapping("/claims")
    public ResponseEntity<ApiResponse<List<PatientClaimDto>>> listAllClaimsForPatient(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId) {
        var data = service.listAllClaimsForPatient(orgId, patientId);
        return ResponseEntity.ok(ApiResponse.ok("Claims loaded", data));
    }

    @GetMapping("/invoices/{invoiceId}/claim")
    public ResponseEntity<ApiResponse<PatientClaimDto>> getActiveClaimForInvoice(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        var data = service.getActiveClaimForInvoice(orgId, patientId, invoiceId);
        return ResponseEntity.ok(ApiResponse.ok("Claim loaded", data));
    }

    @GetMapping("/invoices/{invoiceId}/claims")
    public ResponseEntity<ApiResponse<List<PatientClaimDto>>> listClaimsForInvoice(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        var data = service.listClaimsForInvoice(orgId, patientId, invoiceId);
        return ResponseEntity.ok(ApiResponse.ok("Invoice claims loaded", data));
    }

    @PostMapping("/invoices/{invoiceId}/claim/promote")
    public ResponseEntity<ApiResponse<PatientClaimDto>> promoteClaim(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        var data = service.promoteClaim(orgId, patientId, invoiceId);
        return ResponseEntity.ok(ApiResponse.ok("Claim promoted", data));
    }

    @PostMapping("/invoices/{invoiceId}/claim/send-to-batch")
    public ResponseEntity<ApiResponse<PatientClaimDto>> sendClaimToBatch(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        var data = service.sendClaimToBatch(orgId, patientId, invoiceId);
        return ResponseEntity.ok(ApiResponse.ok("Claim moved to batch", data));
    }

    @PostMapping("/invoices/{invoiceId}/claim/submit")
    public ResponseEntity<ApiResponse<PatientClaimDto>> submitClaim(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        var data = service.submitClaim(orgId, patientId, invoiceId);
        return ResponseEntity.ok(ApiResponse.ok("Claim submitted", data));
    }

    @PostMapping("/invoices/{invoiceId}/claim/close")
    public ResponseEntity<ApiResponse<PatientClaimDto>> closeClaim(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        var data = service.closeClaim(orgId, patientId, invoiceId);
        return ResponseEntity.ok(ApiResponse.ok("Claim closed", data));
    }

    @PostMapping("/invoices/{invoiceId}/claim/void-recreate")
    public ResponseEntity<ApiResponse<PatientClaimDto>> voidAndRecreateClaim(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        var data = service.voidAndRecreateClaim(orgId, patientId, invoiceId);
        return ResponseEntity.ok(ApiResponse.ok("Claim voided and recreated", data));
    }

    /** Compose/Edit core claim fields */
    @PutMapping("/invoices/{invoiceId}/claim")
    public ResponseEntity<ApiResponse<PatientClaimDto>> updateClaim(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @RequestBody PatientBillingService.PatientClaimCoreUpdate body) {

        var data = service.updateClaim(orgId, patientId, invoiceId, body);
        return ResponseEntity.ok(ApiResponse.ok("Claim updated", data));
    }
// --- Attachment & EOB endpoints ---
    @PostMapping(value = "/claims/{claimId}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> uploadClaimAttachment(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long claimId,
            @RequestParam("file") MultipartFile file) throws Exception {
        service.uploadClaimAttachment(orgId, patientId, claimId, file);
        return ResponseEntity.ok(ApiResponse.ok("Attachment uploaded", null));
    }

    @GetMapping("/claims/{claimId}/attachment")
    public ResponseEntity<byte[]> getClaimAttachment(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long claimId) {
        byte[] data = service.getClaimAttachment(orgId, patientId, claimId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @PostMapping(value = "/claims/{claimId}/eob", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> uploadClaimEob(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long claimId,
            @RequestParam("file") MultipartFile file) throws Exception {
        service.uploadClaimEob(orgId, patientId, claimId, file);
        return ResponseEntity.ok(ApiResponse.ok("EOB uploaded", null));
    }

    @GetMapping("/claims/{claimId}/eob")
    public ResponseEntity<byte[]> getClaimEob(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long claimId) {
        byte[] data = service.getClaimEob(orgId, patientId, claimId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
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
        return ResponseEntity.ok(ApiResponse.ok("Insurance payments loaded", data));
    }

    @GetMapping("/invoices/{invoiceId}/insurance-payments")
    public ResponseEntity<ApiResponse<List<PatientInsuranceRemitLineDto>>> listInsurancePaymentsForInvoice(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        var data = service.listInsurancePayments(orgId, patientId, invoiceId, null, null);
        return ResponseEntity.ok(ApiResponse.ok("Insurance payments for invoice loaded", data));
    }

    /** Apply insurance EOB grid */
    @PostMapping("/invoices/{invoiceId}/insurance-payments")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> applyInsurancePayment(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @RequestBody PatientInsurancePaymentRequestDto body) {

        var data = service.applyInsurancePayment(orgId, patientId, invoiceId, body);
        return ResponseEntity.ok(ApiResponse.ok("Insurance payment applied", data));
    }

    /** EDIT insurance remit line */
    @PutMapping("/invoices/{invoiceId}/insurance-payments/{remitId}")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> editInsuranceRemitLine(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @PathVariable Long remitId,
            @RequestBody PatientInsuranceRemitLineDto body) {

        var data = service.editInsuranceRemitLine(orgId, patientId, invoiceId, remitId, body);
        return ResponseEntity.ok(ApiResponse.ok("Insurance payment updated", data));
    }

    /** VOID insurance payment = hard delete remit line */
    @PostMapping("/invoices/{invoiceId}/insurance-payments/{remitId}/void")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> voidInsurancePayment(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @PathVariable Long remitId,
            @RequestBody(required = false) PatientBillingService.VoidReason reason) {

        var data = service.voidInsurancePayment(orgId, patientId, invoiceId, remitId, reason);
        return ResponseEntity.ok(ApiResponse.ok("Insurance payment voided (deleted)", data));
    }

    /** REFUND insurance → increase invoice insurance balance */
    @PostMapping("/invoices/{invoiceId}/insurance-payments/{remitId}/refund")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> refundInsurancePayment(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @PathVariable Long remitId,
            @RequestBody PatientBillingService.RefundRequest body) {

        var data = service.refundInsurancePayment(orgId, patientId, invoiceId, remitId, body);
        return ResponseEntity.ok(ApiResponse.ok("Insurance payment refunded to insurance balance", data));
    }

    /** TRANSFER (insurance balance → patient account credit) */
    @PostMapping("/invoices/{invoiceId}/insurance-payments/{remitId}/transfer-credit-to-patient")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> transferInsuranceCreditToPatient(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @PathVariable Long remitId,
            @RequestBody PatientBillingService.TransferCreditRequest body) {

        var data = service.transferInsuranceCreditToPatient(orgId, patientId, invoiceId, remitId, body);
        return ResponseEntity.ok(ApiResponse.ok("Insurance credit transferred to patient credit", data));
    }

    /* ================= Patient Payment & Credit ================= */

    /** Patient payment → Apply */
    @PostMapping("/invoices/{invoiceId}/patient-payments")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> applyPatientPayment(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @RequestBody PatientPatientPaymentRequestDto body) {

        var data = service.applyPatientPayment(orgId, patientId, invoiceId, body);
        return ResponseEntity.ok(ApiResponse.ok("Patient payment applied", data));
    }

    /** GET all patient payment allocations for a patient */
    @GetMapping("/patient-payments")
    public ResponseEntity<ApiResponse<List<PatientPatientPaymentAllocationDto>>> getAllPatientPayments(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId) {

        var data = service.getAllPatientPayments(orgId, patientId);
        return ResponseEntity.ok(ApiResponse.ok("All patient payments fetched", data));
    }

    /** GET allocations by invoice */
    @GetMapping("/invoices/{invoiceId}/patient-payments")
    public ResponseEntity<ApiResponse<List<PatientPatientPaymentAllocationDto>>> getPaymentsByInvoice(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        var data = service.getPatientPaymentsByInvoice(orgId, patientId, invoiceId);
        return ResponseEntity.ok(ApiResponse.ok("Patient payments for invoice fetched", data));
    }

    /** EDIT patient payment */
    @PutMapping("/invoices/{invoiceId}/patient-payments/{paymentId}")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> editPatientPayment(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @PathVariable Long paymentId,
            @RequestBody PatientPaymentDto body) {

        var data = service.editPatientPayment(orgId, patientId, invoiceId, paymentId, body);
        return ResponseEntity.ok(ApiResponse.ok("Patient payment updated", data));
    }

    /** VOID patient payment = delete payment + allocations */
    @PostMapping("/invoices/{invoiceId}/patient-payments/{paymentId}/void")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> voidPatientPayment(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @PathVariable Long paymentId,
            @RequestBody(required = false) PatientBillingService.VoidReason reason) {

        var data = service.voidPatientPayment(orgId, patientId, invoiceId, paymentId, reason);
        return ResponseEntity.ok(ApiResponse.ok("Patient payment voided (deleted)", data));
    }

    /** REFUND patient payment → move to account credit */
    @PostMapping("/invoices/{invoiceId}/patient-payments/{paymentId}/refund")
    public ResponseEntity<ApiResponse<PatientInvoiceDto>> refundPatientPayment(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @PathVariable Long paymentId,
            @RequestBody PatientBillingService.RefundRequest body) {

        var data = service.refundPatientPayment(orgId, patientId, invoiceId, paymentId, body);
        return ResponseEntity.ok(ApiResponse.ok("Patient payment refunded to patient credit", data));
    }

    /** Transfer patient credit between patients */
    @PostMapping("/patients/{fromPatientId}/transfer-credit/{toPatientId}")
    public ResponseEntity<ApiResponse<PatientAccountCreditDto[]>> transferPatientCreditToPatient(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long fromPatientId,
            @PathVariable Long toPatientId,
            @RequestBody PatientBillingService.TransferCreditRequest body) {

        var data = service.transferPatientCreditToPatient(orgId, fromPatientId, toPatientId, body.amount());
        return ResponseEntity.ok(ApiResponse.ok("Patient credit transferred", data));
    }

    /* ================= Account credit ================= */

    @GetMapping("/account-credit")
    public ResponseEntity<ApiResponse<PatientAccountCreditDto>> getAccountCredit(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId) {

        var data = service.getAccountCredit(orgId, patientId);
        return ResponseEntity.ok(ApiResponse.ok("Account credit loaded", data));
    }

    /** Apply account credit — JSON body { amount } */
    @PostMapping("/account-credit/apply")
    public ResponseEntity<ApiResponse<PatientAccountCreditDto>> applyAccountCredit(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @RequestBody PatientBillingService.ApplyCreditRequest body) {

        var data = service.applyAccountCredit(orgId, patientId, body);
        return ResponseEntity.ok(ApiResponse.ok("Credit applied", data));
    }



    /**
     * List notes for a specific invoice (+ symbol in UI triggers this)
     */
    @GetMapping("/invoices/{invoiceId}/notes")
    public ResponseEntity<ApiResponse<List<PatientBillingNoteDto>>> listInvoiceNotes(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {

        var data = service.listInvoiceNotes(orgId, patientId, invoiceId);
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
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @RequestBody @Valid PatientBillingNoteDto body) { // Add @Valid if validation annotations are added to DTO

        var data = service.createInvoiceNote(orgId, patientId, invoiceId, body);
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
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @PathVariable Long noteId,
            @RequestBody @Valid PatientBillingNoteDto body) { // Add @Valid if validation annotations are added to DTO

        var data = service.updateInvoiceNote(orgId, patientId, invoiceId, noteId, body);
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
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId,
            @PathVariable Long noteId) {

        service.deleteInvoiceNote(orgId, patientId, invoiceId, noteId);
        var resp = new ApiResponse.Builder<Void>()
                .success(true)
                .message("Invoice note deleted")
                .build();
        return ResponseEntity.ok(resp);
    }

    /** Patient Deposit: Add deposit and update account credit */
    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<PatientAccountCreditDto>> addPatientDeposit(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @RequestBody PatientDepositRequest request
    ) {
        var data = service.addPatientDeposit(orgId, patientId, request);
        return ResponseEntity.ok(ApiResponse.ok("Deposit added and account credit updated", data));
    }

    /** Insurance Deposit: Add insurance deposit and update account credit */
    @PostMapping("/insurance-deposit")
    public ResponseEntity<ApiResponse<PatientAccountCreditDto>> addInsuranceDeposit(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @RequestBody InsuranceDepositRequest request
    ) {
        var data = service.addInsuranceDeposit(orgId, patientId, request);
        return ResponseEntity.ok(ApiResponse.ok("Insurance deposit added and account credit updated", data));
    }

    /** Courtesy Credit: Add courtesy credit and update account credit */
    @PostMapping("/courtesy-credit")
    public ResponseEntity<ApiResponse<PatientAccountCreditDto>> addCourtesyCredit(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @RequestBody CourtesyCreditRequest request
    ) {
        var data = service.addCourtesyCredit(orgId, patientId, request);
        return ResponseEntity.ok(ApiResponse.ok("Courtesy credit added and account credit updated", data));
    }
    /** 
     * Print unified statement detail for a single invoice
     */
    @PostMapping("/invoices/{invoiceId}/statement-detail")
    public ResponseEntity<ApiResponse<StatementDetailDto>> printInvoiceStatementDetail(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long invoiceId) {
        StatementDetailDto dto = service.getInvoiceStatementDetail(orgId, patientId, invoiceId);
        return ResponseEntity.ok(ApiResponse.ok("Invoice statement detail loaded", dto));
    }

    /**
     * Print unified statement detail for all invoices/payments for a patient
     */
    @PostMapping("/statement-detail")
    public ResponseEntity<ApiResponse<StatementDetailDto>> printPatientStatementDetail(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId) {
        StatementDetailDto dto = service.getPatientStatementDetail(orgId, patientId);
        return ResponseEntity.ok(ApiResponse.ok("Patient statement detail loaded", dto));
    }

        /** Lock claim (after lock, claim cannot be edited) */
        @PostMapping("/claims/{claimId}/lock")
        public ResponseEntity<ApiResponse<PatientClaimDto>> lockClaim(
                @RequestHeader("x-org-id") Long orgId,
                @PathVariable Long patientId,
                @PathVariable Long claimId) {
            service.lockClaim(orgId, patientId, claimId);
            PatientClaimDto dto = service.toClaimDto(service.getClaimOrThrow(orgId, patientId, claimId));
            return ResponseEntity.ok(ApiResponse.ok("Claim locked", dto));
        }

   
    /**
     * Change claim status (accepts JSON body, not request parameter)
     */
    @PostMapping("/claims/{claimId}/status")
    public ResponseEntity<ApiResponse<PatientClaimDto>> changeClaimStatus(
            @RequestHeader("x-org-id") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long claimId,
            @RequestBody ClaimStatusUpdateDto dto
    ) {
        service.changeClaimStatus(orgId, patientId, claimId, dto);
        PatientClaimDto response = service.toClaimDto(service.getClaimOrThrow(orgId, patientId, claimId));
        return ResponseEntity.ok(ApiResponse.ok("Claim status updated", response));
    }


    /** Submit claim attachment */
        @PostMapping(value = "/claims/{claimId}/submit-attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<ApiResponse<PatientClaimDto>> submitClaimAttachment(
                @RequestHeader("x-org-id") Long orgId,
                @PathVariable Long patientId,
                @PathVariable Long claimId,
                @RequestParam("file") MultipartFile file) throws Exception {
            service.submitClaimAttachment(orgId, patientId, claimId, file);
            PatientClaimDto dto = service.toClaimDto(service.getClaimOrThrow(orgId, patientId, claimId));
            return ResponseEntity.ok(ApiResponse.ok("Claim attachment submitted", dto));
        }

    
      
}