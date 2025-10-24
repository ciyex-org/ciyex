package com.qiaben.ciyex.multitenant;

import com.qiaben.ciyex.dto.integration.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class TenantContextInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            RequestContext context = RequestContext.get();
            if (context == null) {
                context = new RequestContext();
                RequestContext.set(context);
            }
            
            // Get tenant name from header (priority 1)
            String tenantNameHeader = request.getHeader("x-tenant-name");
            if (tenantNameHeader != null && !tenantNameHeader.isEmpty()) {
                context.setTenantName(tenantNameHeader);
                log.debug("Set tenantName '{}' in RequestContext from x-tenant-name header", tenantNameHeader);
            } else {
                // Try to extract from JWT groups claim (priority 2)
                // Only if user is in a specific tenant group like /Tenants/practice_1
                // If user is just in /Tenants group (super-admin), they must use x-tenant-name header
                try {
                    org.springframework.security.core.Authentication authentication = 
                        org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                    
                    if (authentication != null && authentication.getAuthorities() != null) {
                        for (org.springframework.security.core.GrantedAuthority authority : authentication.getAuthorities()) {
                            String authorityStr = authority.getAuthority();
                            // Check for specific tenant group: /Tenants/{tenant_name}
                            // Must have something after /Tenants/ (not just /Tenants)
                            if (authorityStr.contains("/Tenants/")) {
                                int startIndex = authorityStr.indexOf("/Tenants/") + 9;
                                String tenantPart = authorityStr.substring(startIndex);
                                
                                // Only set tenant if there's a specific tenant name (not empty)
                                if (!tenantPart.isEmpty() && !tenantPart.equals("/")) {
                                    int slashIndex = tenantPart.indexOf('/');
                                    String tenantName = slashIndex > 0 ? tenantPart.substring(0, slashIndex) : tenantPart;
                                    context.setTenantName(tenantName);
                                    log.debug("Set tenantName '{}' in RequestContext from JWT groups", tenantName);
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("Could not extract tenant from JWT", e);
                }
            }
            
            return true;
        } catch (Exception e) {
            log.error("Error processing tenant context", e);
            return true; // Continue processing, let other filters handle auth
        }
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // Clean up RequestContext after request processing
        RequestContext.clear();
    }
}
