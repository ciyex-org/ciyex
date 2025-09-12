package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.InventorySettingsDto;
import com.qiaben.ciyex.entity.InventorySettings;
import com.qiaben.ciyex.repository.InventorySettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InventorySettingsService {

    private final InventorySettingsRepository repository;

    @Transactional(readOnly = true)
    public InventorySettingsDto getSettings(Long orgId) {
        InventorySettings entity = repository.findByOrgId(orgId)
                .orElseGet(() -> {
                    InventorySettings defaults = InventorySettings.builder()
                            .orgId(orgId)
                            .lowStockAlerts(true)
                            .autoReorderSuggestions(false)
                            .criticalLowPercentage(10)
                            .createdDate(LocalDateTime.now().toString())
                            .lastModifiedDate(LocalDateTime.now().toString())
                            .build();
                    return repository.save(defaults);
                });
        return toDto(entity);
    }

    private InventorySettingsDto toDto(InventorySettings entity) {
        return InventorySettingsDto.builder()
                .id(entity.getId())
                .orgId(entity.getOrgId())
                .lowStockAlerts(entity.isLowStockAlerts())
                .autoReorderSuggestions(entity.isAutoReorderSuggestions())
                .criticalLowPercentage(entity.getCriticalLowPercentage())
                .build();
    }
}
