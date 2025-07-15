package com.qiaben.ciyex.dto.fhir;


import lombok.Data;

@Data
public class OpenEmrTokenRequest {
    private String scope; // Allow overriding the scope if needed
}
