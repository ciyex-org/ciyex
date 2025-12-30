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

        // Extract org from Keycloak JWT token
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();
                String orgId = KeycloakJwtAuthenticationConverter.extractOrganizationId(jwt);
                if (orgId != null && !orgId.isBlank()) {
                    ctx.setOrgName(orgId);
                    log.debug("Extracted org '{}' from JWT token", orgId);
                } else {
                    log.warn("No organization found in JWT token");
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

