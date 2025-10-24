package com.qiaben.ciyex.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventorySettingsDto {
    private Long id;
    private boolean lowStockAlerts;
    private boolean autoReorderSuggestions;
    private int criticalLowPercentage;
}
