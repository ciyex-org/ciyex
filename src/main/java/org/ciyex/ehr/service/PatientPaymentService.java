package org.ciyex.ehr.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import org.ciyex.ehr.dto.*;
import org.ciyex.ehr.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PatientPaymentService: FHIR-backed service for managing patient payments and allocations.
 * 
 * Stores patient payment records as FHIR Observation resources with extensions.
 * Payment allocations stored as nested extensions within payment observations.
 * Patient account credit stored as extension on Patient resource or separate Observation.
 * 
 * Extension URLs for payment records:
 * - patient-reference: Patient ID
 * - invoice-reference: Invoice ID
 * - payment-amount: Total payment amount
 * - payment-method: Payment method (CASH, CHECK, CARD, ACH, etc.)
 * - payment-created: Payment creation timestamp
 * - payment-allocation: Container for line allocations (extension array)
 *   - payment-allocation-line: Invoice line ID
 *   - payment-allocation-amount: Amount allocated to line
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientPaymentService {
    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    // FHIR Extension URLs
    private static final String EXT_PATIENT = "http://ciyex.com/fhir/StructureDefinition/patient-reference";
    private static final String EXT_INVOICE = "http://ciyex.com/fhir/StructureDefinition/invoice-reference";
    private static final String EXT_PAYMENT_AMOUNT = "http://ciyex.com/fhir/StructureDefinition/payment-amount";
    private static final String EXT_PAYMENT_METHOD = "http://ciyex.com/fhir/StructureDefinition/payment-method";
    private static final String EXT_PAYMENT_CREATED = "http://ciyex.com/fhir/StructureDefinition/payment-created";
    private static final String EXT_PAYMENT_ALLOCATION = "http://ciyex.com/fhir/StructureDefinition/payment-allocation";
    private static final String EXT_PAYMENT_ALLOC_LINE = "http://ciyex.com/fhir/StructureDefinition/payment-allocation-line";
    private static final String EXT_PAYMENT_ALLOC_AMOUNT = "http://ciyex.com/fhir/StructureDefinition/payment-allocation-amount";
    private static final String EXT_LINE_ID = "http://ciyex.com/fhir/StructureDefinition/line-id";
    private static final String EXT_ACCOUNT_CREDIT = "http://ciyex.com/fhir/StructureDefinition/account-credit-balance";

    /* ====== Request DTOs ====== */
    public record VoidReason(String reason) {}
    public record RefundRequest(BigDecimal amount, String reason) {}
    public record TransferCreditRequest(BigDecimal amount, String note) {}

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    /* ================ Patient Payment & Credit (FHIR-backed) ================ */

    /**
     * Get all patient payment allocations for a patient.
     */
    public List<PatientPatientPaymentAllocationDto> getAllPatientPayments(Long patientId) {
        validatePatientExists(patientId);
        log.debug("Getting all patient payments for patient {}", patientId);
        
        List<Observation> allObs = new ArrayList<>();
        Bundle bundle = fhirClientService.search(Observation.class, getPracticeId());
        
        // Handle pagination
        while (bundle != null) {
            List<Observation> pageObs = fhirClientService.extractResources(bundle, Observation.class);
            allObs.addAll(pageObs);
            
            String nextLink = bundle.getLink(Bundle.LINK_NEXT) != null 
                ? bundle.getLink(Bundle.LINK_NEXT).getUrl() 
                : null;
            
            if (nextLink != null) {
                bundle = fhirClientService.loadPage(nextLink, getPracticeId());
            } else {
                break;
            }
        }
        
        log.info("Total observations loaded: {}, filtering for patient {} payments", allObs.size(), patientId);
        
        return allObs.stream()
                .filter(this::isPatientPaymentObservation)
                .filter(obs -> patientId.equals(getPatientIdFromObs(obs)))
                .flatMap(this::extractAllocations)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Get patient payment allocations by invoice.
     */
    public List<PatientPatientPaymentAllocationDto> getPatientPaymentsByInvoice(Long patientId, Long invoiceId) {
        validatePatientExists(patientId);
        validateInvoiceExists(invoiceId);
        log.debug("Getting patient payments for patient {} invoice {}", patientId, invoiceId);
        
        List<Observation> allObs = new ArrayList<>();
        Bundle bundle = fhirClientService.search(Observation.class, getPracticeId());
        
        // Handle pagination
        while (bundle != null) {
            List<Observation> pageObs = fhirClientService.extractResources(bundle, Observation.class);
            allObs.addAll(pageObs);
            
            String nextLink = bundle.getLink(Bundle.LINK_NEXT) != null 
                ? bundle.getLink(Bundle.LINK_NEXT).getUrl() 
                : null;
            
            if (nextLink != null) {
                bundle = fhirClientService.loadPage(nextLink, getPracticeId());
            } else {
                break;
            }
        }
        
        log.info("Total observations loaded: {}, filtering for patient {} invoice {} payments", 
            allObs.size(), patientId, invoiceId);
        
        return allObs.stream()
                .filter(this::isPatientPaymentObservation)
                .filter(obs -> patientId.equals(getPatientIdFromObs(obs)))
                .filter(obs -> invoiceId.equals(getInvoiceIdFromObs(obs)))
                .flatMap(this::extractAllocations)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Get detailed patient payment information including line allocations.
     */
    public PatientPaymentDetailDto getPatientPaymentDetails(Long patientId, Long invoiceId, Long paymentId) {
        validatePatientExists(patientId);
        validateInvoiceExists(invoiceId);
        validatePaymentExists(paymentId);

        Observation payment = fhirClientService.read(Observation.class, String.valueOf(paymentId), getPracticeId());
        Observation invoice = fhirClientService.read(Observation.class, String.valueOf(invoiceId), getPracticeId());
        if (!String.valueOf(patientId).equals(optStringExt(invoice, EXT_PATIENT))) {
            throw new IllegalArgumentException(
                String.format("Invoice ID: %d does not belong to Patient ID: %d", invoiceId, patientId)
            );
        }

        // Build line details with payment allocations
        List<PatientPaymentDetailDto.PatientPaymentLineDetailDto> lineDetails = invoice.getComponent().stream()
                .map(line -> {
                    BigDecimal lineTotal = getComponentDecimal(line, 
                            "http://ciyex.com/fhir/StructureDefinition/line-charge");
                    BigDecimal patientPortion = getComponentDecimal(line, 
                            "http://ciyex.com/fhir/StructureDefinition/line-pt-portion");
                    BigDecimal insurancePortion = getComponentDecimal(line, 
                            "http://ciyex.com/fhir/StructureDefinition/line-ins-portion");
                    BigDecimal previousBalance = lineTotal.subtract(patientPortion).subtract(insurancePortion);

                    // Find payment allocation for this line
                    Long lineId = extractLineId(line);
                    BigDecimal linePayment = payment.getExtension().stream()
                            .filter(ext -> EXT_PAYMENT_ALLOCATION.equals(ext.getUrl()))
                            .filter(ext -> ext.hasExtension())
                            .flatMap(ext -> ext.getExtension().stream())
                            .filter(allocExt -> {
                                Long allocLineId = null;
                                Extension lineIdExt = allocExt.getExtensionByUrl(EXT_PAYMENT_ALLOC_LINE);
                                if (lineIdExt != null && lineIdExt.getValue() instanceof IntegerType it) {
                                    allocLineId = it.getValue().longValue();
                                }
                                return lineId != null && lineId.equals(allocLineId);
                            })
                            .map(allocExt -> {
                                Extension amountExt = allocExt.getExtensionByUrl(EXT_PAYMENT_ALLOC_AMOUNT);
                                if (amountExt != null && amountExt.getValue() instanceof DecimalType dt) {
                                    return dt.getValue();
                                }
                                return BigDecimal.ZERO;
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    String description = getComponentString(line, 
                            "http://ciyex.com/fhir/StructureDefinition/line-treatment");
                    if (description == null) {
                        description = getComponentString(line, 
                                "http://ciyex.com/fhir/StructureDefinition/line-code");
                    }

                    String providerName = getComponentString(line, 
                            "http://ciyex.com/fhir/StructureDefinition/line-provider");

                    return PatientPaymentDetailDto.PatientPaymentLineDetailDto.builder()
                            .lineId(lineId)
                            .description(description != null ? description : "")
                            .providerName(providerName != null ? providerName : "")
                            .amount(lineTotal)
                            .patient(patientPortion)
                            .insurance(insurancePortion)
                            .previousBalance(previousBalance)
                            .payment(linePayment)
                            .build();
                })
                .collect(Collectors.toList());

        // Calculate totals
        BigDecimal patientAmount = getInvoiceExtDecimal(invoice, "http://ciyex.com/fhir/StructureDefinition/pt-balance");
        BigDecimal insuranceAmount = getInvoiceExtDecimal(invoice, "http://ciyex.com/fhir/StructureDefinition/ins-balance");
        BigDecimal previousTotalBalance = getInvoiceExtDecimal(invoice, "http://ciyex.com/fhir/StructureDefinition/total-charge");
        BigDecimal paymentAmount = getPaymentAmount(payment);
        String paymentMethod = getPaymentMethod(payment);

        return PatientPaymentDetailDto.builder()
                .paymentId(paymentId)
                .invoiceId(invoiceId)
                .invoiceNumber(invoiceId.toString())
                .paymentDate(getPaymentCreatedDate(payment))
                .paymentMethod(paymentMethod != null ? paymentMethod : "")
                .chequeNumber("")
                .bankBranchNumber("")
                .patientAmount(patientAmount)
                .insuranceAmount(insuranceAmount)
                .previousTotalBalance(previousTotalBalance)
                .paymentAmount(paymentAmount)
                .ptPaid(nz(patientAmount))
                .insPaid(nz(insuranceAmount))
                .lineDetails(lineDetails)
                .build();
    }

    /**
     * Apply patient payment with allocations to invoice lines.
     * Reduces patient portion on lines based on allocation amounts.
     * Returns payment ID and updated invoice.
     */
    public PatientPaymentResponseDto applyPatientPayment(Long patientId, Long invoiceId, PatientPatientPaymentRequestDto req) {
        validatePatientExists(patientId);
        validateInvoiceExists(invoiceId);
        if (req == null) {
            throw new IllegalArgumentException("Payment request is required");
        }
        if (req.paymentMethod() == null || req.paymentMethod().isEmpty()) {
            throw new IllegalArgumentException("Payment method is required");
        }
        if (req.allocations() == null || req.allocations().isEmpty()) {
            throw new IllegalArgumentException("Payment allocations are required");
        }

        // Verify invoice exists and belongs to patient
        Observation invoice = fhirClientService.read(Observation.class, String.valueOf(invoiceId), getPracticeId());
        if (!String.valueOf(patientId).equals(optStringExt(invoice, EXT_PATIENT))) {
            throw new IllegalArgumentException(
                String.format("Invoice ID: %d does not belong to Patient ID: %d", invoiceId, patientId)
            );
        }

        // Calculate total payment
        BigDecimal totalPayment = req.allocations().stream()
                .map(a -> nz(a.amount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Create payment record as Observation
        Observation payment = new Observation();
        payment.setStatus(Observation.ObservationStatus.FINAL);
        payment.setCode(new CodeableConcept().addCoding(
            new Coding("http://ciyex.com/fhir/CodeSystem/payment", "patient-payment", "Patient Payment")
        ));
        payment.addExtension(new Extension(EXT_PATIENT, new StringType(String.valueOf(patientId))));
        payment.addExtension(new Extension(EXT_INVOICE, new StringType(String.valueOf(invoiceId))));
        payment.addExtension(new Extension(EXT_PAYMENT_AMOUNT, new DecimalType(totalPayment)));
        payment.addExtension(new Extension(EXT_PAYMENT_METHOD, new StringType(req.paymentMethod())));
        payment.addExtension(new Extension(EXT_PAYMENT_CREATED, new StringType(LocalDateTime.now().toString())));

        // Process each allocation
        for (var allocReq : req.allocations()) {
            // Verify line exists in invoice
            if (invoice.getComponent() == null || invoice.getComponent().isEmpty()) {
                throw new IllegalArgumentException("Invoice has no line items");
            }

            Long invoiceLineId = allocReq.invoiceLineId();
            Observation.ObservationComponentComponent line = invoice.getComponent().stream()
                    .filter(c -> invoiceLineId.equals(extractLineId(c)))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Invoice line not found with ID: %d", invoiceLineId)
                    ));

            BigDecimal paymentAmount = nz(allocReq.amount());
            BigDecimal currentPtPortion = getComponentDecimal(line, 
                    "http://ciyex.com/fhir/StructureDefinition/line-pt-portion");

            // Reduce patient portion by payment amount
            BigDecimal newPtPortion = currentPtPortion.subtract(paymentAmount).max(BigDecimal.ZERO);
            line.getExtension().removeIf(e -> "http://ciyex.com/fhir/StructureDefinition/line-pt-portion".equals(e.getUrl()));
            line.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/line-pt-portion", 
                    new DecimalType(newPtPortion)));

            // Add allocation extension to payment
            Extension allocExt = new Extension();
            allocExt.setUrl(EXT_PAYMENT_ALLOCATION);
            allocExt.addExtension(new Extension(EXT_PAYMENT_ALLOC_LINE, new IntegerType(invoiceLineId.intValue())));
            allocExt.addExtension(new Extension(EXT_PAYMENT_ALLOC_AMOUNT, new DecimalType(paymentAmount)));
            payment.addExtension(allocExt);
        }

        // Create payment record and capture the generated ID
        MethodOutcome outcome = fhirClientService.create(payment, getPracticeId());
        Long paymentId = null;
        if (outcome != null && outcome.getId() != null) {
            try {
                paymentId = Long.parseLong(outcome.getId().getIdPart());
            } catch (NumberFormatException e) {
                paymentId = Long.valueOf(Math.abs(outcome.getId().getIdPart().hashCode()));
            }
        }

        // Update invoice totals and status
        recalcInvoiceTotals(invoice);
        updateInvoiceStatus(invoice);
        fhirClientService.update(invoice, getPracticeId());

        // Reduce account credit if available
        reduceAccountCredit(patientId, totalPayment);

        PatientInvoiceDto invoiceDto = readInvoiceDto(invoiceId, patientId);
        return new PatientPaymentResponseDto(paymentId, invoiceDto);
    }

   

    /**
     * Edit patient payment amount and/or method.
     */
    public PatientInvoiceDto editPatientPayment(Long patientId, Long invoiceId, Long paymentId, PatientPaymentDto dto) {
        validatePatientExists(patientId);
        validateInvoiceExists(invoiceId);
        validatePaymentExists(paymentId);
        if (dto == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        // Verify invoice exists
        Observation invoice = fhirClientService.read(Observation.class, String.valueOf(invoiceId), getPracticeId());
        if (!String.valueOf(patientId).equals(optStringExt(invoice, EXT_PATIENT))) {
            throw new IllegalArgumentException(
                String.format("Invoice ID: %d does not belong to Patient ID: %d", invoiceId, patientId)
            );
        }

        // Read and update payment
        Observation payment = fhirClientService.read(Observation.class, String.valueOf(paymentId), getPracticeId());

        if (dto.amount() != null) {
            payment.getExtension().removeIf(e -> EXT_PAYMENT_AMOUNT.equals(e.getUrl()));
            payment.addExtension(new Extension(EXT_PAYMENT_AMOUNT, new DecimalType(dto.amount())));
        }
        if (dto.paymentMethod() != null) {
            String method = dto.paymentMethod().replace(" ", "_").replace("-", "_").toUpperCase();
            payment.getExtension().removeIf(e -> EXT_PAYMENT_METHOD.equals(e.getUrl()));
            payment.addExtension(new Extension(EXT_PAYMENT_METHOD, new StringType(method)));
        }

        fhirClientService.update(payment, getPracticeId());

        // Recalculate invoice totals
        recalcInvoiceTotals(invoice);
        fhirClientService.update(invoice, getPracticeId());

        return readInvoiceDto(invoiceId, patientId);
    }

    /**
     * VOID = delete allocations then delete payment.
     */
    public PatientInvoiceDto voidPatientPayment(Long patientId, Long invoiceId, Long paymentId, VoidReason reason) {
        validatePatientExists(patientId);
        validateInvoiceExists(invoiceId);
        validatePaymentExists(paymentId);

        Observation invoice = fhirClientService.read(Observation.class, String.valueOf(invoiceId), getPracticeId());
        if (!String.valueOf(patientId).equals(optStringExt(invoice, EXT_PATIENT))) {
            throw new IllegalArgumentException(
                String.format("Invoice ID: %d does not belong to Patient ID: %d", invoiceId, patientId)
            );
        }

        Observation payment = fhirClientService.read(Observation.class, String.valueOf(paymentId), getPracticeId());

        // Restore patient portions from payment allocations
        for (Extension ext : payment.getExtension()) {
            if (EXT_PAYMENT_ALLOCATION.equals(ext.getUrl()) && ext.hasExtension()) {
                Long lineId = null;
                BigDecimal amount = null;

                for (Extension e : ext.getExtension()) {
                    if (EXT_PAYMENT_ALLOC_LINE.equals(e.getUrl()) && e.getValue() instanceof IntegerType it) {
                        lineId = it.getValue().longValue();
                    } else if (EXT_PAYMENT_ALLOC_AMOUNT.equals(e.getUrl()) && e.getValue() instanceof DecimalType dt) {
                        amount = dt.getValue();
                    }
                }

                if (lineId != null && amount != null) {
                    final Long finalLineId = lineId;
                    final BigDecimal finalAmount = amount;
                    invoice.getComponent().stream()
                            .filter(c -> finalLineId.equals(extractLineId(c)))
                            .findFirst()
                            .ifPresent(line -> {
                                BigDecimal currentPtPortion = getComponentDecimal(line, 
                                        "http://ciyex.com/fhir/StructureDefinition/line-pt-portion");
                                BigDecimal newPtPortion = currentPtPortion.add(finalAmount);
                                line.getExtension().removeIf(e -> "http://ciyex.com/fhir/StructureDefinition/line-pt-portion".equals(e.getUrl()));
                                line.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/line-pt-portion", 
                                        new DecimalType(newPtPortion)));
                            });
                }
            }
        }

        // Delete payment record
        fhirClientService.delete(Observation.class, String.valueOf(paymentId), getPracticeId());

        // Recalculate invoice totals and update status
        recalcInvoiceTotals(invoice);
        updateInvoiceStatus(invoice);
        fhirClientService.update(invoice, getPracticeId());

        return readInvoiceDto(invoiceId, patientId);
    }

    /**
     * REFUND patient �+' add to patient account credit.
     */
    public PatientInvoiceDto refundPatientPayment(Long patientId, Long invoiceId, Long paymentId, RefundRequest req) {
        validatePatientExists(patientId);
        validateInvoiceExists(invoiceId);
        validatePaymentExists(paymentId);

        Observation invoice = fhirClientService.read(Observation.class, String.valueOf(invoiceId), getPracticeId());
        if (!String.valueOf(patientId).equals(optStringExt(invoice, EXT_PATIENT))) {
            throw new IllegalArgumentException(
                String.format("Invoice ID: %d does not belong to Patient ID: %d", invoiceId, patientId)
            );
        }

        BigDecimal amount = Optional.ofNullable(req).map(RefundRequest::amount)
                .orElseThrow(() -> new IllegalArgumentException("Refund amount required"));
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Refund amount must be > 0");
        }

        // Read payment
        Observation payment = fhirClientService.read(Observation.class, String.valueOf(paymentId), getPracticeId());
        BigDecimal paymentAmount = nz(getPaymentAmount(payment));

        if (paymentAmount.compareTo(amount) != 0 && paymentAmount.compareTo(amount) <= 0) {
            throw new IllegalArgumentException("Refund amount exceeds payment amount");
        }

        // Restore patient portions proportionally
        for (Extension ext : payment.getExtension()) {
            if (EXT_PAYMENT_ALLOCATION.equals(ext.getUrl()) && ext.hasExtension()) {
                Long lineId = null;
                BigDecimal allocAmount = null;

                for (Extension e : ext.getExtension()) {
                    if (EXT_PAYMENT_ALLOC_LINE.equals(e.getUrl()) && e.getValue() instanceof IntegerType it) {
                        lineId = it.getValue().longValue();
                    } else if (EXT_PAYMENT_ALLOC_AMOUNT.equals(e.getUrl()) && e.getValue() instanceof DecimalType dt) {
                        allocAmount = dt.getValue();
                    }
                }

                if (lineId != null && allocAmount != null) {
                    final Long finalLineId = lineId;
                    final BigDecimal refundForLine = paymentAmount.compareTo(amount) == 0 
                            ? allocAmount 
                            : allocAmount.multiply(amount).divide(paymentAmount, 2, java.math.RoundingMode.HALF_UP);
                    
                    invoice.getComponent().stream()
                            .filter(c -> finalLineId.equals(extractLineId(c)))
                            .findFirst()
                            .ifPresent(line -> {
                                BigDecimal currentPtPortion = getComponentDecimal(line, 
                                        "http://ciyex.com/fhir/StructureDefinition/line-pt-portion");
                                BigDecimal newPtPortion = currentPtPortion.add(refundForLine);
                                line.getExtension().removeIf(e -> "http://ciyex.com/fhir/StructureDefinition/line-pt-portion".equals(e.getUrl()));
                                line.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/line-pt-portion", 
                                        new DecimalType(newPtPortion)));
                            });
                }
            }
        }

        // Handle refund: if refund == payment amount, delete payment; otherwise reduce amount
        if (paymentAmount.compareTo(amount) == 0) {
            fhirClientService.delete(Observation.class, String.valueOf(paymentId), getPracticeId());
        } else {
            payment.getExtension().removeIf(e -> EXT_PAYMENT_AMOUNT.equals(e.getUrl()));
            payment.addExtension(new Extension(EXT_PAYMENT_AMOUNT, 
                    new DecimalType(paymentAmount.subtract(amount))));
            fhirClientService.update(payment, getPracticeId());
        }

        // Add refund amount to patient account credit
        addAccountCredit(patientId, amount);

        // Recalculate invoice totals and update status
        recalcInvoiceTotals(invoice);
        updateInvoiceStatus(invoice);
        fhirClientService.update(invoice, getPracticeId());

        return readInvoiceDto(invoiceId, patientId);
    }

    /**
     * Transfer patient account credit from one patient to another.
     */
    public PatientAccountCreditDto[] transferPatientCreditToPatient(Long fromPatientId, Long toPatientId, BigDecimal amount) {
        validatePatientExists(fromPatientId);
        validatePatientExists(toPatientId);
        if (fromPatientId.equals(toPatientId)) {
            throw new IllegalArgumentException("Source and destination patients must differ");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        // Get source patient credit
        BigDecimal fromCredit = getAccountCredit(fromPatientId);
        if (fromCredit.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient credit in source account");
        }

        // Get destination patient credit (or create if doesn't exist)
        BigDecimal toCredit = getAccountCredit(toPatientId);

        // Update credits
        setAccountCredit(fromPatientId, fromCredit.subtract(amount));
        setAccountCredit(toPatientId, nz(toCredit).add(amount));

        return new PatientAccountCreditDto[] {
                new PatientAccountCreditDto(fromPatientId, fromCredit.subtract(amount)),
                new PatientAccountCreditDto(toPatientId, nz(toCredit).add(amount))
        };
    }

    /* ===================== Helpers ===================== */

    /**
     * Null-coalescing: return zero if value is null.
     */
    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * Recalculate invoice totals from all lines.
     */
    private void recalcInvoiceTotals(Observation invoice) {
        BigDecimal totalCharge = BigDecimal.ZERO;
        BigDecimal totalInsPortion = BigDecimal.ZERO;
        BigDecimal totalPtPortion = BigDecimal.ZERO;

        if (invoice.getComponent() != null) {
            for (Observation.ObservationComponentComponent comp : invoice.getComponent()) {
                totalCharge = totalCharge.add(getComponentDecimal(comp, 
                        "http://ciyex.com/fhir/StructureDefinition/line-charge"));
                totalInsPortion = totalInsPortion.add(getComponentDecimal(comp, 
                        "http://ciyex.com/fhir/StructureDefinition/line-ins-portion"));
                totalPtPortion = totalPtPortion.add(getComponentDecimal(comp, 
                        "http://ciyex.com/fhir/StructureDefinition/line-pt-portion"));
            }
        }

        invoice.getExtension().removeIf(e -> "http://ciyex.com/fhir/StructureDefinition/total-charge".equals(e.getUrl()));
        invoice.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/total-charge", 
                new DecimalType(totalCharge)));

        invoice.getExtension().removeIf(e -> "http://ciyex.com/fhir/StructureDefinition/ins-balance".equals(e.getUrl()));
        invoice.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/ins-balance", 
                new DecimalType(totalInsPortion)));

        invoice.getExtension().removeIf(e -> "http://ciyex.com/fhir/StructureDefinition/pt-balance".equals(e.getUrl()));
        invoice.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/pt-balance", 
                new DecimalType(totalPtPortion)));
    }

    /**
     * Update invoice status based on balances.
     */
    private void updateInvoiceStatus(Observation invoice) {
        BigDecimal ptBalance = nz(getInvoiceExtDecimal(invoice, "http://ciyex.com/fhir/StructureDefinition/pt-balance"));
        BigDecimal insBalance = nz(getInvoiceExtDecimal(invoice, "http://ciyex.com/fhir/StructureDefinition/ins-balance"));
        BigDecimal totalOutstanding = ptBalance.add(insBalance);
        BigDecimal totalCharge = nz(getInvoiceExtDecimal(invoice, "http://ciyex.com/fhir/StructureDefinition/total-charge"));

        String newStatus = "OPEN";
        if (totalOutstanding.compareTo(BigDecimal.ZERO) <= 0) {
            newStatus = "PAID";
        } else if (totalOutstanding.compareTo(totalCharge) < 0) {
            newStatus = "PARTIALLY_PAID";
        }

        invoice.getExtension().removeIf(e -> "http://ciyex.com/fhir/StructureDefinition/invoice-status".equals(e.getUrl()));
        invoice.addExtension(new Extension("http://ciyex.com/fhir/StructureDefinition/invoice-status", 
                new StringType(newStatus)));
    }

    /**
     * Extract allocations from payment observation as DTOs.
     */
    private java.util.stream.Stream<PatientPatientPaymentAllocationDto> extractAllocations(Observation payment) {
        if (payment == null || !payment.hasExtension()) {
            return java.util.stream.Stream.empty();
        }
        
        try {
            Long paymentId = Long.parseLong(payment.getIdElement().getIdPart());
            String paymentMethod = getPaymentMethod(payment);
            LocalDateTime createdAt = getPaymentCreatedDate(payment);
            List<PatientPatientPaymentAllocationDto> allocations = new ArrayList<>();
            
            for (Extension ext : payment.getExtension()) {
                if (EXT_PAYMENT_ALLOCATION.equals(ext.getUrl()) && ext.hasExtension()) {
                    Long lineId = null;
                    BigDecimal amount = null;
                    
                    for (Extension e : ext.getExtension()) {
                        if (EXT_PAYMENT_ALLOC_LINE.equals(e.getUrl()) && e.getValue() instanceof IntegerType it) {
                            lineId = it.getValue().longValue();
                        } else if (EXT_PAYMENT_ALLOC_AMOUNT.equals(e.getUrl()) && e.getValue() instanceof DecimalType dt) {
                            amount = dt.getValue();
                        }
                    }
                    
                    if (lineId != null && amount != null) {
                        allocations.add(new PatientPatientPaymentAllocationDto(paymentId, lineId, amount, paymentMethod, createdAt));
                    }
                }
            }
            
            return allocations.stream();
        } catch (Exception e) {
            log.warn("Error extracting allocations from payment: {}", e.getMessage());
            return java.util.stream.Stream.empty();
        }
    }

    /**
     * Get account credit balance for patient.
     */
    private BigDecimal getAccountCredit(Long patientId) {
        try {
            Bundle bundle = fhirClientService.search(Patient.class, getPracticeId());
            return fhirClientService.extractResources(bundle, Patient.class).stream()
                    .filter(p -> patientId.toString().equals(p.getIdElement().getIdPart()))
                    .findFirst()
                    .map(p -> {
                        Extension ext = p.getExtensionByUrl(EXT_ACCOUNT_CREDIT);
                        if (ext != null && ext.getValue() instanceof DecimalType dt) {
                            return dt.getValue();
                        }
                        return BigDecimal.ZERO;
                    })
                    .orElse(BigDecimal.ZERO);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Set account credit balance for patient.
     */
    private void setAccountCredit(Long patientId, BigDecimal amount) {
        try {
            Patient patient = fhirClientService.read(Patient.class, String.valueOf(patientId), getPracticeId());
            patient.getExtension().removeIf(e -> EXT_ACCOUNT_CREDIT.equals(e.getUrl()));
            patient.addExtension(new Extension(EXT_ACCOUNT_CREDIT, new DecimalType(nz(amount))));
            fhirClientService.update(patient, getPracticeId());
        } catch (Exception e) {
            log.warn("Unable to set account credit for patient {}: {}", patientId, e.getMessage());
        }
    }

    /**
     * Add amount to patient account credit.
     */
    private void addAccountCredit(Long patientId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal current = getAccountCredit(patientId);
        setAccountCredit(patientId, nz(current).add(amount));
    }

    /**
     * Reduce account credit by payment amount.
     */
    private void reduceAccountCredit(Long patientId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal current = getAccountCredit(patientId);
        if (current.compareTo(amount) >= 0) {
            setAccountCredit(patientId, current.subtract(amount));
        }
    }

    /**
     * Extract line ID from observation component extension.
     */
    private Long extractLineId(Observation.ObservationComponentComponent comp) {
        Extension ext = comp.getExtensionByUrl(EXT_LINE_ID);
        if (ext != null && ext.getValue() instanceof IntegerType it) {
            return it.getValue().longValue();
        }
        return null;
    }

    /**
     * Get decimal value from observation component extension.
     */
    private BigDecimal getComponentDecimal(Observation.ObservationComponentComponent comp, String url) {
        if (comp == null) return BigDecimal.ZERO;
        Extension ext = comp.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof DecimalType dt) {
            return dt.getValue();
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get string value from observation component extension.
     */
    private String getComponentString(Observation.ObservationComponentComponent comp, String url) {
        if (comp == null) return null;
        Extension ext = comp.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType st) {
            return st.getValue();
        }
        return null;
    }

    /**
     * Get decimal value from observation extension.
     */
    private BigDecimal getInvoiceExtDecimal(Observation obs, String url) {
        if (obs == null) return BigDecimal.ZERO;
        Extension ext = obs.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof DecimalType dt) {
            return dt.getValue();
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get string extension value (optional).
     */
    private String optStringExt(DomainResource resource, String url) {
        if (resource == null) return null;
        Extension ext = resource.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType st) {
            return st.getValue();
        }
        return null;
    }

    /**
     * Get payment amount from payment observation.
     */
    private BigDecimal getPaymentAmount(Observation payment) {
        if (payment == null) return null;
        Extension ext = payment.getExtensionByUrl(EXT_PAYMENT_AMOUNT);
        if (ext != null && ext.getValue() instanceof DecimalType dt) {
            return dt.getValue();
        }
        return null;
    }

    /**
     * Get payment method from payment observation.
     */
    private String getPaymentMethod(Observation payment) {
        if (payment == null) return null;
        Extension ext = payment.getExtensionByUrl(EXT_PAYMENT_METHOD);
        if (ext != null && ext.getValue() instanceof StringType st) {
            return st.getValue();
        }
        return null;
    }

    /**
     * Get payment creation date from payment observation.
     */
    private LocalDateTime getPaymentCreatedDate(Observation payment) {
        if (payment == null) return LocalDateTime.now();
        Extension ext = payment.getExtensionByUrl(EXT_PAYMENT_CREATED);
        if (ext != null && ext.getValue() instanceof StringType st) {
            try {
                return LocalDateTime.parse(st.getValue());
            } catch (Exception e) {
                return LocalDateTime.now();
            }
        }
        return LocalDateTime.now();
    }

    /**
     * Get code from observation.
     */
    private String getCodeFromObservation(Observation obs) {
        if (obs == null || obs.getCode() == null || !obs.getCode().hasCoding()) {
            return null;
        }
        return obs.getCode().getCodingFirstRep().getCode();
    }

    /**
     * Check if observation is a patient payment record.
     */
    private boolean isPatientPaymentObservation(Observation obs) {
        if (obs == null) return false;
        String code = getCodeFromObservation(obs);
        return code != null && code.equals("patient-payment");
    }

    private Long getPatientIdFromObs(Observation obs) {
        Extension ext = obs.getExtensionByUrl(EXT_PATIENT);
        if (ext != null && ext.getValue() instanceof StringType st) {
            try {
                return Long.parseLong(st.getValue());
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private Long getInvoiceIdFromObs(Observation obs) {
        Extension ext = obs.getExtensionByUrl(EXT_INVOICE);
        if (ext != null && ext.getValue() instanceof StringType st) {
            try {
                return Long.parseLong(st.getValue());
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    /**
     * Read invoice DTO from FHIR Observation.
     */
    private PatientInvoiceDto readInvoiceDto(Long invoiceId, Long patientId) {
        try {
            Observation invoice = fhirClientService.read(Observation.class, String.valueOf(invoiceId), getPracticeId());
            return fromFhirObservation(invoice, patientId);
        } catch (Exception e) {
            log.error("Error reading invoice: {}", e.getMessage());
            throw new IllegalArgumentException(
                String.format("Unable to read invoice ID: %d", invoiceId)
            );
        }
    }

    /**
     * Convert FHIR Observation to PatientInvoiceDto.
     */
    

    private PatientInvoiceDto fromFhirObservation(Observation obs, Long patientId) {
        List<PatientInvoiceLineDto> lines = new ArrayList<>();
        if (obs.getComponent() != null) {
            int index = 0;
            for (Observation.ObservationComponentComponent comp : obs.getComponent()) {
                Long lineId = extractLineId(comp);
                if (lineId == null) lineId = (long) index;
                String dosStr = getComponentString(comp, "http://ciyex.com/fhir/StructureDefinition/line-dos");
                LocalDate dos = dosStr != null ? LocalDate.parse(dosStr) : LocalDate.now();
                String code = getComponentString(comp, "http://ciyex.com/fhir/StructureDefinition/line-code");
                String treatment = getComponentString(comp, "http://ciyex.com/fhir/StructureDefinition/line-treatment");
                String provider = getComponentString(comp, "http://ciyex.com/fhir/StructureDefinition/line-provider");
                BigDecimal charge = getComponentDecimal(comp, "http://ciyex.com/fhir/StructureDefinition/line-charge");
                BigDecimal allowed = getComponentDecimal(comp, "http://ciyex.com/fhir/StructureDefinition/line-allowed");
                BigDecimal insWO = getComponentDecimal(comp, "http://ciyex.com/fhir/StructureDefinition/line-ins-writeoff");
                BigDecimal insPortion = getComponentDecimal(comp, "http://ciyex.com/fhir/StructureDefinition/line-ins-portion");
                BigDecimal ptPortion = getComponentDecimal(comp, "http://ciyex.com/fhir/StructureDefinition/line-pt-portion");
                lines.add(new PatientInvoiceLineDto(lineId, dos, code, treatment, provider, charge, allowed, insWO, insPortion, ptPortion));
                index++;
            }
        }

        String statusStr = optStringExt(obs, "http://ciyex.com/fhir/StructureDefinition/invoice-status");
        PatientInvoiceStatus status = PatientInvoiceStatus.OPEN;
        if (statusStr != null) {
            try {
                status = PatientInvoiceStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                status = PatientInvoiceStatus.OPEN;
            }
        }

        BigDecimal insWO = getInvoiceExtDecimal(obs, "http://ciyex.com/fhir/StructureDefinition/ins-writeoff");
        BigDecimal appliedWO = BigDecimal.ZERO;
        BigDecimal ptBalance = getInvoiceExtDecimal(obs, "http://ciyex.com/fhir/StructureDefinition/pt-balance");
        BigDecimal insBalance = getInvoiceExtDecimal(obs, "http://ciyex.com/fhir/StructureDefinition/ins-balance");
        BigDecimal totalCharge = getInvoiceExtDecimal(obs, "http://ciyex.com/fhir/StructureDefinition/total-charge");

        String fhirId = obs.getIdElement().getIdPart();
        Long invoiceId;
        try {
            invoiceId = Long.parseLong(fhirId);
        } catch (NumberFormatException e) {
            invoiceId = Long.valueOf(Math.abs(fhirId.hashCode()));
        }

        String invDateStr = optStringExt(obs, "http://ciyex.com/fhir/StructureDefinition/invoice-date");
        java.time.LocalDateTime invoiceDate = null;
        if (invDateStr != null && !invDateStr.isEmpty()) {
            try {
                invoiceDate = java.time.LocalDateTime.parse(invDateStr);
            } catch (Exception e) {
                try {
                    invoiceDate = java.time.LocalDate.parse(invDateStr).atStartOfDay();
                } catch (Exception ex) {
                    invoiceDate = null;
                }
            }
        }

        return new PatientInvoiceDto(invoiceId, patientId, invoiceDate, status, insWO, appliedWO, ptBalance, insBalance, totalCharge, lines);
    }

    private void validatePatientExists(Long patientId) {
        if (patientId == null) throw new IllegalArgumentException("Patient ID cannot be null");
        try {
            fhirClientService.read(Patient.class, String.valueOf(patientId), getPracticeId());
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Patient not found with ID: %d. Please provide a valid Patient ID.", patientId));
        }
    }

    private void validateInvoiceExists(Long invoiceId) {
        if (invoiceId == null) throw new IllegalArgumentException("Invoice ID cannot be null");
        try {
            fhirClientService.read(Observation.class, String.valueOf(invoiceId), getPracticeId());
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Invoice not found with ID: %d. Please provide a valid Invoice ID.", invoiceId));
        }
    }

    private void validatePaymentExists(Long paymentId) {
        if (paymentId == null) throw new IllegalArgumentException("Payment ID cannot be null");
        try {
            fhirClientService.read(Observation.class, String.valueOf(paymentId), getPracticeId());
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Patient payment not found with ID: %d. Please provide a valid payment ID.", paymentId));
        }
    }
}
