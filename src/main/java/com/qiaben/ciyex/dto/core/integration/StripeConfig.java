package com.qiaben.ciyex.dto.core.integration;

import lombok.Data;

@Data
public class StripeConfig {
    private String apiKey;
    private String webhookSecret;
}
