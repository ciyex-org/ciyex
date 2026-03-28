package org.ciyex.ehr.service.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.marketplace.service.AppInstallationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Storage strategy that delegates to the ciyex-files (Vaultik) microservice.
 * The service URL is discovered from the app installation config (set by marketplace webhook).
 * Passes S3 override headers if the org has custom S3 config.
 */
@Component
@Slf4j
public class VaultikStorageStrategy implements FileStorageStrategy {

    private final AppInstallationService appInstallationService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, RestClient> clientCache = new ConcurrentHashMap<>();

    public VaultikStorageStrategy(AppInstallationService appInstallationService) {
        this.appInstallationService = appInstallationService;
    }

    @Override
    public Map<String, Object> upload(MultipartFile file, String orgId, String patientId, String category) {
        String path = orgId + "/documents/" + (patientId != null ? patientId + "/" : "") +
                java.util.UUID.randomUUID() + "_" + file.getOriginalFilename();

        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", file.getResource());
            builder.part("path", path);
            builder.part("orgId", orgId);
            builder.part("sourceService", "ciyex-ehr");
            if (patientId != null) {
                builder.part("referenceId", "Patient/" + patientId);
            }

            var spec = getClient(orgId).post()
                    .uri("/api/files/store")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(builder.build());

            addAuthHeader(spec);
            addS3OverrideHeaders(spec, orgId);

            String body = spec.retrieve().body(String.class);
            return objectMapper.readValue(body, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Vaultik upload failed for org {}: {}", orgId, e.getMessage());
            throw new RuntimeException("File upload to Vaultik failed", e);
        }
    }

