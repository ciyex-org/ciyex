package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class InventorySettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private boolean lowStockAlerts;
    private boolean autoReorderSuggestions;
    private int criticalLowPercentage;

    private String createdDate;
    private String lastModifiedDate;
}
