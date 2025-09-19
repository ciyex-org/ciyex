package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.GpsBillingCardDto;
import com.qiaben.ciyex.dto.integration.GpsConfig;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.entity.GpsBillingCard;
import com.qiaben.ciyex.repository.GpsBillingCardRepository;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GpsBillingCardService {

    private final GpsBillingCardRepository repository;
    private final OrgIntegrationConfigProvider configProvider;

    public GpsBillingCardService(GpsBillingCardRepository repository, OrgIntegrationConfigProvider configProvider) {
        this.repository = repository;
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
    public List<GpsBillingCardDto> getAllByUser(Long userId) {
        Long orgId = requireOrg("getAllByUser");
        return repository.findByOrgIdAndUserId(orgId, userId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    public Optional<GpsBillingCardDto> getById(Long id) {
        Long orgId = requireOrg("getById");
        return repository.findById(id)
                .filter(card -> card.getOrgId().equals(orgId))
                .map(this::toDto);
    }

    public List<GpsBillingCardDto> getAll() {
        Long orgId = requireOrg("getAll");
        return repository.findByOrgId(orgId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    /* ------------------- CREATE ------------------- */
    @Transactional
    public GpsBillingCardDto create(GpsBillingCardDto dto) {
        Long orgId = requireOrg("create");
        dto.setOrgId(orgId);

        GpsConfig gpsConfig = configProvider.getGpsForCurrentOrg();
        if (gpsConfig == null || gpsConfig.getTransactUrl() == null) {
            throw new RuntimeException("GPS is not configured for this org");
        }

        try {
            // Create customer vault in GPS
            String customerVaultId = createCustomerVault(gpsConfig, dto);
            
            if (customerVaultId == null || customerVaultId.isEmpty()) {
                throw new RuntimeException("Failed to create GPS customer vault");
            }

            GpsBillingCard entity = GpsBillingCard.builder()
                    .orgId(orgId)
                    .userId(dto.getUserId())
                    .gpsCustomerVaultId(customerVaultId)
                    .brand(dto.getBrand())
                    .last4(dto.getLast4())
                    .expMonth(dto.getExpMonth())
                    .expYear(dto.getExpYear())
                    .isDefault(Boolean.TRUE.equals(dto.getIsDefault()))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            // If this is the first card for user/org, make it default automatically
            List<GpsBillingCard> existingCards = repository.findByOrgIdAndUserId(orgId, dto.getUserId());
            if (existingCards.isEmpty()) {
                entity.setIsDefault(true);
            } else if (entity.getIsDefault()) {
                // Clear other default cards for this user
                existingCards.forEach(card -> {
                    card.setIsDefault(false);
                    repository.save(card);
                });
            }

            return toDto(repository.save(entity));

        } catch (Exception e) {
            log.error("Failed to create GPS billing card: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create GPS billing card: " + e.getMessage(), e);
        }
    }

    private String createCustomerVault(GpsConfig gpsConfig, GpsBillingCardDto dto) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("username", gpsConfig.getUsername());
        params.put("password", gpsConfig.getPassword());
        params.put("type", "add_customer");
        params.put("customer_vault", "add_customer");
        params.put("ccnumber", "4111111111111111"); // This would come from frontend tokenization
        params.put("ccexp", String.format("%02d%02d", dto.getExpMonth(), dto.getExpYear() % 100));
        params.put("first_name", dto.getFirstName());
        params.put("last_name", dto.getLastName());
        params.put("address1", dto.getStreet());
        params.put("city", dto.getCity());
        params.put("state", dto.getState());
        params.put("zip", dto.getZip());

        String response = sendGpsRequest(gpsConfig.getTransactUrl(), params);
        
        // Parse response for customer vault ID
        if (response.contains("response=1")) {
            // Success - extract customer vault ID
            String[] parts = response.split("&");
            for (String part : parts) {
                if (part.startsWith("customer_vault_id=")) {
                    return part.substring("customer_vault_id=".length());
                }
            }
        }
        
        log.error("GPS customer vault creation failed: {}", response);
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

    /* ------------------- UPDATE ------------------- */
    @Transactional
    public GpsBillingCardDto update(Long id, GpsBillingCardDto patch, Long orgId) {
        GpsBillingCard existing = repository.findById(id)
                .filter(card -> card.getOrgId().equals(orgId))
                .orElseThrow(() -> new RuntimeException("GPS Billing card not found or access denied"));

        if (patch.getBrand() != null) existing.setBrand(patch.getBrand());
        if (patch.getLast4() != null) existing.setLast4(patch.getLast4());
        if (patch.getExpMonth() != null) existing.setExpMonth(patch.getExpMonth());
        if (patch.getExpYear() != null) existing.setExpYear(patch.getExpYear());
        if (patch.getIsDefault() != null) existing.setIsDefault(patch.getIsDefault());

        existing.setUpdatedAt(LocalDateTime.now());
        return toDto(repository.save(existing));
    }

    /* ------------------- DELETE ------------------- */
    @Transactional
    public void delete(Long id, Long orgId) {
        GpsBillingCard card = repository.findById(id)
                .filter(c -> c.getOrgId().equals(orgId))
                .orElseThrow(() -> new RuntimeException("GPS Billing card not found or access denied"));

        repository.delete(card);
    }

    /* ------------------- SET DEFAULT ------------------- */
    @Transactional
    public GpsBillingCardDto setDefault(Long id, Long orgId) {
        GpsBillingCard targetCard = repository.findById(id)
                .filter(card -> card.getOrgId().equals(orgId))
                .orElseThrow(() -> new RuntimeException("GPS Billing card not found or access denied"));

        // Clear all default flags for this user
        List<GpsBillingCard> userCards = repository.findByOrgIdAndUserId(orgId, targetCard.getUserId());
        userCards.forEach(card -> {
            card.setIsDefault(false);
            card.setUpdatedAt(LocalDateTime.now());
        });
        repository.saveAll(userCards);

        // Set target as default
        targetCard.setIsDefault(true);
        targetCard.setUpdatedAt(LocalDateTime.now());

        return toDto(repository.save(targetCard));
    }

    /* ------------------- MAPPER ------------------- */
    private GpsBillingCardDto toDto(GpsBillingCard entity) {
        GpsBillingCardDto dto = new GpsBillingCardDto();
        dto.setId(entity.getId());
        dto.setOrgId(entity.getOrgId());
        dto.setUserId(entity.getUserId());
        dto.setGpsCustomerVaultId(entity.getGpsCustomerVaultId());
        dto.setGpsTransactionId(entity.getGpsTransactionId());
        dto.setBrand(entity.getBrand());
        dto.setLast4(entity.getLast4());
        dto.setExpMonth(entity.getExpMonth());
        dto.setExpYear(entity.getExpYear());
        dto.setIsDefault(entity.getIsDefault());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}