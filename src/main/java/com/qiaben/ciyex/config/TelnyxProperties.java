package com.qiaben.ciyex.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Data
@ConfigurationProperties(prefix = "telnyx")

public class TelnyxProperties {
    private String apiBaseUrl;
    private String apiKey;
}
