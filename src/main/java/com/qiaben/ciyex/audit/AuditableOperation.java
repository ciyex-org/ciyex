package com.qiaben.ciyex.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that require audit logging for ONC certification
 * 
 * This annotation enables fine-grained control over audit logging for specific
 * operations that are critical for ONC § 170.315(d)(2) compliance.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditableOperation {
    
    /**
     * Type of operation being audited
     */
    String actionType() default "";
    
    /**
     * Type of entity being operated on
     */
    String entityType() default "";
    
    /**
     * Description of the operation
     */
    String description() default "";
    
    /**
     * Whether this operation involves patient data
     * If true, enhanced patient-specific auditing will be applied
     */
    boolean patientData() default false;
    
    /**
     * Risk level of the operation
     */
    AuditRiskLevel riskLevel() default AuditRiskLevel.LOW;
    
    /**
     * Whether this operation is compliance critical
     */
    boolean complianceCritical() default false;
    
    /**
     * Data classification level
     */
    DataClassification dataClassification() default DataClassification.INTERNAL;
}