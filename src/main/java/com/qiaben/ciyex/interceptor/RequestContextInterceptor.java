package com.qiaben.ciyex.interceptor;

import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.security.KeycloakJwtAuthenticationConverter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Slf4j
@Component
public class RequestContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authHeader = request.getHeader("Authorization");

        RequestContext ctx = new RequestContext();
        ctx.setAuthToken(authHeader);

        // Extract org alias from Keycloak JWT token for FHIR partition URL
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();
                // Use org alias (e.g., "sunrise-family-medicine") for FHIR URL path partitioning
                String orgAlias = KeycloakJwtAuthenticationConverter.extractOrganizationAlias(jwt);
                if (orgAlias != null && !orgAlias.isBlank()) {
                    ctx.setOrgName(orgAlias);
                    log.debug("Extracted org alias '{}' from JWT token", orgAlias);
                } else {
                    log.warn("No organization alias found in JWT token");
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract org from JWT token: {}", e.getMessage());
        }

        RequestContext.set(ctx);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        RequestContext.clear();
    }
}

