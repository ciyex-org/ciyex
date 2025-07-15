package com.qiaben.ciyex.dto.fhir;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class StandardApiResponse<T> {
    private List<Object> validationErrors;
    private List<Object> internalErrors;
    private List<T> data;
}
