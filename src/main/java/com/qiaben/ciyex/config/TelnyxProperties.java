package com.qiaben.ciyex.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Data
@Configuration
@ConfigurationProperties(prefix = "telnyx")
@Primary // Mark this as the primary bean

public class TelnyxProperties {
    private String apiBaseUrl;
    private String apiKey;
}
