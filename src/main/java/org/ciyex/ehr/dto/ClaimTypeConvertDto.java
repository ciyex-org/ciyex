package org.ciyex.ehr.dto;

import lombok.Data;

@Data
public class ClaimTypeConvertDto {
    private String targetType; // "MANUAL" or "ELECTRONIC"
}

