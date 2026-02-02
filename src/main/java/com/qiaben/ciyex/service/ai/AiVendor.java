package com.qiaben.ciyex.service.ai;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
public @interface AiVendor {
    @AliasFor(annotation = Component.class)
    String value(); // e.g., "azure", "openai", "mock"
}