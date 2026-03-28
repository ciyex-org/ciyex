package org.ciyex.ehr.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.inventory.dto.InvSettingsDto2;
import org.ciyex.ehr.inventory.entity.InvSettings;
import org.ciyex.ehr.inventory.repository.InvSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvSettingsService2 {

    private final InvSettingsRepository repo;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    @Transactional(readOnly = true)
    public InvSettingsDto2 getSettings() {
        return repo.findByOrgAlias(orgAlias())
                .map(this::toDto)
                .orElseGet(() -> InvSettingsDto2.builder()
                        .lowStockAlerts(true)
                        .autoReorder(false)
                        .criticalLowPct(10)
                        .defaultCostMethod("fifo")
                        .poApprovalRequired(false)
                        .build());
    }

    @Transactional
    public InvSettingsDto2 saveSettings(InvSettingsDto2 dto) {
        var entity = repo.findByOrgAlias(orgAlias())
                .orElse(InvSettings.builder().orgAlias(orgAlias()).build());

        if (dto.getLowStockAlerts() != null) entity.setLowStockAlerts(dto.getLowStockAlerts());
        if (dto.getAutoReorder() != null) entity.setAutoReorder(dto.getAutoReorder());
        if (dto.getCriticalLowPct() != null) entity.setCriticalLowPct(dto.getCriticalLowPct());
        if (dto.getDefaultCostMethod() != null) entity.setDefaultCostMethod(dto.getDefaultCostMethod());
        if (dto.getPoApprovalRequired() != null) entity.setPoApprovalRequired(dto.getPoApprovalRequired());
        if (dto.getPoApprovalThreshold() != null) entity.setPoApprovalThreshold(dto.getPoApprovalThreshold());

        return toDto(repo.save(entity));
    }

    private InvSettingsDto2 toDto(InvSettings e) {
        return InvSettingsDto2.builder()
                .id(e.getId())
                .lowStockAlerts(e.getLowStockAlerts())
                .autoReorder(e.getAutoReorder())
                .criticalLowPct(e.getCriticalLowPct())
                .defaultCostMethod(e.getDefaultCostMethod())
                .poApprovalRequired(e.getPoApprovalRequired())
                .poApprovalThreshold(e.getPoApprovalThreshold())
                .build();
    }
}
