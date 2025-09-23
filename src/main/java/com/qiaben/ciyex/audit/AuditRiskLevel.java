package com.qiaben.ciyex.audit;

/**
 * Risk levels for audit events to support ONC security requirements
 * Helps prioritize security monitoring and compliance review
 */
public enum AuditRiskLevel {
    /**
     * Low risk - routine operations, read access to non-sensitive data
     */
    LOW,
    
    /**
     * Medium risk - data modifications, administrative actions
     */
    MEDIUM,
    
    /**
     * High risk - patient data access, failed authentication attempts, 
     * security-sensitive operations
     */
    HIGH,
    
    /**
     * Critical risk - administrative privilege escalation, 
     * bulk data export, system configuration changes
     */
    CRITICAL
}