    @Override
    public Map<String, Object> list(String orgId, String patientId, String category, int page, int size) {
        try {
            String uri = "/api/files?orgId=" + orgId + "&page=" + page + "&size=" + size;
            if (category != null && !category.isBlank()) {
                uri += "&category=" + category;
            }

            var spec = getClient(orgId).get().uri(uri);
            addAuthHeader(spec);

            String body = spec.retrieve().body(String.class);
            return objectMapper.readValue(body, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Vaultik list failed for org {}: {}", orgId, e.getMessage());
            throw new RuntimeException("File listing from Vaultik failed", e);
        }
    }

    @Override
    public DownloadResult download(String fileId, String orgId) {
        try {
            var spec = getClient(orgId).get().uri("/api/files/{id}/download", fileId);
            addAuthHeader(spec);
            addS3OverrideHeaders(spec, orgId);

            ResponseEntity<byte[]> response = spec.retrieve().toEntity(byte[].class);
            byte[] bytes = response.getBody();
            String contentType = response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
            String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
            String fileName = extractFileName(disposition);

            return new DownloadResult(
                    new ByteArrayInputStream(bytes != null ? bytes : new byte[0]),
                    contentType != null ? contentType : "application/octet-stream",
                    fileName != null ? fileName : "file",
                    bytes != null ? bytes.length : 0
            );
        } catch (Exception e) {
            log.error("Vaultik download failed for file {}: {}", fileId, e.getMessage());
            throw new RuntimeException("File download from Vaultik failed", e);
        }
    }

    @Override
    public void delete(String fileId, String orgId) {
        try {
            var spec = getClient(orgId).delete().uri("/api/files/{id}", fileId);
            addAuthHeader(spec);
            addS3OverrideHeaders(spec, orgId);

            spec.retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.error("Vaultik delete failed for file {}: {}", fileId, e.getMessage());
            throw new RuntimeException("File delete from Vaultik failed", e);
        }
    }

    @Override
    public String getPresignedUrl(String fileId, String orgId, int expirySeconds) {
        try {
            // First get file metadata to find the S3 key
            var infoSpec = getClient(orgId).get().uri("/api/files/{id}", fileId);
            addAuthHeader(infoSpec);
            String infoBody = infoSpec.retrieve().body(String.class);
            Map<String, Object> info = objectMapper.readValue(infoBody, new TypeReference<>() {});
            String s3Key = (String) info.get("s3Key");

            if (s3Key == null) {
                throw new RuntimeException("No S3 key found for file: " + fileId);
            }

            var spec = getClient(orgId).get()
                    .uri("/api/files/by-key/presigned-url?key={key}&expiry={expiry}", s3Key, expirySeconds);
            addAuthHeader(spec);
            addS3OverrideHeaders(spec, orgId);

            String body = spec.retrieve().body(String.class);
            Map<String, Object> result = objectMapper.readValue(body, new TypeReference<>() {});
            return (String) result.get("url");
        } catch (Exception e) {
            log.error("Vaultik presigned URL failed for file {}: {}", fileId, e.getMessage());
            throw new RuntimeException("Presigned URL from Vaultik failed", e);
        }
    }

    // ========================
    // Key-based operations
    // ========================

    @Override
    public void uploadByKey(byte[] data, String key, String contentType, String orgId,
                            String sourceService, String referenceId, String originalFilename) {
        try {
            var spec = getClient(orgId).post()
                    .uri("/api/files/store-bytes")
                    .header("Content-Type", contentType)
                    .header("X-File-Path", key)
                    .header("X-Source-Service", sourceService != null ? sourceService : "unknown")
                    .header("X-Org-Id", orgId != null ? orgId : "")
                    .body(data);

            if (referenceId != null) spec.header("X-Reference-Id", referenceId);
            if (originalFilename != null) spec.header("X-Original-Filename", originalFilename);

            addAuthHeader(spec);
            addS3OverrideHeaders(spec, orgId);

            spec.retrieve().toBodilessEntity();
            log.debug("Vaultik uploadByKey: key={}", key);
        } catch (Exception e) {
            log.error("Vaultik uploadByKey failed for key {}: {}", key, e.getMessage());
            throw new RuntimeException("File upload to Vaultik failed", e);
        }
    }

    @Override
    public String getPresignedUrlByKey(String key, int expirySeconds) {
        String orgId = resolveCurrentOrgId();
        try {
            var spec = getClient(orgId).get()
                    .uri("/api/files/by-key/presigned-url?key={key}&expiry={expiry}", key, expirySeconds);
            addAuthHeader(spec);
            addS3OverrideHeaders(spec, orgId);

            String body = spec.retrieve().body(String.class);
            Map<String, Object> result = objectMapper.readValue(body, new TypeReference<>() {});
            return (String) result.get("url");
        } catch (Exception e) {
            log.error("Vaultik getPresignedUrlByKey failed for key {}: {}", key, e.getMessage());
            throw new RuntimeException("Presigned URL from Vaultik failed", e);
        }
    }

    @Override
    public boolean existsByKey(String key) {
        String orgId = resolveCurrentOrgId();
        try {
            getClient(orgId).head()
                    .uri("/api/files/by-key/exists?key={key}", key)
                    .header(HttpHeaders.AUTHORIZATION, resolveAuthToken())
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public long getSizeByKey(String key) {
        String orgId = resolveCurrentOrgId();
        try {
            var spec = getClient(orgId).get()
                    .uri("/api/files/by-key/size?key={key}", key);
            addAuthHeader(spec);
            addS3OverrideHeaders(spec, orgId);

            String body = spec.retrieve().body(String.class);
            Map<String, Object> result = objectMapper.readValue(body, new TypeReference<>() {});
            return ((Number) result.get("size")).longValue();
        } catch (Exception e) {
            log.error("Vaultik getSizeByKey failed for key {}: {}", key, e.getMessage());
            return 0;
        }
    }

    @Override
    public void deleteByKey(String key) {
        String orgId = resolveCurrentOrgId();
        try {
            var spec = getClient(orgId).delete()
                    .uri("/api/files/by-key?key={key}", key);
            addAuthHeader(spec);
            addS3OverrideHeaders(spec, orgId);

            spec.retrieve().toBodilessEntity();
            log.debug("Vaultik deleteByKey: key={}", key);
        } catch (Exception e) {
            log.error("Vaultik deleteByKey failed for key {}: {}", key, e.getMessage());
            throw new RuntimeException("File delete from Vaultik failed", e);
        }
    }

    @Override
    public byte[] downloadByKey(String key) {
        String orgId = resolveCurrentOrgId();
        try {
            var spec = getClient(orgId).get()
                    .uri("/api/files/by-key/download?key={key}", key);
            addAuthHeader(spec);
            addS3OverrideHeaders(spec, orgId);

            return spec.retrieve().body(byte[].class);
        } catch (Exception e) {
            log.error("Vaultik downloadByKey failed for key {}: {}", key, e.getMessage());
            throw new RuntimeException("File download from Vaultik failed", e);
        }
    }

    // ========================
    // Private helpers
    // ========================

    private String resolveCurrentOrgId() {
        RequestContext ctx = RequestContext.get();
        return ctx != null ? ctx.getOrgName() : null;
    }

    private String resolveAuthToken() {
        RequestContext ctx = RequestContext.get();
        return ctx != null && ctx.getAuthToken() != null ? ctx.getAuthToken() : "";
    }

    /**
     * Get a RestClient for the Vaultik service URL discovered from the org's app installation config.
     * Clients are cached by URL to avoid re-creation on every request.
     */
    private RestClient getClient(String orgId) {
        String serviceUrl = resolveServiceUrl(orgId);
        return clientCache.computeIfAbsent(serviceUrl, url ->
                RestClient.builder().baseUrl(url).build());
    }

    /**
     * Resolve the Vaultik service URL from the org's app installation config.
     * The service_url is set by the marketplace webhook when the app is installed.
     */
    private String resolveServiceUrl(String orgId) {
        var installation = appInstallationService.getInstallation(orgId, "vaultik");
        if (installation == null || installation.getConfig() == null) {
            throw new IllegalStateException("Vaultik app not installed for org: " + orgId);
        }

        Object serviceUrl = installation.getConfig().get("service_url");
        if (serviceUrl == null || serviceUrl.toString().isBlank()) {
            throw new IllegalStateException(
                    "Vaultik service_url not configured for org: " + orgId +
                    ". Re-install the app from the marketplace to set the service URL.");
        }

        return serviceUrl.toString();
    }

    private void addAuthHeader(RestClient.RequestHeadersSpec<?> spec) {
        RequestContext ctx = RequestContext.get();
        if (ctx != null && ctx.getAuthToken() != null) {
            spec.header(HttpHeaders.AUTHORIZATION, ctx.getAuthToken());
        }
    }

    @SuppressWarnings("unchecked")
    private void addS3OverrideHeaders(RestClient.RequestHeadersSpec<?> spec, String orgId) {
        try {
            var installation = appInstallationService.getInstallation(orgId, "vaultik");
            if (installation == null || installation.getConfig() == null) return;

            Map<String, Object> config = installation.getConfig();
            Object customS3Obj = config.get("custom_s3");
            if (!(customS3Obj instanceof Map)) return;

            Map<String, String> customS3 = (Map<String, String>) customS3Obj;
            String endpoint = customS3.get("endpoint");
            String accessKey = customS3.get("access_key");
            String secretKey = customS3.get("secret_key");

            // Only add overrides if all required fields are present
            if (endpoint != null && !endpoint.isBlank()
                    && accessKey != null && !accessKey.isBlank()
                    && secretKey != null && !secretKey.isBlank()) {
                spec.header("X-S3-Override-Endpoint", endpoint);
                spec.header("X-S3-Override-Access-Key", accessKey);
                spec.header("X-S3-Override-Secret-Key", secretKey);

                String region = customS3.get("region");
                if (region != null && !region.isBlank()) {
                    spec.header("X-S3-Override-Region", region);
                }
                String bucket = customS3.get("bucket");
                if (bucket != null && !bucket.isBlank()) {
                    spec.header("X-S3-Override-Bucket", bucket);
                }

                log.debug("Added S3 override headers for org {} (endpoint: {})", orgId, endpoint);
            }
        } catch (Exception e) {
            log.debug("Could not read Vaultik config for org {}: {}", orgId, e.getMessage());
        }
    }

    private String extractFileName(String contentDisposition) {
        if (contentDisposition == null) return null;
        int idx = contentDisposition.indexOf("filename=\"");
        if (idx < 0) return null;
        int start = idx + "filename=\"".length();
        int end = contentDisposition.indexOf('"', start);
        return end > start ? contentDisposition.substring(start, end) : null;
    }
}
