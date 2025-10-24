package com.qiaben.ciyex.multitenant;

import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.entity.User;
import com.qiaben.ciyex.entity.UserOrgRole;
import com.qiaben.ciyex.service.OrganizationAuthService;
import com.qiaben.ciyex.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantContextInterceptor implements HandlerInterceptor {
    
    private final UserService userService;
    private final OrganizationAuthService organizationAuthService;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            // Get org ID from header
            String orgIdHeader = request.getHeader("X-Org-Id");
            
            if (orgIdHeader != null) {
                Long orgId = Long.parseLong(orgIdHeader);
                
                // Validate that the current user has access to this organization
                if (validateUserOrgAccess(orgId)) {
                    RequestContext context = RequestContext.get();
                        if (context == null) {
                            context = new RequestContext();
                            RequestContext.set(context);
                        }
                        context.setOrgId(orgId);
                        log.debug("Set orgId {} in RequestContext from X-Org-Id header", orgId);
                } else {
                    log.warn("User does not have access to organization: {}", orgId);
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return false;
                }
            } else {
                // If no X-Org-Id header, try to get from user's default organization
                setDefaultOrgFromUser();
            }
            
            return true;
        } catch (NumberFormatException e) {
            log.error("Invalid X-Org-Id header format", e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
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
    
    private boolean validateUserOrgAccess(Long orgId) {
        try {
            RequestContext context = RequestContext.get();
            if (context == null || context.getAuthToken() == null) {
                return true; // Let authentication filter handle this
            }
            
            // Get current user and check if they have access to the requested org
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return true; // Let authentication filter handle this
            }
            
            // Use the organization auth service for validation
            return organizationAuthService.validateUserOrgAccess(currentUser.getEmail(), orgId);
                    
        } catch (Exception e) {
            log.error("Error validating user org access", e);
            return false;
        }
    }
    
    private void setDefaultOrgFromUser() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser != null && !currentUser.getUserOrgRoles().isEmpty()) {
                // Use the first organization as default
                UserOrgRole firstRole = currentUser.getUserOrgRoles().iterator().next();
                Long defaultOrgId = firstRole.getOrg().getId();
                
                RequestContext context = RequestContext.get();
                if (context == null) {
                    context = new RequestContext();
                    RequestContext.set(context);
                }
                context.setOrgId(defaultOrgId);
                log.debug("Set default orgId {} in RequestContext", defaultOrgId);
            }
        } catch (Exception e) {
            log.error("Error setting default org from user", e);
        }
    }
    
    private User getCurrentUser() {
        try {
            RequestContext context = RequestContext.get();
            if (context != null && context.getAuthToken() != null) {
                // Extract username from JWT token or use existing service method
                return userService.getCurrentUser();
            }
        } catch (Exception e) {
            log.debug("Could not get current user", e);
        }
        return null;
    }
}
