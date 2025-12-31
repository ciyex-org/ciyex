package com.qiaben.ciyex.dto;

public enum TemplateContext {
    ENCOUNTER,
    PATIENT,
    PROVIDER,
    ORGANIZATION,
    GENERAL;

    public static TemplateContext from(String value) {
        if (value == null || value.isEmpty()) return GENERAL;
        try {
            return TemplateContext.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return GENERAL;
        }
    }
}
