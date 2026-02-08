package org.ciyex.ehr.dto;

public enum TemplateContext {
    ENCOUNTER,
    PORTAL;

    public static TemplateContext from(String value) {
        if (value == null || value.isEmpty()) return ENCOUNTER;
        try {
            return TemplateContext.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ENCOUNTER;
        }
    }
}
