package org.ciyex.ehr.inventory.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvDashboardDto {
    private long totalItems;
    private long lowStockCount;
    private long outOfStockCount;
    private long pendingOrders;
    private BigDecimal totalValue;
    private long expiringWithin30Days;
    private long overdueMaintenanceTasks;
    private List<InvItemDto> lowStockItems;
    private List<Map<String, Object>> categoryBreakdown;
}
