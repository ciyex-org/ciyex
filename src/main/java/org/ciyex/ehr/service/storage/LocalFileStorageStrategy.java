package org.ciyex.ehr.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.config.VaultikProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Fallback storage strategy that writes files to the local filesystem.
 * Used when ciyex-files (Vaultik) is not installed or storage_mode is "local".
 *
 * Folder structure: {localStoragePath}/{orgAlias}/{category}/{patientId}/{uuid}_{filename}
 */
@Component
@Slf4j
public class LocalFileStorageStrategy implements FileStorageStrategy {

    private final String basePath;

    public LocalFileStorageStrategy(VaultikProperties props) {
        this.basePath = props.getLocalStoragePath();
    }

    @Override
    public Map<String, Object> upload(MultipartFile file, String orgId, String patientId, String category) {
        String fileId = UUID.randomUUID().toString();
        String storedName = fileId + "_" + file.getOriginalFilename();
        String cat = (category != null && !category.isBlank()) ? category : "documents";

        Path dir = Path.of(basePath, orgId, cat);
        if (patientId != null && !patientId.isBlank()) {
            dir = dir.resolve(patientId);
        }

        try {
            Files.createDirectories(dir);
            Path filePath = dir.resolve(storedName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("Local file stored: {}", filePath);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", fileId);
            result.put("originalName", file.getOriginalFilename());
            result.put("contentType", file.getContentType());
            result.put("size", file.getSize());
            result.put("category", cat);
            result.put("s3Key", filePath.toString());
            result.put("downloadUrl", "/api/files-proxy/" + fileId + "/download");
            result.put("createdAt", LocalDateTime.now().toString());
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file locally: " + file.getOriginalFilename(), e);
        }
    }

    @Override
    public Map<String, Object> list(String orgId, String patientId, String category, int page, int size) {
        String cat = (category != null && !category.isBlank()) ? category : "documents";
        Path dir = Path.of(basePath, orgId, cat);
        if (patientId != null && !patientId.isBlank()) {
            dir = dir.resolve(patientId);
        }

        List<Map<String, Object>> files = new ArrayList<>();
        if (Files.exists(dir)) {
            try (var stream = Files.list(dir)) {
                var allFiles = stream.filter(Files::isRegularFile).sorted(Comparator.reverseOrder()).toList();
                int start = page * size;
                int end = Math.min(start + size, allFiles.size());

                for (int i = start; i < end; i++) {
                    Path f = allFiles.get(i);
                    String name = f.getFileName().toString();
                    String originalName = name.contains("_") ? name.substring(name.indexOf('_') + 1) : name;
                    String fileId = name.contains("_") ? name.substring(0, name.indexOf('_')) : name;

                    Map<String, Object> fileInfo = new LinkedHashMap<>();
                    fileInfo.put("id", fileId);
                    fileInfo.put("originalName", originalName);
                    fileInfo.put("contentType", Files.probeContentType(f));
                    fileInfo.put("size", Files.size(f));
                    fileInfo.put("category", cat);
                    fileInfo.put("downloadUrl", "/api/files-proxy/" + fileId + "/download");
                    fileInfo.put("createdAt", Files.getLastModifiedTime(f).toString());
                    files.add(fileInfo);
                }

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("files", files);
                result.put("page", page);
                result.put("size", size);
                result.put("totalElements", allFiles.size());
                result.put("totalPages", (int) Math.ceil((double) allFiles.size() / size));
                return result;
            } catch (IOException e) {
                log.error("Failed to list local files: {}", e.getMessage());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("files", files);
        result.put("page", page);
        result.put("size", size);
        result.put("totalElements", 0);
        result.put("totalPages", 0);
        return result;
    }

    @Override
    public DownloadResult download(String fileId, String orgId) {
        // Search for the file by UUID prefix in the org's directories
        Path orgDir = Path.of(basePath, orgId);
        if (!Files.exists(orgDir)) {
            throw new RuntimeException("File not found: " + fileId);
        }

        try {
            Path found = findFileById(orgDir, fileId);
            if (found == null) {
                throw new RuntimeException("File not found: " + fileId);
            }

            String name = found.getFileName().toString();
            String originalName = name.contains("_") ? name.substring(name.indexOf('_') + 1) : name;
            String contentType = Files.probeContentType(found);

            return new DownloadResult(
                    Files.newInputStream(found),
                    contentType != null ? contentType : "application/octet-stream",
                    originalName,
                    Files.size(found)
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to download local file: " + fileId, e);
        }
    }

    @Override
    public void delete(String fileId, String orgId) {
        Path orgDir = Path.of(basePath, orgId);
        if (!Files.exists(orgDir)) return;

        try {
            Path found = findFileById(orgDir, fileId);
            if (found != null) {
                Files.deleteIfExists(found);
                log.info("Local file deleted: {}", found);
            }
        } catch (IOException e) {
            log.error("Failed to delete local file {}: {}", fileId, e.getMessage());
        }
    }

    @Override
    public String getPresignedUrl(String fileId, String orgId, int expirySeconds) {
        // Local storage doesn't support presigned URLs — return download endpoint
        return "/api/files-proxy/" + fileId + "/download";
    }

    // ========================
    // Key-based operations
    // ========================

    @Override
    public void uploadByKey(byte[] data, String key, String contentType, String orgId,
                            String sourceService, String referenceId, String originalFilename) {
        Path filePath = Path.of(basePath, key);
        try {
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, data);
            log.info("Local file stored by key: {}", filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file locally at key: " + key, e);
        }
    }

    @Override
    public String getPresignedUrlByKey(String key, int expirySeconds) {
        return "/api/files-proxy/by-key/download?key=" + key;
    }

    @Override
    public boolean existsByKey(String key) {
        return Files.exists(Path.of(basePath, key));
    }

    @Override
    public long getSizeByKey(String key) {
        Path filePath = Path.of(basePath, key);
        try {
            return Files.exists(filePath) ? Files.size(filePath) : 0;
        } catch (IOException e) {
            return 0;
        }
    }

    @Override
    public void deleteByKey(String key) {
        try {
            Path filePath = Path.of(basePath, key);
            Files.deleteIfExists(filePath);
            log.info("Local file deleted by key: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to delete local file by key {}: {}", key, e.getMessage());
        }
    }

    @Override
    public byte[] downloadByKey(String key) {
        Path filePath = Path.of(basePath, key);
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to download local file by key: " + key, e);
        }
    }

    private Path findFileById(Path dir, String fileId) throws IOException {
        try (var walk = Files.walk(dir)) {
            return walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().startsWith(fileId + "_"))
                    .findFirst()
                    .orElse(null);
        }
    }
}
