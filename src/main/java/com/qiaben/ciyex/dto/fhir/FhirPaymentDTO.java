package com.qiaben.ciyex.dto.fhir;

import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FhirPaymentDTO {

    private String id;
    private LocalDate billDate;
}
