package com.qiaben.ciyex.filter;

import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.service.KeycloakAuthService;
import com.qiaben.ciyex.service.TenantAccessService;
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

    // Paths that don't require tenant resolution
    private static final String[] EXCLUDED_PATHS = {
        "/api/auth/",
        "/api/portal/auth/",
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

                // Get X-Org-Id header if present
                String orgIdHeader = req.getHeader("X-Org-Id");
                Long requestedOrgId = null;
                if (orgIdHeader != null && !orgIdHeader.isBlank()) {
                    try {
                        requestedOrgId = Long.parseLong(orgIdHeader);
                    } catch (NumberFormatException e) {
                        log.warn("Invalid X-Org-Id header: {}", orgIdHeader);
                    }
                }

                // Resolve org ID based on groups, attributes, and header
                Long resolvedOrgId = tenantAccessService.resolveOrgId(groups, groupAttributes, requestedOrgId);

                if (resolvedOrgId != null) {
                    // Set org ID in RequestContext
                    RequestContext context = new RequestContext();
                    context.setOrgId(resolvedOrgId);
                    RequestContext.set(context);
                    log.debug("Resolved org ID: {} for URI: {}", resolvedOrgId, uri);
                } else {
                    // Check if org ID is required for this user
                    if (tenantAccessService.requiresOrgIdHeader(groups)) {
                        log.warn("X-Org-Id header required but not provided or invalid for URI: {}", uri);
                        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        res.setContentType("application/json");
                        res.getWriter().write("{\"error\":\"X-Org-Id header is required\",\"message\":\"You have access to multiple tenants. Please specify X-Org-Id header.\"}");
                        return;
                    } else {
                        log.warn("Could not resolve org ID for URI: {}", uri);
                    }
                }
            }

            // Continue with the request
            chain.doFilter(request, response);

        } finally {
            // Clean up RequestContext
            RequestContext.clear();
        }
    }
}
