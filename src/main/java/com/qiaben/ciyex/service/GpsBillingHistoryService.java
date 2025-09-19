package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.GpsBillingHistoryDto;
import com.qiaben.ciyex.dto.integration.GpsConfig;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.entity.GpsBillingCard;
import com.qiaben.ciyex.entity.GpsBillingHistory;
import com.qiaben.ciyex.entity.InvoiceBill;
import com.qiaben.ciyex.entity.InvoiceStatus;
import com.qiaben.ciyex.repository.GpsBillingCardRepository;
import com.qiaben.ciyex.repository.GpsBillingHistoryRepository;
import com.qiaben.ciyex.repository.InvoiceBillRepository;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GpsBillingHistoryService {

    private final GpsBillingHistoryRepository repository;
    private final GpsBillingCardRepository cardRepository;
    private final InvoiceBillRepository invoiceRepository;
    private final OrgIntegrationConfigProvider configProvider;

    public GpsBillingHistoryService(
            GpsBillingHistoryRepository repository,
            GpsBillingCardRepository cardRepository,
            InvoiceBillRepository invoiceRepository,
            OrgIntegrationConfigProvider configProvider) {
        this.repository = repository;
        this.cardRepository = cardRepository;
        this.invoiceRepository = invoiceRepository;
        this.configProvider = configProvider;
    }

    private Long requireOrg(String operation) {
        RequestContext ctx = RequestContext.get();
        if (ctx == null || ctx.getOrgId() == null) {
            throw new RuntimeException("Organization context required for " + operation);
        }
        return ctx.getOrgId();
    }

    /* ------------------- READ ------------------- */
    public List<GpsBillingHistoryDto> getAllByUser(Long userId) {
        Long orgId = requireOrg("getAllByUser");
        return repository.findByOrgIdAndUserIdOrderByCreatedAtDesc(orgId, userId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    public List<GpsBillingHistoryDto> getAll() {
        Long orgId = requireOrg("getAll");
        return repository.findByOrgIdOrderByCreatedAtDesc(orgId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    /* ------------------- PAY NOW ------------------- */
    @Transactional
    public GpsBillingHistoryDto recordPayment(GpsBillingHistoryDto dto) {
        Long orgId = requireOrg("recordPayment");
        dto.setOrgId(orgId);

        GpsConfig gpsConfig = configProvider.getGpsForCurrentOrg();
        if (gpsConfig == null || gpsConfig.getTransactUrl() == null) {
            throw new RuntimeException("GPS is not configured for this org");
        }

        try {
            String customerVaultId = dto.getGpsCustomerVaultId();
            if (customerVaultId == null || customerVaultId.isBlank()) {
                GpsBillingCard defaultCard = cardRepository.findFirstByUserIdAndOrgIdAndIsDefaultTrue(dto.getUserId(), orgId)
                        .orElseThrow(() -> new RuntimeException("No default GPS billing card found for user " + dto.getUserId()));
                customerVaultId = defaultCard.getGpsCustomerVaultId();
            }

            // Process payment with GPS
            String transactionId = processGpsPayment(gpsConfig, customerVaultId, dto.getAmount());
            
            if (transactionId == null || transactionId.isEmpty()) {
                throw new RuntimeException("GPS payment processing failed");
            }

            // Create invoice record
            InvoiceBill invoice = InvoiceBill.builder()
                    .orgId(orgId)
                    .userId(dto.getUserId())
                    .amount(dto.getAmount().doubleValue())
                    .status(InvoiceStatus.PAID) // GPS payments are immediate
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .receiptUrl("gps-receipt-" + transactionId + ".pdf")
                    .externalId("GPS-INV-" + System.currentTimeMillis())
                    .build();

            invoice = invoiceRepository.save(invoice);

            GpsBillingHistory entity = GpsBillingHistory.builder()
                    .orgId(orgId)
                    .userId(dto.getUserId())
                    .gpsTransactionId(transactionId)
                    .gpsCustomerVaultId(customerVaultId)
                    .amount(dto.getAmount())
                    .status("SUCCESS")
                    .responseMessage("Payment processed successfully")
                    .invoiceBill(invoice)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            return toDto(repository.save(entity));

        } catch (Exception e) {
            log.error("GPS payment failed: {}", e.getMessage(), e);
            
            // Record the failure
            GpsBillingHistory failedEntity = GpsBillingHistory.builder()
                    .orgId(orgId)
                    .userId(dto.getUserId())
                    .gpsTransactionId("FAILED-" + System.currentTimeMillis())
                    .gpsCustomerVaultId(dto.getGpsCustomerVaultId())
                    .amount(dto.getAmount())
                    .status("FAILED")
                    .responseMessage(e.getMessage())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            repository.save(failedEntity);
            throw new RuntimeException("GPS payment failed: " + e.getMessage(), e);
        }
    }

    private String processGpsPayment(GpsConfig gpsConfig, String customerVaultId, BigDecimal amount) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("username", gpsConfig.getUsername());
        params.put("password", gpsConfig.getPassword());
        params.put("type", "sale");
        params.put("customer_vault_id", customerVaultId);
        params.put("amount", amount.toString());

        String response = sendGpsRequest(gpsConfig.getTransactUrl(), params);
        
        // Parse response for transaction ID
        if (response.contains("response=1")) {
            // Success - extract transaction ID
            String[] parts = response.split("&");
            for (String part : parts) {
                if (part.startsWith("transactionid=")) {
                    return part.substring("transactionid=".length());
                }
            }
        }
        
        log.error("GPS payment failed: {}", response);
        return null;
    }

    private String sendGpsRequest(String url, Map<String, String> params) throws Exception {
        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (postData.length() > 0) {
                postData.append('&');
            }
            postData.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            postData.append('=');
            postData.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }

        @SuppressWarnings("deprecation")
        URL urlObj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setDoOutput(true);

        try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
            out.writeBytes(postData.toString());
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
        }

        return response.toString();
    }

    /* ------------------- UPDATE STATUS ------------------- */
    @Transactional
    public void updateStatus(String transactionId, String status) {
        repository.findByGpsTransactionId(transactionId).ifPresent(history -> {
            history.setStatus(status.toUpperCase());
            history.setUpdatedAt(LocalDateTime.now());
            repository.save(history);
        });
    }

    /* ------------------- MAPPER ------------------- */
    private GpsBillingHistoryDto toDto(GpsBillingHistory entity) {
        GpsBillingHistoryDto dto = new GpsBillingHistoryDto();
        dto.setId(entity.getId());
        dto.setOrgId(entity.getOrgId());
        dto.setUserId(entity.getUserId());
        dto.setGpsTransactionId(entity.getGpsTransactionId());
        dto.setGpsCustomerVaultId(entity.getGpsCustomerVaultId());
        dto.setAmount(entity.getAmount());
        dto.setStatus(entity.getStatus());
        dto.setResponseMessage(entity.getResponseMessage());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}