package com.qiaben.ciyex.dto.integration;

import lombok.Data;

@Data
public class StorageConfig {
    private S3 s3;

    @Data
    public static class S3 {
        private String bucket;
        private String accessKey;
        private String secretKey;
        private String region;
    }
}