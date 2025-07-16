package com.qiaben.ciyex.dto.fhir;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FhirPatientBillDTO {

    private String billId;
    private String serviceId;
    private String serviceDate;
    private String appointmentId;
    private String quantity;
    private String unitCost;
    private String totalCost;
}
