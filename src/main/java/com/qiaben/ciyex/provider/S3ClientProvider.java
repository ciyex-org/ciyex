package com.qiaben.ciyex.provider;

import com.qiaben.ciyex.dto.integration.IntegrationKey;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.dto.integration.StorageConfig;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Component
@Slf4j
public class S3ClientProvider {

    private final OrgIntegrationConfigProvider configProvider;

    public S3ClientProvider(OrgIntegrationConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    public S3Client getForCurrentOrg() {
        Long orgId = RequestContext.get().getOrgId();
        if (orgId == null) {
            throw new IllegalStateException("No orgId available in request context");
        }
        StorageConfig config = configProvider.getForCurrentOrg(IntegrationKey.DOCUMENT_STORAGE);
        if (config == null || config.getS3() == null) {
            throw new IllegalStateException("S3 configuration missing for orgId: " + orgId);
        }
        StorageConfig.S3 s3Config = config.getS3();
        AwsBasicCredentials credentials = AwsBasicCredentials.create(s3Config.getAccessKey(), s3Config.getSecretKey());
        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(s3Config.getRegion()))
                .build();
    }

    public String getBucketForCurrentOrg() {
        Long orgId = RequestContext.get().getOrgId();
        StorageConfig config = configProvider.getForCurrentOrg(IntegrationKey.DOCUMENT_STORAGE);
        return config.getS3().getBucket();
    }
}