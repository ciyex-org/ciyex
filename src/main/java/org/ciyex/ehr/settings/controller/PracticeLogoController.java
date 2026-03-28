package org.ciyex.ehr.settings.controller;

import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.settings.entity.PracticeLogo;
import org.ciyex.ehr.settings.repository.PracticeLogoRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Base64;
import java.util.Map;

@PreAuthorize("hasAuthority('SCOPE_user/Organization.read')")
@RestController
@RequestMapping("/api/practice-logo")
@Slf4j
public class PracticeLogoController {

    private final PracticeLogoRepository repository;

    public PracticeLogoController(PracticeLogoRepository repository) {
        this.repository = repository;
    }

    private String getOrgId() {
        RequestContext ctx = RequestContext.get();
        return ctx != null ? ctx.getOrgName() : "default";
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> getLogo() {
        String orgId = getOrgId();
        return repository.findByOrgId(orgId)
                .map(logo -> ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                        .success(true)
                        .message("Logo found")
                        .data(Map.of(
                                "logoData", logo.getLogoData(),
                                "contentType", logo.getContentType() != null ? logo.getContentType() : "",
                                "fileName", logo.getFileName() != null ? logo.getFileName() : ""
                        ))
                        .build()))
                .orElse(ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                        .success(true)
                        .message("No logo found")
                        .data(null)
                        .build()));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadLogo(
            @RequestParam("file") MultipartFile file) {
        try {
            String orgId = getOrgId();
            String contentType = file.getContentType();

            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Only image files are allowed")
                        .build());
            }

            if (file.getSize() > 2 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("File size must be under 2MB")
                        .build());
            }

            String base64 = Base64.getEncoder().encodeToString(file.getBytes());
            String dataUri = "data:" + contentType + ";base64," + base64;

            PracticeLogo logo = repository.findByOrgId(orgId)
                    .orElse(PracticeLogo.builder().orgId(orgId).build());

            logo.setLogoData(dataUri);
            logo.setContentType(contentType);
            logo.setFileName(file.getOriginalFilename());
            repository.save(logo);

            log.info("Practice logo saved for org {}", orgId);

            return ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                    .success(true)
                    .message("Logo uploaded successfully")
                    .data(Map.of(
                            "logoData", dataUri,
                            "contentType", contentType,
                            "fileName", file.getOriginalFilename() != null ? file.getOriginalFilename() : ""
                    ))
                    .build());
        } catch (Exception e) {
            log.error("Failed to upload logo", e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Map<String, String>>builder()
                    .success(false)
                    .message("Failed to upload logo: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteLogo() {
        String orgId = getOrgId();
        repository.deleteByOrgId(orgId);
        log.info("Practice logo deleted for org {}", orgId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Logo deleted successfully")
                .build());
    }
}
