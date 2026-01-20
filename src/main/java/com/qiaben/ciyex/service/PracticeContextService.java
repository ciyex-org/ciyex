package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.security.KeycloakJwtAuthenticationConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

/**
 * Shared service for getting the current practice/tenant ID.
 * Uses RequestContext (populated by interceptor) with JWT fallback.
 */
@Service
public class PracticeContextService {

    /**
     * Get the current practice/org alias from RequestContext or JWT token.
     * This returns the org alias (e.g., "sunrise-family-medicine") used for FHIR URL path partitioning.
     * @return the org alias, or null if none available
     */
    public String getPracticeId() {
        // First try RequestContext (set by interceptor)
        RequestContext ctx = RequestContext.get();
        if (ctx != null && ctx.getOrgName() != null && !ctx.getOrgName().isBlank()) {
            return ctx.getOrgName();
        }

        // Fallback to JWT token - extract org alias for FHIR URL path
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String orgAlias = KeycloakJwtAuthenticationConverter.extractOrganizationAlias(jwt);
            if (orgAlias != null && !orgAlias.isBlank()) {
                return orgAlias;
            }
        }

        // Return null instead of throwing exception - let services handle gracefully
        return null;
    }
}
