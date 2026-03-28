package org.ciyex.ehr.controller;

import lombok.RequiredArgsConstructor;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.service.storage.FileStorageStrategy;
import org.ciyex.ehr.service.storage.FileStorageStrategyResolver;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;

/**
 * Proxy controller for file storage operations.
 * Routes requests to either Vaultik (ciyex-files) or local storage
 * based on the org's app installation config.
 */
@PreAuthorize("hasAuthority('SCOPE_user/DocumentReference.read')")
@RestController
@RequestMapping("/api/files-proxy")
@RequiredArgsConstructor
public class FileStorageProxyController {

    private final FileStorageStrategyResolver strategyResolver;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('SCOPE_user/DocumentReference.write')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "patientId", required = false) String patientId,
            @RequestParam(value = "category", required = false) String category) {

        String orgId = getOrgId();
        FileStorageStrategy strategy = strategyResolver.resolve(orgId);
        Map<String, Object> result = strategy.upload(file, orgId, patientId, category);
        return ResponseEntity.ok(ApiResponse.ok("File uploaded", result));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<Map<String, Object>>> list(
            @RequestParam(value = "patientId", required = false) String patientId,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        String orgId = getOrgId();
        FileStorageStrategy strategy = strategyResolver.resolve(orgId);
        Map<String, Object> result = strategy.list(orgId, patientId, category, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Files listed", result));
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable String fileId) {
        String orgId = getOrgId();
        FileStorageStrategy strategy = strategyResolver.resolve(orgId);
        FileStorageStrategy.DownloadResult result = strategy.download(fileId, orgId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.fileName() + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(result.size()))
                .body(new InputStreamResource(result.inputStream()));
    }

    @DeleteMapping("/{fileId}")
    @PreAuthorize("hasAuthority('SCOPE_user/DocumentReference.write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String fileId) {
        String orgId = getOrgId();
        FileStorageStrategy strategy = strategyResolver.resolve(orgId);
        strategy.delete(fileId, orgId);
        return ResponseEntity.ok(ApiResponse.ok("File deleted", null));
    }

    @GetMapping("/{fileId}/presigned-url")
    public ResponseEntity<ApiResponse<Map<String, String>>> presignedUrl(
            @PathVariable String fileId,
            @RequestParam(value = "expiry", defaultValue = "3600") int expiry) {

        String orgId = getOrgId();
        FileStorageStrategy strategy = strategyResolver.resolve(orgId);
        String url = strategy.getPresignedUrl(fileId, orgId, expiry);
        return ResponseEntity.ok(ApiResponse.ok("Presigned URL generated", Map.of("url", url)));
    }

    // ========================
    // Key-based operations (used by marketplace apps like telehealth via SDK)
    // ========================

    @PostMapping(value = "/store-bytes")
    @PreAuthorize("hasAuthority('SCOPE_user/DocumentReference.write')")
    public ResponseEntity<ApiResponse<Void>> storeBytes(
            @RequestBody byte[] data,
            @RequestHeader("Content-Type") String contentType,
            @RequestHeader("X-File-Path") String key,
            @RequestHeader(value = "X-Source-Service", required = false) String sourceService,
            @RequestHeader(value = "X-Reference-Id", required = false) String referenceId,
            @RequestHeader(value = "X-Org-Id", required = false) String orgId,
            @RequestHeader(value = "X-Original-Filename", required = false) String originalFilename) {

        String resolvedOrgId = orgId != null && !orgId.isBlank() ? orgId : getOrgId();
        FileStorageStrategy strategy = strategyResolver.resolve(resolvedOrgId);
        strategy.uploadByKey(data, key, contentType, resolvedOrgId, sourceService, referenceId, originalFilename);
        return ResponseEntity.ok(ApiResponse.ok("File stored", null));
    }

    @GetMapping("/by-key/presigned-url")
    public ResponseEntity<ApiResponse<Map<String, String>>> presignedUrlByKey(
            @RequestParam String key,
            @RequestParam(value = "expiry", defaultValue = "3600") int expiry) {

        String orgId = getOrgId();
        FileStorageStrategy strategy = strategyResolver.resolve(orgId);
        String url = strategy.getPresignedUrlByKey(key, expiry);
        return ResponseEntity.ok(ApiResponse.ok("Presigned URL generated", Map.of("url", url)));
    }

    @RequestMapping(value = "/by-key/exists", method = RequestMethod.HEAD)
    public ResponseEntity<Void> existsByKey(@RequestParam String key) {
        String orgId = getOrgId();
        FileStorageStrategy strategy = strategyResolver.resolve(orgId);
        boolean exists = strategy.existsByKey(key);
        return exists ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/by-key/size")
    public ResponseEntity<ApiResponse<Map<String, Long>>> sizeByKey(@RequestParam String key) {
        String orgId = getOrgId();
        FileStorageStrategy strategy = strategyResolver.resolve(orgId);
        long size = strategy.getSizeByKey(key);
        return ResponseEntity.ok(ApiResponse.ok("File size", Map.of("size", size)));
    }

    @DeleteMapping("/by-key")
    @PreAuthorize("hasAuthority('SCOPE_user/DocumentReference.write')")
    public ResponseEntity<ApiResponse<Void>> deleteByKey(@RequestParam String key) {
        String orgId = getOrgId();
        FileStorageStrategy strategy = strategyResolver.resolve(orgId);
        strategy.deleteByKey(key);
        return ResponseEntity.ok(ApiResponse.ok("File deleted", null));
    }

    @GetMapping("/by-key/download")
    public ResponseEntity<byte[]> downloadByKey(@RequestParam String key) {
        String orgId = getOrgId();
        FileStorageStrategy strategy = strategyResolver.resolve(orgId);
        byte[] data = strategy.downloadByKey(key);
        String filename = key.substring(key.lastIndexOf('/') + 1);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(data);
    }

    private String getOrgId() {
        RequestContext ctx = RequestContext.get();
        return ctx != null ? ctx.getOrgName() : null;
    }
}
