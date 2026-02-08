package org.ciyex.ehr.controller;

import org.ciyex.ehr.dto.*;
import org.ciyex.ehr.service.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;



@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/patient-billing/{patientId}")
public class PatientBillingController {

  private final PatientInvoiceService invoiceService;
  private final PatientClaimService claimService;
  private final PatientInsurancePaymentService insurancePaymentService;
  private final PatientPaymentService paymentService;
  private final PatientCreditService creditService;
  private final PatientBillingNoteService noteService;
  private final PatientDepositService depositService;
  private final PatientBillingPrintService service;



   @GetMapping("/statement")
   public ResponseEntity<ApiResponse<PatientStatementDto>> getPatientStatement(
           @PathVariable Long patientId) {
       try {
           PatientStatementDto dto = service.getPatientStatement(patientId);
           return ResponseEntity.ok(ApiResponse.ok("Statement loaded", dto));
       } catch (IllegalArgumentException ex) {
           return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                   .body(ApiResponse.error(ex.getMessage()));
       }
   }



  /** Transfer INS balance to PT balance */
  @PostMapping("/invoices/{invoiceId}/transfer-outstanding-to-patient")
  public ResponseEntity<ApiResponse<PatientInvoiceDto>> transferOutstandingToPatient(

          @PathVariable Long patientId,
          @PathVariable Long invoiceId,
          @RequestBody TransferRequest body) {
      try {
          PatientInvoiceDto updated = invoiceService.transferOutstandingToPatient(patientId, invoiceId, body.amount);
          return ResponseEntity.ok(ApiResponse.ok("Transfer successful", updated));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                  .body(ApiResponse.error(ex.getMessage()));
      }
  }

  /** Transfer PT balance to INS balance */
  @PostMapping("/invoices/{invoiceId}/transfer-outstanding-to-insurance")
  public ResponseEntity<ApiResponse<PatientInvoiceDto>> transferOutstandingToInsurance(

          @PathVariable Long patientId,
          @PathVariable Long invoiceId,
          @RequestBody TransferRequest body) {
      try {
          PatientInvoiceDto updated = invoiceService.transferOutstandingToInsurance(patientId, invoiceId, body.amount);
          return ResponseEntity.ok(ApiResponse.ok("Transfer successful", updated));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                  .body(ApiResponse.error(ex.getMessage()));
      }
  }

  public static class TransferRequest {
      public Double amount;
  }


  /* ===================== Invoices ===================== */




