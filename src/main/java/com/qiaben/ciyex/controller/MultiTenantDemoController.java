package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.entity.Org;
import com.qiaben.ciyex.service.MultiTenantAuthService;
import com.qiaben.ciyex.service.OrganizationAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/tenant")
@RequiredArgsConstructor
public class MultiTenantDemoController {
    
    private final MultiTenantAuthService multiTenantAuthService;
    private final OrganizationAuthService organizationAuthService;
    
    /**
     * Get current tenant context information
     */
    @GetMapping("/context")
    public ResponseEntity<Map<String, Object>> getCurrentContext(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        RequestContext context = RequestContext.get();
        if (context != null) {
            response.put("orgId", context.getOrgId());
            response.put("facilityId", context.getFacilityId());
            response.put("role", context.getRole());
        }
        
        response.put("user", authentication.getName());
        response.put("schemaName", context != null && context.getOrgId() != null ? 
                    "practice_" + context.getOrgId() : "master");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all organizations the current user has access to
     */
    @GetMapping("/organizations")
    public ResponseEntity<List<Org>> getUserOrganizations(Authentication authentication) {
        String userEmail = authentication.getName();
        List<Org> organizations = organizationAuthService.getOrganizationsForUser(userEmail);
        return ResponseEntity.ok(organizations);
    }
    
    /**
     * Switch to a different organization
     */
    @PostMapping("/switch/{orgId}")
    public ResponseEntity<Map<String, Object>> switchOrganization(
            @PathVariable Long orgId,
            Authentication authentication) {
        
        try {
            multiTenantAuthService.switchOrganization(orgId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Successfully switched to organization: " + orgId);
            response.put("newOrgId", orgId);
            response.put("schemaName", "practice_" + orgId);
            
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(403).body(error);
        }
    }
    
    /**
     * Test endpoint to verify schema isolation
     * This would query data from the current tenant's schema
     */
    @GetMapping("/test-isolation")
    public ResponseEntity<Map<String, Object>> testSchemaIsolation() {
        RequestContext context = RequestContext.get();
        
        Map<String, Object> response = new HashMap<>();
        response.put("currentSchema", context != null && context.getOrgId() != null ? 
                    "practice_" + context.getOrgId() : "master");
        response.put("message", "This endpoint would query data from the tenant-specific schema");
        response.put("timestamp", System.currentTimeMillis());
        
        // In a real implementation, you would query tenant-specific data here
        // For example: patientService.getAllPatients() would automatically
        // query from the correct schema based on the current RequestContext
        
        return ResponseEntity.ok(response);
    }
}
