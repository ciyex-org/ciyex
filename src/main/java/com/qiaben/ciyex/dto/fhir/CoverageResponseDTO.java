package com.qiaben.ciyex.dto.fhir;

import lombok.Data;

import java.util.Map;

@Data
public class CoverageResponseDTO {
    private Map<String, Object> meta;
    private String resourceType;
    private String type;
    private int total;
    private Object[] link;
    private Object[] entry;
}
