package com.qiaben.ciyex.dto.fhir;

import lombok.Data;
import java.util.Map;

@Data
public class FhirPersonListResponseDto {
    private Map<String, Object> jsonObject;

    public Map<String, Object> getJsonObject() {
        return jsonObject;
    }

    public void setJsonObject(Map<String, Object> jsonObject) {
        this.jsonObject = jsonObject;
    }


}
