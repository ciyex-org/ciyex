package org.ciyex.ehr.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvMaintenanceDto {
    private Long id;

    @NotBlank(message = "Equipment name is required")
    private String equipmentName;

    private String equipmentId;

    @NotBlank(message = "Category is required")
    private String category;

    private String location;
    private String dueDate;
    private String lastServiceDate;
    private String nextServiceDate;
    private String assignee;
    private String vendor;

    @NotBlank(message = "Priority is required")
    private String priority;

    @NotBlank(message = "Status is required")
    private String status;

    private String notes;
    private BigDecimal cost;

    private String createdAt;
    private String updatedAt;
}
