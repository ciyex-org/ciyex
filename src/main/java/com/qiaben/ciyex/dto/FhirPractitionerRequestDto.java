package com.qiaben.ciyex.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class FhirPractitionerRequestDto {
    private String id;
    private Map<String, Object> meta;
    private String resourceType;
    private Map<String, String> text;
    private List<Map<String, String>> identifier;
    private boolean active;
    private List<Map<String, Object>> name;
}