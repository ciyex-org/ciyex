package org.ciyex.ehr.usermgmt.controller;

import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.security.SmartScopeResolver;
import org.ciyex.ehr.usermgmt.service.PermissionResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Returns the current user's resolved permissions from {@code role_permission_config}.
 * Used by the frontend PermissionGuard to control page-level access.
 *
 * <p>Permissions are looked up via {@link PermissionResolver} (with Caffeine caching),
 * not from RequestContext (which no longer loads permissions per-request).</p>
 */
@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/api/user")
public class UserPermissionController {

    private final PermissionResolver permissionResolver;
    private final SmartScopeResolver smartScopeResolver;

    public UserPermissionController(PermissionResolver permissionResolver,
                                    SmartScopeResolver smartScopeResolver) {
        this.permissionResolver = permissionResolver;
        this.smartScopeResolver = smartScopeResolver;
    }

    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyPermissions() {
        RequestContext ctx = RequestContext.get();

        String orgAlias = ctx.getOrgName();
        String userRole = ctx.getUserRole();

        // Resolve category-level permissions from role_permission_config (org-scoped)
        List<String> permissions;
        if (orgAlias != null && userRole != null) {
            permissions = permissionResolver.resolve(orgAlias, List.of(userRole));
        } else {
            permissions = List.of();
        }

        // Resolve SMART on FHIR scopes from DB (org-customisable) with static fallback
        Set<String> scopes = userRole != null
                ? smartScopeResolver.resolveScopes(orgAlias, List.of(userRole))
                : Set.of();

        // Extract writable resource types (e.g. "Appointment" from "SCOPE_user/Appointment.write")
        List<String> writableResources = scopes.stream()
                .filter(s -> s.endsWith(".write"))
                .map(s -> s.replace("SCOPE_user/", "").replace("SCOPE_patient/", "").replace(".write", ""))
                .sorted()
                .collect(Collectors.toList());

        // Extract readable resource types (e.g. "Communication" from "SCOPE_user/Communication.read")
        List<String> readableResources = scopes.stream()
                .filter(s -> s.endsWith(".read"))
                .map(s -> s.replace("SCOPE_user/", "").replace("SCOPE_patient/", "").replace(".read", ""))
                .sorted()
                .collect(Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("role", userRole != null ? userRole : "");
        data.put("permissions", permissions);
        data.put("writableResources", writableResources);
        data.put("readableResources", readableResources);

        return ResponseEntity.ok(ApiResponse.ok("Permissions retrieved", data));
    }
}
