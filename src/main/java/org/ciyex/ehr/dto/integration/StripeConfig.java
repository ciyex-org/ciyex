package org.ciyex.ehr.dto.integration;

import lombok.Data;

@Data
public class StripeConfig {
    private String apiKey;
    private String webhookSecret;
    private String publishableKey;
}
