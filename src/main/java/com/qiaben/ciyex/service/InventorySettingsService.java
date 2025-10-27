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

    @Transactional
    public InventorySettingsDto getSettings(Long orgId) {
        InventorySettings entity = repository.findFirstByOrderByIdAsc()
                .orElseGet(() -> {
                    InventorySettings defaults = InventorySettings.builder()
                            .lowStockAlerts(true)
                            .autoReorderSuggestions(false)
                            .criticalLowPercentage(10)
                            .build();
                    return repository.save(defaults);
                });
        return toDto(entity);
    }

    @Transactional
    public InventorySettingsDto updateSettings(Long orgId, InventorySettingsDto dto) {
        InventorySettings entity = repository.findFirstByOrderByIdAsc()
                .orElse(InventorySettings.builder().build());

        entity.setLowStockAlerts(dto.isLowStockAlerts());
        entity.setAutoReorderSuggestions(dto.isAutoReorderSuggestions());
        entity.setCriticalLowPercentage(dto.getCriticalLowPercentage());

        return toDto(repository.save(entity));
    }

    private InventorySettingsDto toDto(InventorySettings entity) {
        return InventorySettingsDto.builder()
                .id(entity.getId())
                .lowStockAlerts(entity.isLowStockAlerts())
                .autoReorderSuggestions(entity.isAutoReorderSuggestions())
                .criticalLowPercentage(entity.getCriticalLowPercentage())
                .build();
    }
}
