package com.qiaben.ciyex.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that should operate in tenant context.
 * Can specify the parameter name that contains the org ID.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TenantOperation {
    
    /**
     * Name of the parameter that contains the org ID.
     * Default is "orgId".
     */
    String orgIdParam() default "orgId";
    
    /**
     * Whether this operation requires tenant context.
     * If true and no org ID is found, an exception will be thrown.
     */
    boolean required() default false;
}
