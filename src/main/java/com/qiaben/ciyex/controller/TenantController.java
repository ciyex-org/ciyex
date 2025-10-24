package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.service.TenantProvisionService;
import com.qiaben.ciyex.service.KeycloakAuthService;
import com.qiaben.ciyex.service.TenantAccessService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.Map;

import com.qiaben.ciyex.dto.ApiResponse;

@Slf4j
@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantProvisionService provisionService;
    
    @Autowired
    private KeycloakAuthService keycloakAuthService;
    
    @Autowired
    private TenantAccessService tenantAccessService;

    public TenantController(TenantProvisionService provisionService) {
        this.provisionService = provisionService;
    }
    
    /**
     * Get accessible tenants for the current user
     * Used by frontend to show practice selection page
     */
    @GetMapping("/accessible")
    public ResponseEntity<ApiResponse<AccessibleTenantsResponse>> getAccessibleTenants(
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            String token = authHeader.substring(7); // Remove "Bearer "
            List<String> groups = keycloakAuthService.extractGroupsFromToken(token);
            
            boolean hasFullAccess = tenantAccessService.hasAccessToAllTenants(groups);
            List<String> tenants = tenantAccessService.getAccessibleTenants(groups);
            
            // If user has full access, they can access any tenant (return empty list to indicate "ALL")
            boolean requiresSelection = (hasFullAccess || tenants.size() > 1);
            
            AccessibleTenantsResponse data = new AccessibleTenantsResponse(
                hasFullAccess,
                hasFullAccess ? List.of() : tenants, // Empty list means "ALL" for full access users
                requiresSelection
            );
            
            ApiResponse<AccessibleTenantsResponse> response = ApiResponse.<AccessibleTenantsResponse>builder()
                    .success(true)
                    .message("Accessible tenants retrieved")
                    .data(data)
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get accessible tenants", e);
            ApiResponse<AccessibleTenantsResponse> response = ApiResponse.<AccessibleTenantsResponse>builder()
                    .success(false)
                    .message("Failed to get accessible tenants: " + e.getMessage())
                    .data(null)
                    .build();
            return ResponseEntity.status(500).body(response);
        }
    }
    
    @Data
    public static class AccessibleTenantsResponse {
        private final boolean hasFullAccess;
        private final List<String> tenants;
        private final boolean requiresSelection;
        
        public AccessibleTenantsResponse(boolean hasFullAccess, List<String> tenants, boolean requiresSelection) {
            this.hasFullAccess = hasFullAccess;
            this.tenants = tenants;
            this.requiresSelection = requiresSelection;
        }
    }

    @PostMapping("/{orgId}")
    public ResponseEntity<?> createTenant(@PathVariable String orgId, @RequestBody(required = false) CreateTenantRequest body) {
        String template = body != null ? body.getTemplatePath() : null;
        String sourceSchema = body != null ? body.getSourceSchema() : null;
        log.info("Provision request received for orgId={} template={} sourceSchema={}", orgId, template, sourceSchema);

        try {
            provisionService.provisionTenantFromTemplate(orgId, template, sourceSchema);
            ProvisionResponse resp = new ProvisionResponse(true, "Tenant provisioned", null);
            ApiResponse<ProvisionResponse> api = ApiResponse.<ProvisionResponse>builder()
                    .success(true)
                    .message("Tenant provisioned")
                    .data(resp)
                    .build();
            return ResponseEntity.created(URI.create("/api/tenants/" + orgId)).body(api);
        } catch (Exception e) {
            log.error("Provisioning failed for orgId={}", orgId, e);
            String errMsg = e.getMessage() != null ? e.getMessage() : e.toString();
            ProvisionResponse resp = new ProvisionResponse(false, "Provisioning failed", errMsg);
            ApiResponse<ProvisionResponse> api = ApiResponse.<ProvisionResponse>builder()
                    .success(false)
                    .message("Provisioning failed")
                    .data(resp)
                    .build();
            return ResponseEntity.status(500).body(api);
        }
    }

    @Data
    public static class CreateTenantRequest {
        private String templatePath;
        private String sourceSchema;
    }

    @Data
    public static class ProvisionResponse {
        private final boolean success;
        private final String message;
        private final String error;

        public ProvisionResponse(boolean success, String message, String error) {
            this.success = success;
            this.message = message;
            this.error = error;
        }
    }

    @PostMapping("/{orgId}/upload")
    public ResponseEntity<?> uploadAndApply(@PathVariable String orgId,
                                           @RequestParam(value = "sourceSchema", required = false) String sourceSchema,
                                           @RequestPart("files") MultipartFile[] files) {
        log.info("Upload provisioning request received for orgId={} filesCount={} sourceSchema={}", orgId, files == null ? 0 : files.length, sourceSchema);

        if (files == null || files.length == 0) {
            ApiResponse<ProvisionResponse> resp = ApiResponse.<ProvisionResponse>builder()
                    .success(false)
                    .message("No files uploaded")
                    .data(new ProvisionResponse(false, "No files uploaded", ""))
                    .build();
            return ResponseEntity.badRequest().body(resp);
        }

        java.nio.file.Path tempDir = null;
        try {
            tempDir = java.nio.file.Files.createTempDirectory("tenant-upload-" + orgId + "-");
            for (MultipartFile mf : files) {
                String fname = mf.getOriginalFilename();
                if (fname == null || fname.isBlank()) {
                    fname = "upload-" + System.currentTimeMillis() + ".sql";
                }
                java.nio.file.Path out = tempDir.resolve(fname);
                mf.transferTo(out);
            }

            String templatePath;
            if (files.length == 1) {
                String name = files[0].getOriginalFilename();
                if (name == null || name.isBlank()) {
                    // find any file in the temp dir (should be the one we just wrote)
                    try (var s = java.nio.file.Files.list(tempDir)) {
                        name = s.findFirst().orElseThrow(() -> new RuntimeException("Uploaded file missing"))
                                .getFileName().toString();
                    }
                }
                templatePath = tempDir.resolve(name).toString();
            } else {
                templatePath = tempDir.toString();
            }

            provisionService.provisionTenantFromTemplate(orgId, templatePath, sourceSchema);

            // cleanup temp files
            try (var stream = java.nio.file.Files.walk(tempDir)) {
                stream.sorted(java.util.Comparator.reverseOrder()).map(java.nio.file.Path::toFile).forEach(java.io.File::delete);
            } catch (Exception ignore) {
                // ignore cleanup errors
            }

            ProvisionResponse pr = new ProvisionResponse(true, "Tenant provisioned", null);
            ApiResponse<ProvisionResponse> api = ApiResponse.<ProvisionResponse>builder().success(true).message("Tenant provisioned").data(pr).build();
            return ResponseEntity.created(URI.create("/api/tenants/" + orgId)).body(api);
        } catch (Exception e) {
            log.error("Upload provisioning failed for orgId={}", orgId, e);
            // leave temp files for inspection
            String errMsg = e.getMessage() != null ? e.getMessage() : e.toString();
            ProvisionResponse resp = new ProvisionResponse(false, "Provisioning failed", errMsg);
            ApiResponse<ProvisionResponse> api = ApiResponse.<ProvisionResponse>builder().success(false).message("Provisioning failed").data(resp).build();
            return ResponseEntity.status(500).body(api);
        }
    }
}
