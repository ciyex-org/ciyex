package com.qiaben.ciyex.dto.openemr;

import lombok.Data;

@Data
public class OpenEmrTokenResponse {
    private String accessToken;
    private String tokenType;
    private Long expiresIn;
    private String rawResponse;
}