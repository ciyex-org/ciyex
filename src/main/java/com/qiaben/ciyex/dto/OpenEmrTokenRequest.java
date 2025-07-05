package com.qiaben.ciyex.dto;


import lombok.Data;

@Data
public class OpenEmrTokenRequest {
    private String scope; // Allow overriding the scope if needed
}
