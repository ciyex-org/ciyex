package org.ciyex.ehr.inventory.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvOrderLineDto {
    private Long id;
    private Long itemId;
    private String itemName;
    private Integer quantityOrdered;
    private Integer quantityReceived;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private String lotNumber;
    private String expiryDate;
    private String notes;
}