  @PostMapping("/invoices/{invoiceId}/backdate")
  public ResponseEntity<ApiResponse<PatientInvoiceDto>> backdateInvoice(

          @PathVariable Long patientId,
          @PathVariable Long invoiceId,
          @RequestBody BackdateRequest body) {
      try {
          var data = invoiceService.backdateInvoice(patientId, invoiceId,
                  new PatientInvoiceService.BackdateRequest(body.date()));
          return ResponseEntity.ok(ApiResponse.ok("Invoice backdated", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                  .body(ApiResponse.error(ex.getMessage()));
      }
  }

  @PostMapping("/account-adjustment")
  public ResponseEntity<ApiResponse<PatientAccountCreditDto>> accountAdjustment(

          @PathVariable Long patientId,
          @RequestBody AccountAdjustmentRequest body) {
      try {
          var req = new PatientInvoiceService.AccountAdjustmentRequest(
                  body.adjustmentType(),
                  body.flatRate(),
                  body.specificAmount(),
                  body.description(),
                  body.includeCourtesyCredit()
          );
          var data = invoiceService.accountAdjustment(patientId, req);
          return ResponseEntity.ok(ApiResponse.ok("Account adjusted", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                  .body(ApiResponse.error(ex.getMessage()));
      }
  }

  /** Adjust specific invoice with percentage discount and adjustment type */
  @PostMapping("/invoices/{invoiceId}/adjust")
  public ResponseEntity<ApiResponse<PatientInvoiceDto>> adjustInvoice(

          @PathVariable Long patientId,
          @PathVariable Long invoiceId,
          @RequestBody InvoiceAdjustmentRequest body) {
      try {
          var data = invoiceService.adjustInvoice(patientId, invoiceId, body);
          return ResponseEntity.ok(ApiResponse.ok("Invoice adjusted", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                  .body(ApiResponse.error(ex.getMessage()));
      }
  }

  @GetMapping("/invoices")
  public ResponseEntity<ApiResponse<List<PatientInvoiceDto>>> listInvoices(

          @PathVariable Long patientId) {
      try {
          var data = invoiceService.listInvoices(patientId);
          return ResponseEntity.ok(ApiResponse.ok("Invoices loaded", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error listing invoices for patient {}", patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error loading invoices: " + ex.getMessage()));
      }
  }

  /** +Add Procedure → create invoice (JSON body) */
  @PostMapping("/invoices")
  public ResponseEntity<ApiResponse<PatientInvoiceDto>> createInvoiceFromProcedure(

          @PathVariable Long patientId,
          @RequestBody PatientInvoiceService.CreateInvoiceRequest body) {
      try {
          if (body == null) {
              return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                      .body(ApiResponse.error("Request body is required"));
          }
          var data = invoiceService.createInvoiceFromProcedure(patientId, body);
          return ResponseEntity.ok(ApiResponse.ok("Invoice created", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error creating invoice for patient {}", patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error creating invoice: " + ex.getMessage()));
      }
  }



  /** Update invoice from procedure (JSON body) */
  @PutMapping("/invoices/{invoiceId}")
  public ResponseEntity<ApiResponse<PatientInvoiceDto>> updateInvoiceFromProcedure(
          @PathVariable Long patientId,
          @PathVariable Long invoiceId,
          @RequestBody PatientInvoiceService.UpdateInvoiceRequest body) {
      try {
          if (body == null) {
              return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                      .body(ApiResponse.error("Request body is required"));
          }
          var data = invoiceService.updateInvoiceFromProcedure(patientId, invoiceId, body);
          return ResponseEntity.ok(ApiResponse.ok("Invoice updated", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error updating invoice {} for patient {}", invoiceId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error updating invoice: " + ex.getMessage()));
      }
  }

  /** Delete invoice */
  @DeleteMapping("/invoices/{invoiceId}")
  public ResponseEntity<ApiResponse<Void>> deleteInvoice(
          @PathVariable Long patientId,
          @PathVariable Long invoiceId) {
      try {
          invoiceService.deleteInvoice(patientId, invoiceId);
          return ResponseEntity.ok(ApiResponse.ok("Invoice deleted", null));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error deleting invoice {} for patient {}", invoiceId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error deleting invoice: " + ex.getMessage()));
      }
  }

  /** Get invoice lines for a specific invoice */
  @GetMapping("/invoices/{invoiceId}/lines")
  public ResponseEntity<ApiResponse<List<PatientInvoiceLineDto>>> getInvoiceLines(
          @PathVariable Long patientId,
          @PathVariable Long invoiceId) {
      try {
          var data = invoiceService.getInvoiceLines(patientId, invoiceId);
          return ResponseEntity.ok(ApiResponse.ok("Invoice lines loaded", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error getting invoice lines for invoice {} patient {}", invoiceId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error loading invoice lines: " + ex.getMessage()));
      }
  }

  /** Edit line amount (re-estimate) — JSON body { newCharge } */
  @PutMapping("/invoices/{invoiceId}/lines/{lineId}")
  public ResponseEntity<ApiResponse<PatientInvoiceDto>> updateInvoiceLineAmount(

          @PathVariable Long patientId,
          @PathVariable Long invoiceId,
          @PathVariable Long lineId,
          @RequestBody PatientInvoiceService.UpdateLineAmountRequest body) {
      try {
          if (body == null || body.newCharge() == null) {
              return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                      .body(ApiResponse.error("New charge amount is required"));
          }
          var data = invoiceService.updateInvoiceLineAmount(patientId, invoiceId, lineId, body);
          return ResponseEntity.ok(ApiResponse.ok("Line updated", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error updating line {} for invoice {} patient {}", lineId, invoiceId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error updating invoice line: " + ex.getMessage()));
      }
  }

  /** Percentage adjustment — JSON body { percent } */
  @PostMapping("/invoices/{invoiceId}/adjustment")
  public ResponseEntity<ApiResponse<PatientInvoiceDto>> applyInvoicePercentageAdjustment(

          @PathVariable Long patientId,
          @PathVariable Long invoiceId,
          @RequestBody PatientInvoiceService.PercentageAdjustmentRequest body) {
      try {
          if (body == null) {
              return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                      .body(ApiResponse.error("Request body with percentage is required"));
          }
          var data = invoiceService.applyInvoicePercentageAdjustment(patientId, invoiceId, body);
          return ResponseEntity.ok(ApiResponse.ok("Adjustment applied", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error applying adjustment to invoice {} patient {}", invoiceId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error applying adjustment: " + ex.getMessage()));
      }
  }




  /* ===================== Claims ===================== */



  @GetMapping("/claims")
  public ResponseEntity<ApiResponse<List<PatientClaimDto>>> listAllClaimsForPatient(

          @PathVariable Long patientId) {
      try {
          var data = claimService.listAllClaimsForPatient(patientId);
          return ResponseEntity.ok(ApiResponse.ok("Claims loaded", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error listing claims for patient {}", patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error loading claims: " + ex.getMessage()));
      }
  }

  @GetMapping("/invoices/{invoiceId}/claim")
  public ResponseEntity<ApiResponse<PatientClaimDto>> getActiveClaimForInvoice(

          @PathVariable Long patientId,
          @PathVariable Long invoiceId) {
      try {
          var data = claimService.getActiveClaimForInvoice(patientId, invoiceId);
          return ResponseEntity.ok(ApiResponse.ok("Claim loaded", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error getting claim for invoice {} patient {}", invoiceId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error loading claim: " + ex.getMessage()));
      }
  }

  @GetMapping("/invoices/{invoiceId}/claims")
  public ResponseEntity<ApiResponse<List<PatientClaimDto>>> listClaimsForInvoice(

          @PathVariable Long patientId,
          @PathVariable Long invoiceId) {
      try {
          var data = claimService.listClaimsForInvoice(patientId, invoiceId);
          return ResponseEntity.ok(ApiResponse.ok("Invoice claims loaded", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error listing claims for invoice {} patient {}", invoiceId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error loading claims: " + ex.getMessage()));
      }
  }

  @PostMapping("/invoices/{invoiceId}/claim/promote")
  public ResponseEntity<ApiResponse<PatientClaimDto>> promoteClaim(

          @PathVariable Long patientId,
          @PathVariable Long invoiceId) {
      try {
          var data = claimService.promoteClaim(patientId, invoiceId);
          return ResponseEntity.ok(ApiResponse.ok("Claim promoted", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error promoting claim for invoice {} patient {}", invoiceId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error promoting claim: " + ex.getMessage()));
      }
  }

  @PostMapping("/invoices/{invoiceId}/claim/send-to-batch")
  public ResponseEntity<ApiResponse<PatientClaimDto>> sendClaimToBatch(

          @PathVariable Long patientId,
          @PathVariable Long invoiceId) {
      try {
          var data = claimService.sendClaimToBatch(patientId, invoiceId);
          return ResponseEntity.ok(ApiResponse.ok("Claim moved to batch", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error sending claim to batch for invoice {} patient {}", invoiceId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error sending claim to batch: " + ex.getMessage()));
      }
  }

  @PostMapping("/invoices/{invoiceId}/claim/submit")
  public ResponseEntity<ApiResponse<PatientClaimDto>> submitClaim(

          @PathVariable Long patientId,
          @PathVariable Long invoiceId) {
      try {
          var data = claimService.submitClaim(patientId, invoiceId);
          return ResponseEntity.ok(ApiResponse.ok("Claim submitted", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error submitting claim for invoice {} patient {}", invoiceId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error submitting claim: " + ex.getMessage()));
      }
  }

  @PostMapping("/invoices/{invoiceId}/claim/close")
  public ResponseEntity<ApiResponse<PatientClaimDto>> closeClaim(

          @PathVariable Long patientId,
          @PathVariable Long invoiceId) {
      try {
          var data = claimService.closeClaim(patientId, invoiceId);
          return ResponseEntity.ok(ApiResponse.ok("Claim closed", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error closing claim for invoice {} patient {}", invoiceId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error closing claim: " + ex.getMessage()));
      }
  }

  @PostMapping("/invoices/{invoiceId}/claim/void-recreate")
  public ResponseEntity<ApiResponse<PatientClaimDto>> voidAndRecreateClaim(

          @PathVariable Long patientId,
          @PathVariable Long invoiceId) {
      try {
          var data = claimService.voidAndRecreateClaim(patientId, invoiceId);
          return ResponseEntity.ok(ApiResponse.ok("Claim voided and recreated", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error voiding/recreating claim for invoice {} patient {}", invoiceId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error voiding and recreating claim: " + ex.getMessage()));
      }
  }

  /** Compose/Edit core claim fields */
  @PutMapping("/invoices/{invoiceId}/claim")
  public ResponseEntity<ApiResponse<PatientClaimDto>> updateClaim(

          @PathVariable Long patientId,
          @PathVariable Long invoiceId,
          @RequestBody PatientClaimService.PatientClaimCoreUpdate body) {
      try {
          if (body == null) {
              return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                      .body(ApiResponse.error("Request body is required"));
          }
          var data = claimService.updateClaim(patientId, invoiceId, body);
          return ResponseEntity.ok(ApiResponse.ok("Claim updated", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error updating claim for invoice {} patient {}", invoiceId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error updating claim: " + ex.getMessage()));
      }
  }

  /** Get claim line details (DOS, code, description, provider, total submitted amount) */
  @GetMapping("/claims/{claimId}/lines")
  public ResponseEntity<ApiResponse<List<ClaimLineDetailDto>>> getClaimLineDetails(
          @PathVariable Long patientId,
          @PathVariable Long claimId) {
      try {
          var data = claimService.getClaimLineDetails(claimId);
          return ResponseEntity.ok(ApiResponse.ok("Claim lines loaded", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error getting claim lines for claim {} patient {}", claimId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error loading claim lines: " + ex.getMessage()));
      }
  }

  // --- Attachment & EOB endpoints ---
  @PostMapping(value = "/claims/{claimId}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<Void>> uploadClaimAttachment(

          @PathVariable Long patientId,
          @PathVariable Long claimId,
          @RequestParam("file") MultipartFile file) {
      try {
          claimService.submitClaimAttachment(patientId, claimId, file);
          return ResponseEntity.ok(ApiResponse.ok("Attachment uploaded", null));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error uploading attachment: " + ex.getMessage()));
      }
  }

  @GetMapping("/claims/{claimId}/attachment")
  public ResponseEntity<byte[]> getClaimAttachment(

          @PathVariable Long patientId,
          @PathVariable Long claimId) {
      try {
          byte[] data = claimService.getClaimAttachment(patientId, claimId);
          return ResponseEntity.ok()
                  .contentType(MediaType.APPLICATION_OCTET_STREAM)
                  .body(data);
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                  .body(null);
      }
  }

  @PostMapping(value = "/claims/{claimId}/eob", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<Void>> uploadClaimEob(

          @PathVariable Long patientId,
          @PathVariable Long claimId,
          @RequestParam("file") MultipartFile file) {
      try {
          claimService.uploadClaimEob(patientId, claimId, file);
          return ResponseEntity.ok(ApiResponse.ok("EOB uploaded", null));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error uploading EOB: " + ex.getMessage()));
      }
  }

  @GetMapping("/claims/{claimId}/eob")
  public ResponseEntity<byte[]> getClaimEob(

          @PathVariable Long patientId,
          @PathVariable Long claimId) {
      try {
          byte[] data = claimService.getClaimEob(patientId, claimId);
          return ResponseEntity.ok()
                  .contentType(MediaType.APPLICATION_OCTET_STREAM)
                  .body(data);
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                  .body(null);
      }
  }


  /* ================= Insurance Payment ================= */

  @GetMapping("/insurance-payments")
  public ResponseEntity<ApiResponse<List<PatientInsuranceRemitLineDto>>> listInsurancePayments(

          @PathVariable Long patientId,
          @RequestParam(required = false) Long invoiceId,
          @RequestParam(required = false) Long claimId,
          @RequestParam(required = false) Long insuranceId) {
      try {
          var data = insurancePaymentService.listInsurancePayments(patientId, invoiceId, claimId, insuranceId);
          return ResponseEntity.ok(ApiResponse.ok("Insurance payments loaded", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error listing insurance payments for patient {}", patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error loading insurance payments: " + ex.getMessage()));
      }
  }

  @GetMapping("/invoices/{invoiceId}/insurance-payments")
  public ResponseEntity<ApiResponse<List<PatientInsuranceRemitLineDto>>> listInsurancePaymentsForInvoice(

          @PathVariable Long patientId,
          @PathVariable Long invoiceId) {
      try {
          var data = insurancePaymentService.listInsurancePayments(patientId, invoiceId, null, null);
          return ResponseEntity.ok(ApiResponse.ok("Insurance payments for invoice loaded", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error listing insurance payments for invoice {} patient {}", invoiceId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error loading insurance payments: " + ex.getMessage()));
      }
  }

  /** Apply insurance EOB grid */
  @PostMapping("/invoices/{invoiceId}/insurance-payments")
  public ResponseEntity<ApiResponse<InsurancePaymentResponseDto>> applyInsurancePayment(

          @PathVariable Long patientId,
          @PathVariable Long invoiceId,
          @RequestBody PatientInsurancePaymentRequestDto body) {
      try {
          if (body == null || body.lines() == null || body.lines().isEmpty()) {
              return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                      .body(ApiResponse.error("Request body with payment lines is required"));
          }
          var data = insurancePaymentService.applyInsurancePayment(patientId, invoiceId, body);
          return ResponseEntity.ok(ApiResponse.ok("Insurance payment applied", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error applying insurance payment for invoice {} patient {}", invoiceId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error applying insurance payment: " + ex.getMessage()));
      }
  }

  /** EDIT insurance remit line */
  @PutMapping("/invoices/{invoiceId}/insurance-payments/{remitId}")
  public ResponseEntity<ApiResponse<PatientInvoiceDto>> editInsuranceRemitLine(

          @PathVariable Long patientId,
          @PathVariable Long invoiceId,
          @PathVariable Long remitId,
          @RequestBody PatientInsuranceRemitLineDto body) {
      try {
          if (body == null) {
              return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                      .body(ApiResponse.error("Request body is required"));
          }
          var data = insurancePaymentService.editInsuranceRemitLine(patientId, invoiceId, remitId, body);
          return ResponseEntity.ok(ApiResponse.ok("Insurance payment updated", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error editing insurance remit {} for invoice {} patient {}", remitId, invoiceId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error updating insurance payment: " + ex.getMessage()));
      }
  }

  /** VOID insurance payment = hard delete remit line */
  @PostMapping("/invoices/{invoiceId}/insurance-payments/{remitId}/void")
  public ResponseEntity<ApiResponse<PatientInvoiceDto>> voidInsurancePayment(

          @PathVariable Long patientId,
          @PathVariable Long invoiceId,
          @PathVariable Long remitId,
          @RequestBody(required = false) PatientInsurancePaymentService.VoidReason reason) {
      try {
          var data = insurancePaymentService.voidInsurancePayment(patientId, invoiceId, remitId, reason);
          return ResponseEntity.ok(ApiResponse.ok("Insurance payment voided (deleted)", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error voiding insurance payment {} for invoice {} patient {}", remitId, invoiceId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error voiding insurance payment: " + ex.getMessage()));
      }
  }

  /** REFUND insurance → increase invoice insurance balance */
  @PostMapping("/invoices/{invoiceId}/insurance-payments/{remitId}/refund")
  public ResponseEntity<ApiResponse<PatientInvoiceDto>> refundInsurancePayment(

          @PathVariable Long patientId,
          @PathVariable Long invoiceId,
          @PathVariable Long remitId,
          @RequestBody PatientInsurancePaymentService.RefundRequest body) {
      try {
          if (body == null || body.amount() == null) {
              return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                      .body(ApiResponse.error("Refund amount is required"));
          }
          var data = insurancePaymentService.refundInsurancePayment(patientId, invoiceId, remitId, body);
          return ResponseEntity.ok(ApiResponse.ok("Insurance payment refunded to insurance balance", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (IllegalStateException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error refunding insurance payment {} for invoice {} patient {}", remitId, invoiceId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error refunding insurance payment: " + ex.getMessage()));
      }
  }

  /** TRANSFER (insurance balance → patient account credit) */
  @PostMapping("/invoices/{invoiceId}/insurance-payments/{remitId}/transfer-credit-to-patient")
  public ResponseEntity<ApiResponse<PatientInvoiceDto>> transferInsuranceCreditToPatient(

          @PathVariable Long patientId,
          @PathVariable Long invoiceId,
          @PathVariable Long remitId,
          @RequestBody PatientInsurancePaymentService.TransferCreditRequest body) {
      try {
          if (body == null || body.amount() == null) {
              return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                      .body(ApiResponse.error("Transfer amount is required"));
          }
          var data = insurancePaymentService.transferInsuranceCreditToPatient(patientId, invoiceId, remitId, body);
          return ResponseEntity.ok(ApiResponse.ok("Insurance overpayment adjusted", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (IllegalStateException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error transferring insurance credit for remit {} invoice {} patient {}", remitId, invoiceId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error transferring insurance credit: " + ex.getMessage()));
      }
  }

  /** Get detailed insurance payment information */
  @GetMapping("/invoices/{invoiceId}/insurance-payments/{remitId}/details")
  public ResponseEntity<ApiResponse<InsurancePaymentDetailDto>> getInsurancePaymentDetails(
          @PathVariable Long patientId,
          @PathVariable Long invoiceId,
          @PathVariable Long remitId) {
      try {
          log.info("Getting insurance payment details for patient {} invoice {} remit {}", patientId, invoiceId, remitId);
          InsurancePaymentDetailDto details = insurancePaymentService.getInsurancePaymentDetails(patientId, invoiceId, remitId);
          return ResponseEntity.ok(ApiResponse.ok("Insurance payment details retrieved", details));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (RuntimeException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error getting insurance payment details for remit {} invoice {} patient {}", remitId, invoiceId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error retrieving insurance payment details: " + ex.getMessage()));
      }
  }

  /* ================= Patient Payment & Credit ================= */

  /** Patient payment → Apply */
  @PostMapping("/invoices/{invoiceId}/patient-payments")
  public ResponseEntity<ApiResponse<PatientPaymentResponseDto>> applyPatientPayment(

          @PathVariable Long patientId,
          @PathVariable Long invoiceId,
          @RequestBody PatientPatientPaymentRequestDto body) {
      try {
          if (body == null || body.allocations() == null || body.allocations().isEmpty()) {
              return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                      .body(ApiResponse.error("Request body with payment allocations is required"));
          }
          if (body.paymentMethod() == null || body.paymentMethod().isEmpty()) {
              return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                      .body(ApiResponse.error("Payment method is required"));
          }
          var data = paymentService.applyPatientPayment(patientId, invoiceId, body);
          return ResponseEntity.ok(ApiResponse.ok("Patient payment applied", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (RuntimeException ex) {
          if (ex.getMessage() != null && ex.getMessage().contains("not found")) {
              return ResponseEntity.status(HttpStatus.NOT_FOUND)
                      .body(ApiResponse.error(ex.getMessage()));
          }
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error applying patient payment for invoice {} patient {}", invoiceId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error applying patient payment: " + ex.getMessage()));
      }
  }

  /** Get detailed patient payment information */
  @GetMapping("/invoices/{invoiceId}/patient-payments/{paymentId}/details")
  public ResponseEntity<ApiResponse<PatientPaymentDetailDto>> getPatientPaymentDetails(
          @PathVariable Long patientId,
          @PathVariable Long invoiceId,
          @PathVariable Long paymentId) {
      try {
          log.info("Getting patient payment details for patient {} invoice {} payment {}", patientId, invoiceId, paymentId);
          PatientPaymentDetailDto details = paymentService.getPatientPaymentDetails(patientId, invoiceId, paymentId);
          return ResponseEntity.ok(ApiResponse.ok("Patient payment details retrieved", details));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (RuntimeException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error getting patient payment details for payment {} invoice {} patient {}", paymentId, invoiceId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error retrieving patient payment details: " + ex.getMessage()));
      }
  }

  /** GET all patient payment allocations for a patient */
  @GetMapping("/patient-payments")
  public ResponseEntity<ApiResponse<List<PatientPatientPaymentAllocationDto>>> getAllPatientPayments(

          @PathVariable Long patientId) {
      try {
          var data = paymentService.getAllPatientPayments(patientId);
          return ResponseEntity.ok(ApiResponse.ok("All patient payments fetched", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error getting all patient payments for patient {}", patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error fetching patient payments: " + ex.getMessage()));
      }
  }

  /** GET allocations by invoice */
  @GetMapping("/invoices/{invoiceId}/patient-payments")
  public ResponseEntity<ApiResponse<List<PatientPatientPaymentAllocationDto>>> getPaymentsByInvoice(

          @PathVariable Long patientId,
          @PathVariable Long invoiceId) {
      try {
          var data = paymentService.getPatientPaymentsByInvoice(patientId, invoiceId);
          return ResponseEntity.ok(ApiResponse.ok("Patient payments for invoice fetched", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error getting patient payments for invoice {} patient {}", invoiceId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error fetching patient payments: " + ex.getMessage()));
      }
  }

  /** EDIT patient payment */
  @PutMapping("/invoices/{invoiceId}/patient-payments/{paymentId}")
  public ResponseEntity<ApiResponse<PatientInvoiceDto>> editPatientPayment(

          @PathVariable Long patientId,
          @PathVariable Long invoiceId,
          @PathVariable Long paymentId,
          @RequestBody PatientPaymentDto body) {
      try {
          if (body == null) {
              return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                      .body(ApiResponse.error("Request body is required"));
          }
          var data = paymentService.editPatientPayment(patientId, invoiceId, paymentId, body);
          return ResponseEntity.ok(ApiResponse.ok("Patient payment updated", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error editing patient payment {} for invoice {} patient {}", paymentId, invoiceId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error updating patient payment: " + ex.getMessage()));
      }
  }

  /** VOID patient payment = delete payment + allocations */
  @PostMapping("/invoices/{invoiceId}/patient-payments/{paymentId}/void")
  public ResponseEntity<ApiResponse<PatientInvoiceDto>> voidPatientPayment(

          @PathVariable Long patientId,
          @PathVariable Long invoiceId,
          @PathVariable Long paymentId,
          @RequestBody(required = false) PatientPaymentService.VoidReason reason) {
      try {
          var data = paymentService.voidPatientPayment(patientId, invoiceId, paymentId, reason);
          return ResponseEntity.ok(ApiResponse.ok("Patient payment voided (deleted)", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error voiding patient payment {} for invoice {} patient {}", paymentId, invoiceId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error voiding patient payment: " + ex.getMessage()));
      }
  }

  /** REFUND patient payment → move to account credit */
  @PostMapping("/invoices/{invoiceId}/patient-payments/{paymentId}/refund")
  public ResponseEntity<ApiResponse<PatientInvoiceDto>> refundPatientPayment(

          @PathVariable Long patientId,
          @PathVariable Long invoiceId,
          @PathVariable Long paymentId,
          @RequestBody PatientPaymentService.RefundRequest body) {
      try {
          if (body == null || body.amount() == null) {
              return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                      .body(ApiResponse.error("Refund amount is required"));
          }
          var data = paymentService.refundPatientPayment(patientId, invoiceId, paymentId, body);
          return ResponseEntity.ok(ApiResponse.ok("Patient payment refunded to patient credit", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error refunding patient payment {} for invoice {} patient {}", paymentId, invoiceId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error refunding patient payment: " + ex.getMessage()));
      }
  }

  /** Transfer patient credit between patients */
  @PostMapping("/patients/{fromPatientId}/{toPatientId}/transfer-credit")
  public ResponseEntity<ApiResponse<PatientAccountCreditDto[]>> transferPatientCreditToPatient(

          @PathVariable Long fromPatientId,
          @PathVariable Long toPatientId,
          @RequestBody PatientPaymentService.TransferCreditRequest body) {
      try {
          if (body == null || body.amount() == null) {
              return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                      .body(ApiResponse.error("Transfer amount is required"));
          }
          var data = paymentService.transferPatientCreditToPatient(fromPatientId, toPatientId, body.amount());
          return ResponseEntity.ok(ApiResponse.ok("Patient credit transferred", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error transferring credit from patient {} to patient {}", fromPatientId, toPatientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error transferring patient credit: " + ex.getMessage()));
      }
  }

  /* ================= Account credit ================= */

  @GetMapping("/account-credit")
  public ResponseEntity<ApiResponse<PatientAccountCreditDto>> getAccountCredit(

          @PathVariable Long patientId) {

      var data = creditService.getAccountCredit(patientId);
      return ResponseEntity.ok(ApiResponse.ok("Account credit loaded", data));
  }

  /** Apply account credit — JSON body { amount } */
  @PostMapping("/account-credit/apply")
  public ResponseEntity<ApiResponse<PatientAccountCreditDto>> applyAccountCredit(

          @PathVariable Long patientId,
          @RequestBody PatientCreditService.ApplyCreditRequest body) {

      var data = creditService.applyAccountCredit(patientId, body);
      return ResponseEntity.ok(ApiResponse.ok("Credit applied", data));
  }



  /**
   * List notes for a specific invoice (+ symbol in UI triggers this)
   */
  @GetMapping("/invoices/{invoiceId}/notes")
  public ResponseEntity<ApiResponse<List<PatientBillingNoteDto>>> listInvoiceNotes(

          @PathVariable Long patientId,
          @PathVariable Long invoiceId) {

      var data = noteService.listInvoiceNotes(patientId, invoiceId);
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

      var data = noteService.createInvoiceNote(patientId, invoiceId, body);
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

      var data = noteService.updateInvoiceNote(patientId, invoiceId, noteId, body);
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

      noteService.deleteInvoiceNote(patientId, invoiceId, noteId);
      var resp = new ApiResponse.Builder<Void>()
              .success(true)
              .message("Invoice note deleted")
              .build();
      return ResponseEntity.ok(resp);
  }

  /** Add patient deposit */
  @PostMapping("/deposit")
  public ResponseEntity<ApiResponse<PatientDepositDto>> addPatientDeposit(
          @PathVariable Long patientId,
          @RequestBody PatientDepositRequest request) {
      try {
          if (request == null || request.amount() == null) {
              return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                      .body(ApiResponse.error("Deposit amount is required"));
          }
          PatientDepositDto result = depositService.addPatientDeposit(patientId, request);
          return ResponseEntity.ok(ApiResponse.ok("Deposit added successfully", result));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error adding deposit for patient {}", patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error adding deposit: " + ex.getMessage()));
      }
  }

  /** Get all patient deposits */
  @GetMapping("/deposit")
  public ResponseEntity<ApiResponse<List<PatientDepositDto>>> getPatientDeposits(
          @PathVariable Long patientId) {
      try {
          List<PatientDepositDto> deposits = depositService.getPatientDeposits(patientId);
          return ResponseEntity.ok(ApiResponse.ok("Deposits retrieved successfully", deposits));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error getting deposits for patient {}", patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error retrieving deposits: " + ex.getMessage()));
      }
  }

  /** Get a single patient deposit */
  @GetMapping("/deposit/{depositId}")
  public ResponseEntity<ApiResponse<PatientDepositDto>> getPatientDeposit(
          @PathVariable Long patientId,
          @PathVariable Long depositId) {
      try {
          PatientDepositDto deposit = depositService.getPatientDeposit(patientId, depositId);
          return ResponseEntity.ok(ApiResponse.ok("Deposit retrieved successfully", deposit));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error getting deposit {} for patient {}", depositId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error retrieving deposit: " + ex.getMessage()));
      }
  }

