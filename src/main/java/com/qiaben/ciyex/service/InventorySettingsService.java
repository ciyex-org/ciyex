package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.InventorySettingsDto;
import com.qiaben.ciyex.entity.InventorySettings;
import com.qiaben.ciyex.repository.InventorySettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventorySettingsService {

    private final InventorySettingsRepository repository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Transactional
    public InventorySettingsDto getSettings() {
        InventorySettings entity = repository.findFirstByOrderByIdAsc()
                .orElseGet(() -> {
                    InventorySettings defaults = InventorySettings.builder()
                            .lowStockAlerts(true)
                            .autoReorderSuggestions(false)
                            .criticalLowPercentage(10)
                            .build();
                    // Auto-generate externalId (FHIR id) for defaults
                    defaults.setExternalId("invset-" + UUID.randomUUID());
                    return repository.save(defaults);
                });
        // Ensure externalId exists for existing record
        if (entity.getExternalId() == null || entity.getExternalId().isBlank()) {
            entity.setExternalId("invset-" + UUID.randomUUID());
            entity = repository.save(entity);
        }
        return toDto(entity);
    }

    @Transactional
    public InventorySettingsDto updateSettings(InventorySettingsDto dto) {
        InventorySettings entity = repository.findFirstByOrderByIdAsc()
                .orElse(InventorySettings.builder().build());

        entity.setLowStockAlerts(dto.isLowStockAlerts());
        entity.setAutoReorderSuggestions(dto.isAutoReorderSuggestions());
        entity.setCriticalLowPercentage(dto.getCriticalLowPercentage());

        // Keep existing externalId or assign if missing
        if (entity.getExternalId() == null || entity.getExternalId().isBlank()) {
            entity.setExternalId("invset-" + UUID.randomUUID());
        }

        return toDto(repository.save(entity));
    }

    private InventorySettingsDto toDto(InventorySettings entity) {
        InventorySettingsDto dto = InventorySettingsDto.builder()
                .id(entity.getId())
                .lowStockAlerts(entity.isLowStockAlerts())
                .autoReorderSuggestions(entity.isAutoReorderSuggestions())
                .criticalLowPercentage(entity.getCriticalLowPercentage())
                .build();

        // Populate audit from AuditableEntity
        InventorySettingsDto.Audit audit = new InventorySettingsDto.Audit();
        if (entity.getCreatedDate() != null) {
            audit.setCreatedDate(entity.getCreatedDate().format(DATE_FORMATTER));
        }
        if (entity.getLastModifiedDate() != null) {
            audit.setLastModifiedDate(entity.getLastModifiedDate().format(DATE_FORMATTER));
        }
        dto.setAudit(audit);

        // Map FHIR id from persisted externalId
        dto.setFhirId(entity.getExternalId());
        return dto;
    }
}
