package com.qiaben.ciyex.entity;

public enum TemplateContext {
    ENCOUNTER, PORTAL;

    public static TemplateContext from(String v) {
        if (v == null) return null;
        return switch (v.trim().toLowerCase()) {
            case "encounter" -> ENCOUNTER;
            case "portal" -> PORTAL;
            default -> throw new IllegalArgumentException("Unsupported context: " + v);
        };
    }

    @Override public String toString() { return name().toLowerCase(); }
}


