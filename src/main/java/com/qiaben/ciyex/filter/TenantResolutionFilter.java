package com.qiaben.ciyex.filter;

import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.service.KeycloakAuthService;
import com.qiaben.ciyex.service.TenantAccessService;
import com.qiaben.ciyex.service.TenantSchemaService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Filter to resolve tenant/org ID based on Keycloak groups and X-Org-Id header
 * 
 * Logic:
 * 1. Extract JWT token from Authorization header
 * 2. Extract groups from token
 * 3. Determine if X-Org-Id is required based on groups
 * 4. Resolve org ID and set in RequestContext
 */
@Component
@Order(2) // Run after JWT filter
@Slf4j
public class TenantResolutionFilter implements Filter {

    @Autowired
    private KeycloakAuthService keycloakAuthService;

    @Autowired
    private TenantAccessService tenantAccessService;
    
    @Autowired
    private TenantSchemaService tenantSchemaService;

    // Paths that don't require tenant resolution
    private static final String[] EXCLUDED_PATHS = {
        "/api/auth/",
        "/api/portal/auth/",
        "/api/tenants/accessible",  // Allow users to discover their accessible tenants
        "/actuator/",
        "/error"
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String uri = req.getRequestURI();

        // Skip tenant resolution for excluded paths
        for (String excludedPath : EXCLUDED_PATHS) {
            if (uri.startsWith(excludedPath)) {
                chain.doFilter(request, response);
                return;
            }
        }

        try {
            // Extract JWT token from Authorization header
            String authHeader = req.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                // Extract groups from token
                List<String> groups = keycloakAuthService.extractGroupsFromToken(token);
                log.debug("User groups: {}", groups);

                // Extract group attributes from token
                java.util.Map<String, java.util.Map<String, Object>> groupAttributes = 
                        keycloakAuthService.extractGroupAttributesFromToken(token);
                log.debug("Group attributes: {}", groupAttributes);

                // Get X-Tenant-Name header if present (optional, for multi-tenant users)
                String tenantNameHeader = req.getHeader("X-Tenant-Name");
                
                // Determine tenant name
                String tenantName = null;
                
                if (tenantAccessService.hasAccessToAllTenants(groups)) {
                    // User has full access, must specify tenant
                    if (tenantNameHeader == null || tenantNameHeader.isBlank()) {
                        log.warn("X-Tenant-Name header required for users with full access");
                        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        res.setContentType("application/json");
                        res.getWriter().write("{\"error\":\"X-Tenant-Name header is required\",\"message\":\"You have access to all tenants. Please specify X-Tenant-Name header.\"}");
                        return;
                    }
                    tenantName = tenantNameHeader;
                } else {
                    // Get accessible tenants
                    List<String> accessibleTenants = tenantAccessService.getAccessibleTenants(groups);
                    
                    if (accessibleTenants.isEmpty()) {
                        log.warn("User has no tenant access for URI: {}", uri);
                        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        res.setContentType("application/json");
                        res.getWriter().write("{\"error\":\"No tenant access\",\"message\":\"You do not have access to any tenants.\"}");
                        return;
                    }
                    
                    if (accessibleTenants.size() == 1) {
                        // Single tenant, use it
                        tenantName = accessibleTenants.get(0);
                    } else {
                        // Multiple tenants, must specify
                        if (tenantNameHeader == null || tenantNameHeader.isBlank()) {
                            log.warn("X-Tenant-Name header required for users with multiple tenant access");
                            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"error\":\"X-Tenant-Name header is required\",\"message\":\"You have access to multiple tenants. Please specify X-Tenant-Name header. Accessible tenants: " + String.join(", ", accessibleTenants) + "\"}");
                            return;
                        }
                        
                        // Validate tenant access
                        if (!tenantAccessService.hasAccessToTenant(groups, tenantNameHeader)) {
                            log.warn("User does not have access to tenant: {}", tenantNameHeader);
                            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"error\":\"Access denied\",\"message\":\"You do not have access to tenant: " + tenantNameHeader + "\"}");
                            return;
                        }
                        
                        tenantName = tenantNameHeader;
                    }
                }
                
                // Ensure schema exists for tenant and get schema name
                String schemaName = tenantSchemaService.ensureSchemaForTenant(tenantName);
                
                // Get org_id from group attributes (for backward compatibility)
                String tenantGroupPath = "/Tenants/" + tenantName;
                Long orgId = tenantAccessService.getOrgIdFromGroupAttributes(groupAttributes, tenantGroupPath);
                
                // Set tenant info in RequestContext
                RequestContext context = new RequestContext();
                context.setOrgId(orgId); // May be null for new tenants
                // TODO: Add tenantName and schemaName to RequestContext
                RequestContext.set(context);
                
                log.debug("Resolved tenant: {}, schema: {}, orgId: {} for URI: {}", 
                        tenantName, schemaName, orgId, uri);
            }

            // Continue with the request
            chain.doFilter(request, response);

        } finally {
            // Clean up RequestContext
            RequestContext.clear();
        }
    }
}
