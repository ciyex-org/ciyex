package org.ciyex.ehr.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.inventory.dto.InvDashboardDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvDashboardService {

    private final InvItemService itemService;
    private final InvOrderService orderService;
    private final InvMaintenanceService2 maintenanceService;

    @Transactional(readOnly = true)
    public InvDashboardDto getDashboard() {
        return InvDashboardDto.builder()
                .totalItems(itemService.countAll())
                .lowStockCount(itemService.countLowAndCritical().getOrDefault("low", 0L))
                .outOfStockCount(itemService.countLowAndCritical().getOrDefault("critical", 0L))
                .pendingOrders(orderService.countPending())
                .totalValue(itemService.getTotalInventoryValue())
                .expiringWithin30Days(itemService.getExpiringItems(30).size())
                .overdueMaintenanceTasks(maintenanceService.countOverdue())
                .lowStockItems(itemService.getLowStockItems())
                .categoryBreakdown(itemService.getCategoryBreakdown())
                .build();
    }
}
