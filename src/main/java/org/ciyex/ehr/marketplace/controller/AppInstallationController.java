package org.ciyex.ehr.marketplace.controller;

import lombok.RequiredArgsConstructor;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.marketplace.dto.AppInstallationResponse;
import org.ciyex.ehr.marketplace.dto.InstallAppRequest;
import org.ciyex.ehr.marketplace.dto.LaunchRequest;
import org.ciyex.ehr.marketplace.service.AppInstallationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

@PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
@RestController
@RequestMapping("/api/app-installations")
@RequiredArgsConstructor
public class AppInstallationController {

    private final AppInstallationService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AppInstallationResponse>>> getInstalledApps() {
        String orgId = RequestContext.get().getOrgName();
        var apps = service.getInstalledApps(orgId);
        return ResponseEntity.ok(ApiResponse.ok("Installed apps retrieved", apps));
    }

    @GetMapping("/{appSlug}")
    public ResponseEntity<ApiResponse<AppInstallationResponse>> getInstallation(@PathVariable String appSlug) {
        String orgId = RequestContext.get().getOrgName();
        var installation = service.getInstallation(orgId, appSlug);
        if (installation == null) {
            return ResponseEntity.ok(ApiResponse.ok("App not installed", null));
        }
        return ResponseEntity.ok(ApiResponse.ok("Installation retrieved", installation));
    }

    @GetMapping("/{appSlug}/installed")
    public ResponseEntity<ApiResponse<Boolean>> isInstalled(@PathVariable String appSlug) {
        String orgId = RequestContext.get().getOrgName();
        boolean installed = service.isInstalled(orgId, appSlug);
        return ResponseEntity.ok(ApiResponse.ok("Check complete", installed));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AppInstallationResponse>> installApp(
            @RequestBody InstallAppRequest request,
            Authentication authentication) {
        String orgId = RequestContext.get().getOrgName();
        String installedBy = authentication != null ? authentication.getName() : "system";
        var installation = service.installApp(orgId, installedBy, request);
        return ResponseEntity.ok(ApiResponse.ok("App installed", installation));
    }

    @DeleteMapping("/{appSlug}")
    public ResponseEntity<ApiResponse<Void>> uninstallApp(@PathVariable String appSlug) {
        String orgId = RequestContext.get().getOrgName();
        service.uninstallApp(orgId, appSlug);
        return ResponseEntity.ok(ApiResponse.ok("App uninstalled", null));
    }

    @PutMapping("/{appSlug}/config")
    public ResponseEntity<ApiResponse<AppInstallationResponse>> updateConfig(
            @PathVariable String appSlug,
            @RequestBody Map<String, Object> config) {
        String orgId = RequestContext.get().getOrgName();
        var installation = service.updateConfig(orgId, appSlug, config);
        return ResponseEntity.ok(ApiResponse.ok("Config updated", installation));
    }

    @PostMapping("/{appSlug}/launch")
    public ResponseEntity<ApiResponse<Void>> logLaunch(
            @PathVariable String appSlug,
            @RequestBody LaunchRequest request,
            Authentication authentication) {
        String orgId = RequestContext.get().getOrgName();
        String launchedBy = authentication != null ? authentication.getName() : "unknown";
        service.logLaunch(orgId, appSlug, launchedBy, request);
        return ResponseEntity.ok(ApiResponse.ok("Launch logged", null));
    }
}
