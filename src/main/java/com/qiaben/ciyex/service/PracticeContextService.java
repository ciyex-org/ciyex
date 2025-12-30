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
     * Get the current practice ID from RequestContext or JWT token.
     * @return the practice ID
     * @throws IllegalStateException if no practice ID can be determined
     */
    public String getPracticeId() {
        // First try RequestContext (set by interceptor)
        RequestContext ctx = RequestContext.get();
        if (ctx != null && ctx.getOrgName() != null && !ctx.getOrgName().isBlank()) {
            return ctx.getOrgName();
        }

        // Fallback to JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String orgId = KeycloakJwtAuthenticationConverter.extractOrganizationId(jwt);
            if (orgId != null && !orgId.isBlank()) {
                return orgId;
            }
        }

        throw new IllegalStateException("No practice/tenant ID found in RequestContext or JWT token.");
    }
}
