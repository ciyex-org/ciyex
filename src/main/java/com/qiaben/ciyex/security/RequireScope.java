package com.qiaben.ciyex.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface RequireScope {

    String[] value();

    Mode mode() default Mode.ANY;

    enum Mode {
        ANY,
        ALL
    }
}
