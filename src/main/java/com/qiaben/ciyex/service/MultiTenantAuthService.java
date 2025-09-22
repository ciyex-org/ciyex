package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MultiTenantAuthService {
    
    private final OrganizationAuthService organizationAuthService;
    private final UserService userService;
    
    /**
     * Initialize tenant context during authentication
     * This should be called after successful JWT authentication
     */
    public void initializeTenantContext(String userEmail, String orgIdHeader) {
        try {
            Long orgId = null;
            
            // If X-Org-Id header is provided, validate it
            if (orgIdHeader != null && !orgIdHeader.trim().isEmpty()) {
                orgId = Long.parseLong(orgIdHeader);
                
                // Validate user has access to this organization
                if (!organizationAuthService.validateUserOrgAccess(userEmail, orgId)) {
                    log.warn("User {} does not have access to organization {}", userEmail, orgId);
                    throw new SecurityException("Access denied to organization: " + orgId);
                }
            } else {
                // No org header provided, use user's default organization
                orgId = organizationAuthService.getDefaultOrganizationForUser(userEmail);
            }
            
            if (orgId != null) {
                RequestContext context = RequestContext.get();
                if (context == null) {
                    context = new RequestContext();
                    RequestContext.set(context);
                }
                context.setOrgId(orgId);
                log.debug("Initialized tenant context with orgId: {} for user: {}", orgId, userEmail);
            } else {
                log.warn("No organization found for user: {}", userEmail);
            }
            
        } catch (NumberFormatException e) {
            log.error("Invalid organization ID format: {}", orgIdHeader, e);
            throw new IllegalArgumentException("Invalid organization ID format");
        } catch (Exception e) {
            log.error("Error initializing tenant context for user: {}", userEmail, e);
            throw e;
        }
    }
    
    /**
     * Switch organization context for authenticated user
     */
    public void switchOrganization(Long newOrgId) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("No authenticated user found");
        }
        
        // Validate user has access to the new organization
        if (!organizationAuthService.validateUserOrgAccess(currentUser.getEmail(), newOrgId)) {
            throw new SecurityException("Access denied to organization: " + newOrgId);
        }
        
        RequestContext context = RequestContext.get();
        if (context == null) {
            context = new RequestContext();
            RequestContext.set(context);
        }
        context.setOrgId(newOrgId);
        
        log.info("Switched organization context to {} for user {}", newOrgId, currentUser.getEmail());
    }
    
    /**
     * Get current organization ID from context
     */
    public Long getCurrentOrganizationId() {
        RequestContext context = RequestContext.get();
        return context != null ? context.getOrgId() : null;
    }
}
