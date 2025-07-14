package com.qiaben.ciyex.dto.fhir;

import java.util.Map;

public class FhirProcedureListResponseDto {
    private Map<String, Object> jsonObject;

    public Map<String, Object> getJsonObject() {
        return jsonObject;
    }

    public void setJsonObject(Map<String, Object> jsonObject) {
        this.jsonObject = jsonObject;
    }
}
