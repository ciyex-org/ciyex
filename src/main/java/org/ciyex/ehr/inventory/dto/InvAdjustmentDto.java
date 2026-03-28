package org.ciyex.ehr.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvAdjustmentDto {
    private Long id;

    @NotNull(message = "Item ID is required")
    private Long itemId;
    private String itemName;

    @NotNull(message = "Quantity change is required")
    private Integer quantityChange;

    @NotBlank(message = "Reason code is required")
    private String reasonCode;

    private String notes;
    private String adjustedBy;
    private String referenceType;
    private Long referenceId;

    private String createdAt;
}
