package org.ciyex.ehr.settings.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.settings.entity.UiColorConfig;
import org.ciyex.ehr.settings.service.UiColorConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@PreAuthorize("hasAuthority('SCOPE_user/Organization.read')")
@RestController
@RequestMapping("/api/ui-colors")
@RequiredArgsConstructor
@Slf4j
public class UiColorConfigController {

    private final UiColorConfigService colorService;

    private String getOrgId() {
        RequestContext ctx = RequestContext.get();
        return ctx != null ? ctx.getOrgName() : "default";
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UiColorConfig>>> getAll() {
        List<UiColorConfig> configs = colorService.findAll(getOrgId());
        return ResponseEntity.ok(ApiResponse.<List<UiColorConfig>>builder()
                .success(true)
                .message("Color configs retrieved")
                .data(configs)
                .build());
    }

    @GetMapping("/{category}")
    public ResponseEntity<ApiResponse<List<UiColorConfig>>> getByCategory(
            @PathVariable String category) {
        List<UiColorConfig> configs = colorService.findByCategory(getOrgId(), category);
        return ResponseEntity.ok(ApiResponse.<List<UiColorConfig>>builder()
                .success(true)
                .message("Color configs retrieved for " + category)
                .data(configs)
                .build());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<ApiResponse<List<UiColorConfig>>> bulkSave(
            @RequestBody List<UiColorConfigService.ColorConfigRequest> requests) {
        String orgId = getOrgId();
        log.info("Saving {} color configs for org {}", requests.size(), orgId);

        List<UiColorConfig> all = colorService.bulkSave(orgId, requests);
        return ResponseEntity.ok(ApiResponse.<List<UiColorConfig>>builder()
                .success(true)
                .message("Color configs saved successfully")
                .data(all)
                .build());
    }
}
