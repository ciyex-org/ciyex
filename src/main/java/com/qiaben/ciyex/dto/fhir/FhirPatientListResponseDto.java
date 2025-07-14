package com.qiaben.ciyex.dto.fhir;

import lombok.Data;
import java.util.List;

@Data
public class FhirPatientListResponseDto {
    private List<Object> validationErrors;
    private List<Object> internalErrors;
    private List<FhirPatientDto> data;
}