  /** Update patient deposit */
  @PutMapping("/deposit/{depositId}")
  public ResponseEntity<ApiResponse<PatientDepositDto>> updatePatientDeposit(
          @PathVariable Long patientId,
          @PathVariable Long depositId,
          @RequestBody PatientDepositRequest request) {
      try {
          if (request == null || request.amount() == null) {
              return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                      .body(ApiResponse.error("Deposit amount is required"));
          }
          PatientDepositDto result = depositService.updatePatientDeposit(patientId, depositId, request);
          return ResponseEntity.ok(ApiResponse.ok("Deposit updated successfully", result));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error updating deposit {} for patient {}", depositId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error updating deposit: " + ex.getMessage()));
      }
  }

  /** Delete patient deposit */
  @DeleteMapping("/deposit/{depositId}")
  public ResponseEntity<ApiResponse<Void>> deletePatientDeposit(
          @PathVariable Long patientId,
          @PathVariable Long depositId) {
      try {
          depositService.deletePatientDeposit(patientId, depositId);
          return ResponseEntity.ok(ApiResponse.ok("Deposit deleted successfully", null));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error deleting deposit {} for patient {}", depositId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error deleting deposit: " + ex.getMessage()));
      }
  }


  /* ===================== Insurance Deposit ===================== */

  @PostMapping("/insurance-deposit")
  public ResponseEntity<ApiResponse<InsuranceDepositDto>> addInsuranceDeposit(
          @PathVariable Long patientId,
          @RequestBody InsuranceDepositDto request) {
      try {
          var data = depositService.addInsuranceDeposit(patientId, request);
          return ResponseEntity.ok(ApiResponse.ok("Insurance deposit added", data));
      } catch (Exception ex) {
          log.error("Error adding insurance deposit for patient {}", patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error adding insurance deposit: " + ex.getMessage()));
      }
  }

  @GetMapping("/insurance-deposit/{depositId}")
  public ResponseEntity<ApiResponse<InsuranceDepositDto>> getInsuranceDeposit(
          @PathVariable Long patientId,
          @PathVariable Long depositId) {
      try {
          var data = depositService.getInsuranceDeposit(patientId, depositId);
          return ResponseEntity.ok(ApiResponse.ok("Insurance deposit retrieved", data));
      } catch (Exception ex) {
          log.error("Error getting insurance deposit for patient {}", patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error retrieving insurance deposit: " + ex.getMessage()));
      }
  }

  @GetMapping("/insurance-deposit")
  public ResponseEntity<ApiResponse<List<InsuranceDepositDto>>> getInsuranceDeposits(
          @PathVariable Long patientId) {
      try {
          var data = depositService.getInsuranceDeposits(patientId);
          return ResponseEntity.ok(ApiResponse.ok("Insurance deposits retrieved", data));
      } catch (Exception ex) {
          log.error("Error getting insurance deposits for patient {}", patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error retrieving insurance deposits: " + ex.getMessage()));
      }
  }

  @PutMapping("/insurance-deposit/{depositId}")
  public ResponseEntity<ApiResponse<InsuranceDepositDto>> updateInsuranceDeposit(
          @PathVariable Long patientId,
          @PathVariable Long depositId,
          @RequestBody InsuranceDepositDto request) {
      try {
          var data = depositService.updateInsuranceDeposit(patientId, depositId, request);
          return ResponseEntity.ok(ApiResponse.ok("Insurance deposit updated", data));
      } catch (Exception ex) {
          log.error("Error updating insurance deposit for patient {}", patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error updating insurance deposit: " + ex.getMessage()));
      }
  }

  @DeleteMapping("/insurance-deposit/{depositId}")
  public ResponseEntity<ApiResponse<Void>> deleteInsuranceDeposit(
          @PathVariable Long patientId,
          @PathVariable Long depositId) {
      try {
          depositService.deleteInsuranceDeposit(patientId, depositId);
          return ResponseEntity.ok(ApiResponse.ok("Insurance deposit deleted", null));
      } catch (Exception ex) {
          log.error("Error deleting insurance deposit for patient {}", patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error deleting insurance deposit: " + ex.getMessage()));
      }
  }

  /** Courtesy Credit: Add courtesy credit and update account credit */
  @PostMapping("/courtesy-credit")
  public ResponseEntity<ApiResponse<PatientAccountCreditDto>> addCourtesyCredit(

          @PathVariable Long patientId,
          @RequestBody CourtesyCreditRequest request
  ) {
      var data = depositService.addCourtesyCredit(patientId, request);
      return ResponseEntity.ok(ApiResponse.ok("Courtesy credit added and account credit updated", data));
  }

  /**
   * //     * Apply courtesy credit to invoice (for uncollected patient balance)
   * //
   */
  @PostMapping("/invoices/{invoiceId}/courtesy-credit")
  public ResponseEntity<ApiResponse<InvoiceCourtesyCreditDto>> applyCourtesyCreditToInvoice(

          @PathVariable Long patientId,
          @PathVariable Long invoiceId,
          @RequestBody CourtesyCreditRequest request
  ) {
      var data = depositService.applyCourtesyCreditToInvoice(patientId, invoiceId, request);
      return ResponseEntity.ok(ApiResponse.ok("Courtesy credit applied to invoice, patient balance reduced", data));
  }


  /**
   * Get courtesy credit applied to specific invoice
   */
  @GetMapping("/invoices/{invoiceId}/courtesy-credit")
  public ResponseEntity<ApiResponse<List<InvoiceCourtesyCreditDto>>> getInvoiceCourtesyCredit(
          @PathVariable Long patientId,
          @PathVariable Long invoiceId
  ) {
      var data = depositService.getInvoiceWithCourtesyCredit(patientId, invoiceId);
      return ResponseEntity.ok(ApiResponse.ok("Invoice courtesy credit details retrieved", data));
  }

  /**
   * Update courtesy credit applied to specific invoice
   */
  @PutMapping("/invoices/{invoiceId}/courtesy-credit")
  public ResponseEntity<ApiResponse<InvoiceCourtesyCreditDto>> updateInvoiceCourtesyCredit(
          @PathVariable Long patientId,
          @PathVariable Long invoiceId,
          @RequestBody CourtesyCreditRequest request
  ) {
      var data = depositService.updateInvoiceCourtesyCredit(patientId, invoiceId, request);
      return ResponseEntity.ok(ApiResponse.ok("Invoice courtesy credit updated successfully", data));
  }

  /**
   * Remove courtesy credit from specific invoice
   */
  @DeleteMapping("/invoices/{invoiceId}/courtesy-credit")
  public ResponseEntity<ApiResponse<InvoiceCourtesyCreditDto>> removeInvoiceCourtesyCredit(
          @PathVariable Long patientId,
          @PathVariable Long invoiceId
  ) {
      var data = depositService.removeInvoiceCourtesyCredit(patientId, invoiceId);
      return ResponseEntity.ok(ApiResponse.ok("Courtesy credit removed from invoice successfully", data));
  }



  /** Lock claim (after lock, claim cannot be edited) */
  @PostMapping("/claims/{claimId}/lock")
  public ResponseEntity<ApiResponse<PatientClaimDto>> lockClaim(

          @PathVariable Long patientId,
          @PathVariable Long claimId) {
      try {
          claimService.lockClaim(patientId, claimId);
          PatientClaimDto dto = claimService.getClaimDtoById(claimId);
          return ResponseEntity.ok(ApiResponse.ok("Claim locked", dto));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                  .body(ApiResponse.error(ex.getMessage()));
      }
  }


  /**
   * Change claim status (accepts JSON body, not request parameter)
   */
  @PostMapping("/claims/{claimId}/status")
  public ResponseEntity<ApiResponse<PatientClaimDto>> changeClaimStatus(

          @PathVariable Long patientId,
          @PathVariable Long claimId,
          @RequestBody ClaimStatusUpdateDto dto
  ) {
      try {
          claimService.changeClaimStatus(patientId, claimId, dto);
          PatientClaimDto response = claimService.getClaimDtoById(claimId);
          return ResponseEntity.ok(ApiResponse.ok("Claim status updated", response));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                  .body(ApiResponse.error(ex.getMessage()));
      }
  }


  /** Submit claim attachment */
  @PostMapping(value = "/claims/{claimId}/submit-attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<PatientClaimDto>> submitClaimAttachment(

          @PathVariable Long patientId,
          @PathVariable Long claimId,
          @RequestParam("file") MultipartFile file) throws Exception {
      try {
          claimService.submitClaimAttachment(patientId, claimId, file);
          PatientClaimDto dto = claimService.getClaimDtoById(claimId);
          return ResponseEntity.ok(ApiResponse.ok("Claim attachment submitted", dto));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error submitting attachment: " + ex.getMessage()));
      }
  }

   /**
    * Generate a printable invoice for a specific invoice (for print invoice button)
    */
   @GetMapping("/invoices/{invoiceId}/print")
   public ResponseEntity<ApiResponse<PatientInvoicePrintDto>> getPrintableInvoice(
           @PathVariable Long patientId,
           @PathVariable Long invoiceId) {
       try {
           PatientInvoicePrintDto dto = service.getPrintableInvoice(patientId, invoiceId);
           return ResponseEntity.ok(ApiResponse.ok("Invoice loaded for printing", dto));
       } catch (IllegalArgumentException ex) {
           return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                   .body(ApiResponse.error(ex.getMessage()));
       }
   }

  /** Get credit adjustment details for invoice */
  @GetMapping("/invoices/{invoiceId}/credit-adjustment")
  public ResponseEntity<ApiResponse<CreditAdjustmentDetailDto>> getCreditAdjustment(
          @PathVariable Long patientId,
          @PathVariable Long invoiceId) {
      try {
          CreditAdjustmentDetailDto dto = creditService.getCreditAdjustment(patientId, invoiceId);
          return ResponseEntity.ok(ApiResponse.ok("Credit adjustment loaded", dto));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                  .body(ApiResponse.error(ex.getMessage()));
      }
  }

  /** Get transfer of credit details for invoice */
  @GetMapping("/invoices/{invoiceId}/transfer-of-credit")
  public ResponseEntity<ApiResponse<TransferOfCreditDetailDto>> getTransferOfCredit(
          @PathVariable Long patientId,
          @PathVariable Long invoiceId) {
      try {
          TransferOfCreditDetailDto dto = creditService.getTransferOfCredit(patientId, invoiceId);
          return ResponseEntity.ok(ApiResponse.ok("Transfer of credit loaded", dto));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                  .body(ApiResponse.error(ex.getMessage()));
      }
  }

  /**
   * Auto-fetch EHR claim form data for printing/display
   * GET /api/patient-billing/{patientId}/claims/{claimId}/ehr-form-data
   */
  @GetMapping("/claims/{claimId}/ehr-form-data")
  public ResponseEntity<ApiResponse<EhrClaimFormDataDto>> getEhrClaimFormData(
          @PathVariable Long patientId,
          @PathVariable Long claimId) {
      try {
          EhrClaimFormDataDto data = claimService.getEhrClaimFormData(patientId, claimId);
          return ResponseEntity.ok(ApiResponse.ok("EHR claim form data loaded", data));
      } catch (IllegalArgumentException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(ApiResponse.error(ex.getMessage()));
      } catch (Exception ex) {
          log.error("Error fetching EHR claim form data for claim {} patient {}", claimId, patientId, ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("Error loading EHR claim form data: " + ex.getMessage()));
      }
  }




}