package org.ciyex.ehr.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.payment.dto.PaymentConfigDto;
import org.ciyex.ehr.payment.entity.PaymentConfig;
import org.ciyex.ehr.payment.repository.PaymentConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentConfigService {

    private final PaymentConfigRepository repo;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    @Transactional(readOnly = true)
    public PaymentConfigDto getConfig() {
        return repo.findByOrgAlias(orgAlias())
                .map(this::toDto)
                .orElse(PaymentConfigDto.builder()
                        .processor("stripe")
                        .enabled(false)
                        .config("{}")
                        .acceptedMethods("[\"credit_card\",\"debit_card\",\"bank_account\",\"fsa\",\"hsa\"]")
                        .convenienceFeeEnabled(false)
                        .autoReceipt(true)
                        .build());
    }

    @Transactional
    public PaymentConfigDto saveConfig(PaymentConfigDto dto) {
        var config = repo.findByOrgAlias(orgAlias()).orElse(new PaymentConfig());
        config.setOrgAlias(orgAlias());

        if (dto.getProcessor() != null) config.setProcessor(dto.getProcessor());
        if (dto.getEnabled() != null) config.setEnabled(dto.getEnabled());
        if (dto.getConfig() != null) config.setConfig(dto.getConfig());
        if (dto.getAcceptedMethods() != null) config.setAcceptedMethods(dto.getAcceptedMethods());
        if (dto.getConvenienceFeeEnabled() != null) config.setConvenienceFeeEnabled(dto.getConvenienceFeeEnabled());
        if (dto.getConvenienceFeePercent() != null) config.setConvenienceFeePercent(dto.getConvenienceFeePercent());
        if (dto.getConvenienceFeeFlat() != null) config.setConvenienceFeeFlat(dto.getConvenienceFeeFlat());
        if (dto.getAutoReceipt() != null) config.setAutoReceipt(dto.getAutoReceipt());
        if (dto.getReceiptEmailTemplateId() != null) config.setReceiptEmailTemplateId(dto.getReceiptEmailTemplateId());

        config = repo.save(config);
        return toDto(config);
    }

    @Transactional(readOnly = true)
    public String getAcceptedMethods() {
        return repo.findByOrgAlias(orgAlias())
                .map(PaymentConfig::getAcceptedMethods)
                .orElse("[\"credit_card\",\"debit_card\",\"bank_account\",\"fsa\",\"hsa\"]");
    }

    private PaymentConfigDto toDto(PaymentConfig e) {
        return PaymentConfigDto.builder()
                .id(e.getId())
                .processor(e.getProcessor())
                .enabled(e.getEnabled())
                .config(e.getConfig())
                .acceptedMethods(e.getAcceptedMethods())
                .convenienceFeeEnabled(e.getConvenienceFeeEnabled())
                .convenienceFeePercent(e.getConvenienceFeePercent())
                .convenienceFeeFlat(e.getConvenienceFeeFlat())
                .autoReceipt(e.getAutoReceipt())
                .receiptEmailTemplateId(e.getReceiptEmailTemplateId())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }
}
