package com.qiaben.ciyex.dto.fhir;

import java.util.Map;

public class FhirProcedureByIdResponseDto {
    private Map<String, Object> data;

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
