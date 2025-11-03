package com.qiaben.ciyex.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

/**
 * Configuration properties for S3 storage integration
 */
@Configuration
@ConfigurationProperties(prefix = "ciyex.integrations.storage.s3")
@Data
public class S3Configuration {

    /**
     * S3 bucket name
     */
    private String bucket;

    /**
     * AWS region for S3
     */
    private String region;

    /**
     * AWS access key ID
     */
    private String accessKey;

    /**
     * AWS secret access key
     */
    private String secretKey;
}