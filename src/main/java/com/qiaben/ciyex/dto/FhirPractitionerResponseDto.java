package com.qiaben.ciyex.dto;

import lombok.Data;
import java.util.Map;
import java.util.List;

@Data
public class FhirPractitionerResponseDto {
    private Map<String, Object> meta;
    private String resourceType;
    private String type;
    private int total;
    private List<Object> entry;
    private List<Map<String, String>> link;
}