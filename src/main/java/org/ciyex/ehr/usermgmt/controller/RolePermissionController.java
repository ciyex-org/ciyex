package org.ciyex.ehr.usermgmt.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.usermgmt.dto.RolePermissionDto;
import org.ciyex.ehr.usermgmt.service.RolePermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.NoSuchElementException;

@PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
@Slf4j
public class RolePermissionController {

    private final RolePermissionService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RolePermissionDto>>> listRoles() {
        try {
            var roles = service.listRoles();
            return ResponseEntity.ok(ApiResponse.ok("Roles retrieved", roles));
        } catch (Exception e) {
            log.error("Failed to list roles", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RolePermissionDto>> getRole(@PathVariable Long id) {
        try {
            var role = service.getRole(id);
            return ResponseEntity.ok(ApiResponse.ok("Role retrieved", role));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get role {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RolePermissionDto>> createRole(@RequestBody RolePermissionDto dto) {
        try {
            var created = service.createRole(dto);
            return ResponseEntity.ok(ApiResponse.ok("Role created", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create role", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RolePermissionDto>> updateRole(
            @PathVariable Long id, @RequestBody RolePermissionDto dto) {
        try {
            var updated = service.updateRole(id, dto);
            return ResponseEntity.ok(ApiResponse.ok("Role updated", updated));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update role {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id) {
        try {
            service.deleteRole(id);
            return ResponseEntity.ok(ApiResponse.ok("Role deleted", null));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete role {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }
}
