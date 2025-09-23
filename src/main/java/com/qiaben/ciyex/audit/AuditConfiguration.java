package com.qiaben.ciyex.audit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

/**
 * Configuration properties for the audit logging system
 * Supports customization of audit behavior for different environments
 */
@Configuration
@ConfigurationProperties(prefix = "ciyex.audit")
@Data
public class AuditConfiguration {
    
    /**
     * Whether audit logging is enabled
     */
    private boolean enabled = true;
    
    /**
     * Whether to log successful operations
     */
    private boolean logSuccessfulOperations = true;
    
    /**
     * Whether to log failed operations
     */
    private boolean logFailedOperations = true;
    
    /**
     * Whether to log controller method calls automatically
     */
    private boolean autoLogControllerMethods = true;
    
    /**
     * Whether to log service method calls automatically
     */
    private boolean autoLogServiceMethods = true;
    
    /**
     * Whether to capture detailed request/response information
     */
    private boolean captureDetailedInfo = true;
    
    /**
     * Maximum length for description field
     */
    private int maxDescriptionLength = 500;
    
    /**
     * Maximum length for details field
     */
    private int maxDetailsLength = 10000;
    
    /**
     * Whether to anonymize user information in logs
     */
    private boolean anonymizeUserInfo = false;
    
    /**
     * Retention period for audit logs in days
     */
    private int retentionPeriodDays = 2555; // 7 years for healthcare compliance
    
    /**
     * Whether to archive old audit logs instead of deleting them
     */
    private boolean archiveOldLogs = true;
    
    /**
     * Batch size for audit log processing
     */
    private int batchSize = 100;
    
    /**
     * Whether to use asynchronous audit logging
     */
    private boolean asyncLogging = false;
    
    /**
     * Thread pool size for async audit logging
     */
    private int asyncThreadPoolSize = 5;
    
    /**
     * Whether to validate audit log integrity
     */
    private boolean validateIntegrity = true;
    
    /**
     * Organization ID for multi-tenant systems
     */
    private Long defaultOrganizationId;
    
    /**
     * Whether to log IP addresses
     */
    private boolean logIpAddresses = true;
    
    /**
     * Whether to log user agent information
     */
    private boolean logUserAgent = true;
    
    /**
     * Whether to log session information
     */
    private boolean logSessionInfo = true;
    
    /**
     * Specific endpoints to exclude from automatic audit logging
     */
    private String[] excludedEndpoints = {
        "/health", "/metrics", "/info", "/actuator"
    };
    
    /**
     * Specific user roles to exclude from automatic audit logging
     */
    private String[] excludedUserRoles = {
        "SYSTEM", "HEALTH_CHECK"
    };
    
    /**
     * Whether to enable real-time audit alerts
     */
    private boolean enableAlerts = true;
    
    /**
     * Threshold for failed login attempts to trigger alert
     */
    private int failedLoginAlertThreshold = 5;
    
    /**
     * Time window for failed login threshold (in minutes)
     */
    private int failedLoginTimeWindowMinutes = 15;
    
    /**
     * Whether to enable compliance monitoring
     */
    private boolean enableComplianceMonitoring = true;
    
    /**
     * Whether to generate daily compliance reports
     */
    private boolean generateDailyReports = true;
    
    /**
     * Email addresses for audit alerts
     */
    private String[] alertEmailAddresses = {};
    
    /**
     * Whether to enable tamper detection
     */
    private boolean enableTamperDetection = true;
    
    /**
     * Whether to use digital signatures for audit logs
     */
    private boolean useDigitalSignatures = false;
    
    /**
     * Secret key for audit log signing (if enabled)
     */
    private String signingSecretKey;
}