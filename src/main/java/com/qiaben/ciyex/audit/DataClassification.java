package com.qiaben.ciyex.audit;

/**
 * Data classification levels for audit events to support HIPAA and ONC requirements
 * Helps track access to different types of healthcare information
 */
public enum DataClassification {
    /**
     * Public information - no privacy concerns
     */
    PUBLIC,
    
    /**
     * Internal information - organizational data
     */
    INTERNAL,
    
    /**
     * Confidential information - business sensitive data
     */
    CONFIDENTIAL,
    
    /**
     * Protected Health Information - HIPAA regulated patient data
     * Requires special handling and audit attention
     */
    PHI,
    
    /**
     * Sensitive PHI - highly sensitive patient information
     * (mental health, substance abuse, genetic information)
     */
    SENSITIVE_PHI
}