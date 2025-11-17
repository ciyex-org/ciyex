package com.qiaben.ciyex.interceptor;

import com.qiaben.ciyex.dto.integration.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
@Component
public class RequestContextInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Log incoming header info to help debug tenant routing
        String authHeader = request.getHeader("Authorization");
        String tenantHeader = request.getHeader("X-Tenant-Name");


        RequestContext ctx = new RequestContext();
        ctx.setAuthToken(authHeader);

        RequestContext.set(ctx);

        // Populate tenantName: prefer explicit header, otherwise try security context, else set safe default
        if (tenantHeader != null && !tenantHeader.isBlank()) {
            ctx.setTenantName(tenantHeader.trim());
        } else {
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated()) {
                    // Best-effort: look for an authority containing 'Tenants/' and extract last segment
                    String tenantFromAuth = auth.getAuthorities().stream()
                            .map(Object::toString)
                            .filter(s -> s.contains("Tenants/"))
                            .findFirst()
                            .map(s -> s.substring(s.lastIndexOf('/') + 1))
                            .orElse(null);
                    if (tenantFromAuth != null && !tenantFromAuth.isBlank()) {
                        ctx.setTenantName(tenantFromAuth);
                    } else {
                        ctx.setTenantName("default");
                    }
                } else {
                    ctx.setTenantName("default");
                }
            } catch (Exception e) {
                log.warn("Failed to extract tenant from security context, using default", e);
                ctx.setTenantName("default");
            }
        }

        RequestContext.set(ctx);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        RequestContext.clear();
    }
}
