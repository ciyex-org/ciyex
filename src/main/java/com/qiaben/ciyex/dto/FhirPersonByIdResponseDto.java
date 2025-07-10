package com.qiaben.ciyex.dto;

import lombok.Data;
import java.util.Map;

@Data
public class FhirPersonByIdResponseDto {
    private Map<String, Object> data;

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }


}
