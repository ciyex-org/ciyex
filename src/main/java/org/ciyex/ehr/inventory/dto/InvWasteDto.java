package org.ciyex.ehr.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvWasteDto {
    private Long id;

    @NotNull(message = "Item ID is required")
    private Long itemId;
    private String itemName;

    @NotNull(message = "Quantity is required")
    private Integer quantity;

    @NotBlank(message = "Reason code is required")
    private String reasonCode;

    private String notes;
    private String loggedBy;

    private String createdAt;
}
