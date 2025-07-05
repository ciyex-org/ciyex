package com.qiaben.ciyex.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "openemr.oauth")
public class OpenEmrOAuthProperties {
    private String privateKeyPath;
    private String clientId;
    private String audience;
    private String kid;
    private String tokenUrl;
    private String scope;
}
