package org.ciyex.ehr.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvLotDto {
    private Long id;

    @NotNull(message = "Item ID is required")
    private Long itemId;
    private String itemName;

    @NotBlank(message = "Lot number is required")
    private String lotNumber;

    private String expiryDate;

    @NotNull(message = "Quantity is required")
    private Integer quantity;

    private String receivedDate;
    private BigDecimal costPerUnit;
    private String notes;

    private String createdAt;
}
