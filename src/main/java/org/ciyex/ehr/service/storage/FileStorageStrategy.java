package org.ciyex.ehr.service.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;

/**
 * Strategy interface for file storage operations.
 * Implementations: VaultikStorageStrategy (ciyex-files service) and LocalFileStorageStrategy (filesystem).
 */
public interface FileStorageStrategy {

    /**
     * Upload a file and return metadata as a map.
     */
    Map<String, Object> upload(MultipartFile file, String orgId, String patientId, String category);

    /**
     * List files for an org, optionally filtered by patientId and category.
     */
    Map<String, Object> list(String orgId, String patientId, String category, int page, int size);

    /**
     * Download file content by file ID.
     */
    DownloadResult download(String fileId, String orgId);

    /**
     * Delete a file by ID.
     */
    void delete(String fileId, String orgId);

    /**
     * Get a presigned URL for direct download (not all strategies support this).
     */
    String getPresignedUrl(String fileId, String orgId, int expirySeconds);

    record DownloadResult(InputStream inputStream, String contentType, String fileName, long size) {}

    // ========================
    // Key-based operations (used by services like telehealth that manage their own key structure)
    // ========================

    /**
     * Upload raw bytes to a specific key path.
     */
    void uploadByKey(byte[] data, String key, String contentType, String orgId,
                     String sourceService, String referenceId, String originalFilename);

    /**
     * Get a presigned download URL for a specific key.
     */
    String getPresignedUrlByKey(String key, int expirySeconds);

    /**
     * Check if a file exists at the given key.
     */
    boolean existsByKey(String key);

    /**
     * Get the size of a file at the given key.
     */
    long getSizeByKey(String key);

    /**
     * Delete a file by key.
     */
    void deleteByKey(String key);

    /**
     * Download file bytes by key.
     */
    byte[] downloadByKey(String key);
}
