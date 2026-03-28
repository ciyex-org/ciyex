package org.ciyex.ehr.interceptor;

import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.security.ClinicalRole;
import org.ciyex.ehr.security.KeycloakJwtAuthenticationConverter;
import org.ciyex.ehr.security.RolePermissionRegistry;
import org.ciyex.ehr.usermgmt.service.PermissionResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Populates {@link RequestContext} (ThreadLocal) from the authenticated JWT on every request.
 *
 * <p>This interceptor is responsible for <em>tenant context</em> only: it resolves the
 * organisation alias, user identity, and primary clinical role, and stores them in a
 * thread-local so downstream services can call {@link RequestContext#get()} without
 * receiving the HTTP request.</p>
 *
 * <p><strong>Permission enforcement is NOT done here.</strong>
 * All access control is handled by Spring Security's
 * {@code @PreAuthorize("hasAuthority(Permission.XXX)")} annotations on controllers,
 * backed by SMART on FHIR scope authorities granted at authentication time by
 * {@link KeycloakJwtAuthenticationConverter}.</p>
 *
 * <p>The legacy DB-based permission lookup via {@code PermissionResolver} has been
 * removed from the hot path. Permission strings stored in {@code role_permission_config}
 * are preserved for reference and can be used for custom org-level role display via the
 * admin API, but are no longer loaded per-request.</p>
 */
@Slf4j
@Component
public class RequestContextInterceptor implements HandlerInterceptor {

    private final PermissionResolver permissionResolver;

    public RequestContextInterceptor(PermissionResolver permissionResolver) {
        this.permissionResolver = permissionResolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authHeader = request.getHeader("Authorization");

        RequestContext ctx = new RequestContext();
        ctx.setAuthToken(authHeader);

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();

                // ── Org alias resolution ─────────────────────────────────────
                String orgAlias = KeycloakJwtAuthenticationConverter.extractOrganizationAlias(jwt);

                // Check if user has ciyex_super_admin role — they can override org via header
                boolean isCiyexSuperAdmin = jwtAuth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(a -> a.equals("ROLE_CIYEX_SUPER_ADMIN"));

                String headerOrg = request.getHeader("X-Tenant-Name");
                if (headerOrg == null || headerOrg.isBlank()) {
                    headerOrg = request.getHeader("X-Org-Alias");
                }

                if (isCiyexSuperAdmin && headerOrg != null && !headerOrg.isBlank()) {
                    // ciyex_super_admin: prefer header org (allows cross-org switching)
                    ctx.setOrgName(headerOrg);
                    orgAlias = headerOrg;
                    log.debug("ciyex_super_admin org override: '{}' for user '{}'",
                            headerOrg, jwt.getClaimAsString("preferred_username"));
                } else if (orgAlias != null && !orgAlias.isBlank()) {
                    ctx.setOrgName(orgAlias);
                } else if (headerOrg != null && !headerOrg.isBlank()) {
                    // Fallback: header org for users without JWT org claim
                    ctx.setOrgName(headerOrg);
                    orgAlias = headerOrg;
                    log.debug("Using header org '{}' for user '{}'",
                            headerOrg, jwt.getClaimAsString("preferred_username"));
                } else {
                    log.debug("No organisation claim in JWT for user '{}'",
                            jwt.getClaimAsString("preferred_username"));
                }

                // ── Primary role from Spring Security authorities ───────────────
                // Authorities were already expanded to SMART scopes by
                // KeycloakJwtAuthenticationConverter; extract the ROLE_ ones.
                List<String> roleNames = jwtAuth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(a -> a.startsWith("ROLE_"))
                        .map(a -> a.substring(5))
                        .filter(r -> !r.equals("CIYEX_SUPER_ADMIN")) // handled separately
                        .collect(Collectors.toList());

                // Try standard ClinicalRole priority first
                ClinicalRole primary = RolePermissionRegistry.primaryRole(roleNames);
                if (primary != null) {
                    ctx.setUserRole(primary.name());
                } else {
                    // Custom role — use PermissionResolver to find highest-priority role from DB
                    String dbPrimary = permissionResolver.determinePrimaryRole(orgAlias, roleNames);
                    ctx.setUserRole(dbPrimary);
                }

                log.debug("RequestContext: org={} user={} role={}",
                        orgAlias, jwt.getClaimAsString("preferred_username"),
                        ctx.getUserRole());
            }
        } catch (Exception e) {
            log.warn("Failed to extract tenant context from JWT: {}", e.getMessage());
        }

        RequestContext.set(ctx);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        RequestContext.clear();
    }

}
