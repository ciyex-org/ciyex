package com.qiaben.ciyex.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation specifically for marking patient data access operations
 * 
 * This annotation ensures comprehensive auditing of patient data access
 * as required by ONC § 170.315(d)(2) and HIPAA Security Rule.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PatientDataAccess {
    
    /**
     * Type of patient data access (VIEW, CREATE, UPDATE, DELETE)
     */
    String accessType() default "VIEW";
    
    /**
     * Description of what patient data is being accessed
     */
    String description() default "";
    
    /**
     * Whether this access requires special authorization
     */
    boolean requiresAuthorization() default false;
    
    /**
     * Whether this is a sensitive data access (mental health, genetics, etc.)
     */
    boolean sensitiveData() default false;
}