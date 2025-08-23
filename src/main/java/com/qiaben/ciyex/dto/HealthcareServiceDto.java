package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class HealthcareServiceDto {
    private Long id;
    private String name;
    private String description;
    private String location;
    private String type;
    private Long orgId; // Organization ID passed in header
    private String hoursOfOperation;
}
