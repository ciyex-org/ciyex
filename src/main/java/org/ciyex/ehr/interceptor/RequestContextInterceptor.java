package org.ciyex.ehr.interceptor;

import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.security.KeycloakJwtAuthenticationConverter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RequestContextInterceptor implements HandlerInterceptor {

    private static final String X_ORG_ALIAS_HEADER = "X-Org-Alias";
    
    private final Set<String> superAdminOrgs;

    public RequestContextInterceptor(@Value("${ciyex.super-admin.orgs:}") String superAdminOrgsConfig) {
        this.superAdminOrgs = Arrays.stream(superAdminOrgsConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        log.info("Super admin orgs configured: {}", superAdminOrgs);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authHeader = request.getHeader("Authorization");
        String xOrgAliasHeader = request.getHeader(X_ORG_ALIAS_HEADER);

        RequestContext ctx = new RequestContext();
        ctx.setAuthToken(authHeader);

        // Extract org alias from Keycloak JWT token for FHIR partition URL
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();
                // Use org alias (e.g., "sunrise-family-medicine") for FHIR URL path partitioning
                String tokenOrgAlias = KeycloakJwtAuthenticationConverter.extractOrganizationAlias(jwt);
                
                if (tokenOrgAlias != null && !tokenOrgAlias.isBlank()) {
                    // Check if user is super admin and has X-Org-Alias header
                    if (superAdminOrgs.contains(tokenOrgAlias) && xOrgAliasHeader != null && !xOrgAliasHeader.isBlank()) {
                        ctx.setOrgName(xOrgAliasHeader);
                        ctx.setSuperAdmin(true);
                        log.debug("Super admin '{}' accessing org '{}' via X-Org-Alias header", tokenOrgAlias, xOrgAliasHeader);
                    } else {
                        ctx.setOrgName(tokenOrgAlias);
                        ctx.setSuperAdmin(superAdminOrgs.contains(tokenOrgAlias));
                        log.debug("Extracted org alias '{}' from JWT token", tokenOrgAlias);
                    }
                } else {
                    log.debug("No organization alias found in JWT token - proceeding without org context");
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

