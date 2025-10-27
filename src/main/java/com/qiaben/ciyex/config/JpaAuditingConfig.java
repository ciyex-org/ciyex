package com.qiaben.ciyex.config;

import com.qiaben.ciyex.dto.integration.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * Configuration for JPA auditing.
 * Enables automatic population of @CreatedBy and @LastModifiedBy fields.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@Slf4j
public class JpaAuditingConfig {

    /**
     * Provides the current auditor (user) from the RequestContext.
     * This will be used to populate @CreatedBy and @LastModifiedBy fields.
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return new AuditorAwareImpl();
    }

    /**
     * Implementation of AuditorAware that retrieves the current user
     * from the RequestContext ThreadLocal.
     */
    @Slf4j
    static class AuditorAwareImpl implements AuditorAware<String> {

        @Override
        public Optional<String> getCurrentAuditor() {
            RequestContext context = RequestContext.get();
            
            if (context != null) {
                // Try to extract user information from auth token or other context fields
                String auditor = extractUserFromContext(context);
                if (auditor != null) {
                    log.debug("Current auditor: {}", auditor);
                    return Optional.of(auditor);
                }
            }
            
            // Return system as default if no user context is available
            log.debug("No user context available, using 'system' as auditor");
            return Optional.of("system");
        }

        /**
         * Extracts user identifier from RequestContext.
         * You can customize this method based on how user information is stored in your context.
         */
        private String extractUserFromContext(RequestContext context) {
            // If you have a userId or username field in RequestContext, use it
            // For now, we'll use the tenant name as a fallback
            // You may want to add a userId field to RequestContext
            
            String authToken = context.getAuthToken();
            if (authToken != null && !authToken.isEmpty()) {
                // You could parse JWT token here to extract username/userId
                // For now, return a placeholder
                return "user-from-token"; // TODO: Parse actual user from JWT token
            }
            
            // Fallback to tenant name if no user info available
            String tenantName = context.getTenantName();
            if (tenantName != null) {
                return "tenant:" + tenantName;
            }
            
            return null;
        }
    }
}
