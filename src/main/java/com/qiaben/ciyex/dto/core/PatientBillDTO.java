package com.qiaben.ciyex.dto.core;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientBillDTO {

    @NotBlank(message = "Bill ID is required")
    private String billId;

    @NotBlank(message = "Service ID is required")
    private String serviceId;

    @NotBlank(message = "Service date is required")
    private String serviceDate;

    @NotBlank(message = "Appointment ID is required")
    private String appointmentId;

    @NotBlank(message = "Quantity is required")
    private String quantity;

    @NotBlank(message = "Unit cost is required")
    private String unitCost;

    @NotBlank(message = "Total cost is required")
    private String totalCost;
}

