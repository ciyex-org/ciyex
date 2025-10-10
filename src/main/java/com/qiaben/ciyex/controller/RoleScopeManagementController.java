package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.entity.RoleName;
import com.qiaben.ciyex.entity.User;
import com.qiaben.ciyex.service.RoleScopeManagementService;
import com.qiaben.ciyex.service.UserService;
import com.qiaben.ciyex.security.RequireScope;
import com.qiaben.ciyex.dto.integration.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/role-scope")
@RequiredArgsConstructor
@Slf4j
@RequireScope("user:write") // Admin-level functionality
public class RoleScopeManagementController {

    private final RoleScopeManagementService roleScopeManagementService;
    private final UserService userService;

    /**
     * Get default scopes for a specific role
     */
    @GetMapping("/role/{role}/default-scopes")
    @RequireScope("user:read")
    public ResponseEntity<Map<String, Object>> getDefaultScopesForRole(@PathVariable String role) {
        try {
            RoleName roleName = RoleName.valueOf(role.toUpperCase());
            List<String> defaultScopes = roleScopeManagementService.getDefaultScopesForRole(roleName);
            
            return ResponseEntity.ok(Map.of(
                "role", role,
                "defaultScopes", defaultScopes,
                "scopeCount", defaultScopes.size()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid role name: " + role,
                "validRoles", List.of(RoleName.values())
            ));
        }
    }

    /**
     * Initialize role-scope templates (for admin use)
     */
    @PostMapping("/initialize")
    public ResponseEntity<Map<String, Object>> initializeRoleScopeTemplates() {
        try {
            roleScopeManagementService.initializeRoleScopeTemplates();
            return ResponseEntity.ok(Map.of(
                "message", "Role-scope templates initialized successfully",
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("Error initializing role-scope templates", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to initialize role-scope templates",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Add a new role with default scopes
     */
    @PostMapping("/role/{role}/create")
    public ResponseEntity<Map<String, Object>> addNewRole(
            @PathVariable String role, 
            @RequestBody Map<String, Object> request) {
        try {
            RoleName roleName = RoleName.valueOf(role.toUpperCase());
            @SuppressWarnings("unchecked")
            List<String> defaultScopes = (List<String>) request.get("defaultScopes");
            
            if (defaultScopes == null || defaultScopes.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "defaultScopes is required and cannot be empty"
                ));
            }
            
            roleScopeManagementService.addNewRole(roleName, defaultScopes);
            
            return ResponseEntity.ok(Map.of(
                "message", "Role created successfully",
                "role", role,
                "defaultScopes", defaultScopes
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid role name: " + role,
                "validRoles", List.of(RoleName.values())
            ));
        } catch (Exception e) {
            log.error("Error creating new role", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to create role",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get user's scopes for current organization
     */
    @GetMapping("/user/{userId}/scopes")
    @RequireScope("user:read")
    public ResponseEntity<Map<String, Object>> getUserScopes(@PathVariable Long userId) {
        try {
            Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
            if (orgId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Organization context required (X-Org-Id header missing)"
                ));
            }
            
            User user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            
            Set<String> userScopes = roleScopeManagementService.getUserScopesForOrg(user, orgId);
            
            return ResponseEntity.ok(Map.of(
                "userId", userId,
                "orgId", orgId,
                "scopes", userScopes,
                "scopeCount", userScopes.size()
            ));
        } catch (Exception e) {
            log.error("Error getting user scopes", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to get user scopes",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get detailed scope analysis for a user (debugging/admin tool)
     */
    @GetMapping("/user/{userId}/analysis")
    @RequireScope("user:read")
    public ResponseEntity<Map<String, Object>> getUserScopeAnalysis(@PathVariable Long userId) {
        try {
            Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
            if (orgId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Organization context required (X-Org-Id header missing)"
                ));
            }
            
            User user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> analysis = roleScopeManagementService.getScopeAnalysis(user, orgId);
            
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            log.error("Error getting user scope analysis", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to get user scope analysis",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Assign additional scope to a user (beyond role defaults)
     */
    @PostMapping("/user/{userId}/additional-scope")
    public ResponseEntity<Map<String, Object>> assignAdditionalScope(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {
        try {
            Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
            if (orgId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Organization context required (X-Org-Id header missing)"
                ));
            }
            
            String scopeCode = request.get("scopeCode");
            if (scopeCode == null || scopeCode.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "scopeCode is required"
                ));
            }
            
            roleScopeManagementService.assignAdditionalScopeToUser(userId, orgId, scopeCode.trim());
            
            return ResponseEntity.ok(Map.of(
                "message", "Additional scope assigned successfully",
                "userId", userId,
                "orgId", orgId,
                "scopeCode", scopeCode.trim()
            ));
        } catch (Exception e) {
            log.error("Error assigning additional scope", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to assign additional scope",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Remove additional scope from a user
     */
    @DeleteMapping("/user/{userId}/additional-scope")
    public ResponseEntity<Map<String, Object>> removeAdditionalScope(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {
        try {
            Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
            if (orgId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Organization context required (X-Org-Id header missing)"
                ));
            }
            
            String scopeCode = request.get("scopeCode");
            if (scopeCode == null || scopeCode.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "scopeCode is required"
                ));
            }
            
            roleScopeManagementService.removeAdditionalScopeFromUser(userId, orgId, scopeCode.trim());
            
            return ResponseEntity.ok(Map.of(
                "message", "Additional scope removed successfully",
                "userId", userId,
                "orgId", orgId,
                "scopeCode", scopeCode.trim()
            ));
        } catch (Exception e) {
            log.error("Error removing additional scope", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to remove additional scope",
                "message", e.getMessage()
            ));
        }
    }
}