package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.GpsPaymentDto;
import com.qiaben.ciyex.dto.PaymentRequestDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.dto.BillingHistoryDto;
import com.qiaben.ciyex.entity.GpsPayment;
import com.qiaben.ciyex.entity.InvoiceBill;
import com.qiaben.ciyex.entity.InvoiceStatus;
import com.qiaben.ciyex.repository.GpsPaymentRepository;
import com.qiaben.ciyex.repository.InvoiceBillRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GpsPaymentService {

    private final GpsPaymentRepository repository;
    private final InvoiceBillRepository invoiceBillRepository;
    private final BillingHistoryService billingHistoryService;

    public GpsPaymentService(GpsPaymentRepository repository,
                             InvoiceBillRepository invoiceBillRepository,
                             BillingHistoryService billingHistoryService) {
        this.repository = repository;
        this.invoiceBillRepository = invoiceBillRepository;
        this.billingHistoryService = billingHistoryService;
    }

    private Long requireOrg(String operation) {
        RequestContext ctx = RequestContext.get();
        if (ctx == null || ctx.getOrgId() == null) {
            throw new RuntimeException("Organization context required for " + operation);
        }
        return ctx.getOrgId();
    }

    /* ------------ GPS Simulation Stub ------------ */
    private String simulateGpsCharge(Long cardId, String currency, double amount) {
        log.info("Simulating GPS charge: cardId={}, currency={}, amount={}", cardId, currency, amount);
        // TODO: Replace with real GPS API call
        return "GPS-" + System.currentTimeMillis();
    }

    /* ------------ Pay Invoices with GPS ------------ */
    @Transactional
    public GpsPaymentDto createPaymentForInvoices(PaymentRequestDto request) {
        Long orgId = requireOrg("gpsPayment");

        // 1. Fetch invoices
        List<InvoiceBill> invoices = request.isPayAll()
                ? invoiceBillRepository.findByOrgId(orgId)
                : invoiceBillRepository.findAllById(request.getInvoiceIds());

    double totalAmount = invoices.stream()
        .filter(inv -> inv.getStatus() == InvoiceStatus.UNPAID)
        .map(inv -> inv.getAmount().doubleValue())
        .reduce(0.0, Double::sum);

        if (totalAmount <= 0) {
            throw new RuntimeException("No unpaid invoices available for GPS payment");
        }

        // 2. Handle cardId/cardRef
        Long cardId = null;
        String cardRef = null;
        if (request.getCardId() != null) {
            try {
                cardId = Long.valueOf(request.getCardId()); // GPS numeric ID
            } catch (NumberFormatException e) {
                cardRef = request.getCardId(); // external string ref
                log.info("Storing card reference as string: {}", cardRef);
            }
        }

        // 3. Simulate GPS API call
        String transactionId = simulateGpsCharge(cardId, "USD", totalAmount);
        LocalDateTime now = LocalDateTime.now();

        // 4. Save GPS payment record
    GpsPayment entity = GpsPayment.builder()
        .orgId(orgId)
        .userId(request.getOrgId())
                .cardId(cardId)
                .cardRef(cardRef)
                .gpsTransactionId(transactionId)
                .amount(BigDecimal.valueOf(totalAmount))
                .currency("USD")
                .status("SUCCESS")
                .description("GPS invoice payment")
                .createdAt(now)
                .updatedAt(now)
                .build();
        repository.save(entity);

        // 5. Record unified billing history
        try {
        BillingHistoryDto bh = BillingHistoryDto.builder()
            .orgId(orgId)
            .userId(request.getOrgId())
            .provider(com.qiaben.ciyex.entity.BillingHistory.BillingProvider.GPS)
            .gpsTransactionId(transactionId)
            .gpsCustomerVaultId(cardRef != null ? cardRef : (cardId != null ? cardId.toString() : null))
            .amount(BigDecimal.valueOf(totalAmount))
            .status(com.qiaben.ciyex.entity.BillingHistory.BillingStatus.SUCCEEDED)
            .responseMessage("GPS payment processed")
            .createdAt(now)
            .updatedAt(now)
            .build();
            billingHistoryService.recordGpsPayment(bh);
        } catch (Exception e) {
            log.warn("Failed to write GPS billing history record: {}", e.getMessage());
        }

        // 6. Mark invoices as PAID
        invoices.stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.UNPAID)
                .forEach(inv -> {
                    inv.setStatus(InvoiceStatus.PAID);
                    inv.setPaidAt(now);
                    invoiceBillRepository.save(inv);
                });

        log.info("GPS payment successful: txId={}, amount={}, invoices={}", transactionId, totalAmount, invoices.size());

        return toDto(entity);
    }

    /* ------------ CRUD ------------ */
    public List<GpsPaymentDto> getAll() {
        Long orgId = requireOrg("getAll");
        return repository.findByOrgId(orgId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    public List<GpsPaymentDto> getAllByUser(Long userId) {
        Long orgId = requireOrg("getAllByUser");
        return repository.findByOrgIdAndUserId(orgId, userId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    public Optional<GpsPaymentDto> getById(Long id) {
        Long orgId = requireOrg("getById");
        return repository.findById(id)
                .filter(p -> p.getOrgId().equals(orgId))
                .map(this::toDto);
    }

    @Transactional
    public GpsPaymentDto create(GpsPaymentDto dto) {
        Long orgId = requireOrg("create");
        dto.setOrgId(orgId);

        LocalDateTime now = LocalDateTime.now();
        GpsPayment entity = GpsPayment.builder()
                .orgId(orgId)
                .userId(dto.getUserId())
                .cardId(dto.getCardId())
                .cardRef(dto.getCardRef())
                .gpsTransactionId(dto.getGpsTransactionId())
                .amount(dto.getAmount())
                .currency(dto.getCurrency())
                .status(dto.getStatus())
                .description(dto.getDescription())
                .createdAt(now)
                .updatedAt(now)
                .build();

        return toDto(repository.save(entity));
    }

    @Transactional
    public GpsPaymentDto update(Long id, GpsPaymentDto patch, Long orgId) {
        GpsPayment existing = repository.findById(id)
                .filter(p -> p.getOrgId().equals(orgId))
                .orElseThrow(() -> new RuntimeException("GPS Payment not found or access denied"));

        if (patch.getStatus() != null) existing.setStatus(patch.getStatus());
        if (patch.getDescription() != null) existing.setDescription(patch.getDescription());

        existing.setUpdatedAt(LocalDateTime.now());
        return toDto(repository.save(existing));
    }

    @Transactional
    public void delete(Long id, Long orgId) {
        GpsPayment payment = repository.findById(id)
                .filter(p -> p.getOrgId().equals(orgId))
                .orElseThrow(() -> new RuntimeException("GPS Payment not found or access denied"));
        repository.delete(payment);
    }

    /* ------------ Mapper ------------ */
    private GpsPaymentDto toDto(GpsPayment entity) {
        GpsPaymentDto dto = new GpsPaymentDto();
        dto.setId(entity.getId());
        dto.setOrgId(entity.getOrgId());
        dto.setUserId(entity.getUserId());
        dto.setCardId(entity.getCardId());
        dto.setCardRef(entity.getCardRef());
        dto.setGpsTransactionId(entity.getGpsTransactionId());
        dto.setAmount(entity.getAmount());
        dto.setCurrency(entity.getCurrency());
        dto.setStatus(entity.getStatus());
        dto.setDescription(entity.getDescription());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
