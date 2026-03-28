package org.ciyex.ehr.inventory.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvSettingsDto2 {
    private Long id;
    private Boolean lowStockAlerts;
    private Boolean autoReorder;
    private Integer criticalLowPct;
    private String defaultCostMethod;
    private Boolean poApprovalRequired;
    private BigDecimal poApprovalThreshold;
}
