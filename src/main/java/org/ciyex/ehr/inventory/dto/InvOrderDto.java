package org.ciyex.ehr.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvOrderDto {
    private Long id;

    private String poNumber;

    private Long supplierId;
    private String supplierName;

    @NotBlank(message = "Status is required")
    private String status;

    private String orderDate;
    private String expectedDate;
    private String receivedDate;

    private String notes;
    private BigDecimal totalAmount;

    private String createdBy;
    private String approvedBy;

    private List<InvOrderLineDto> lines;

    private String createdAt;
    private String updatedAt;
}
