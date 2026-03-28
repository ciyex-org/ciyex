package org.ciyex.ehr.security;

import java.util.Optional;

/**
 * Clinical roles for the Ciyex EHR platform, aligned with SMART on FHIR launch context.
 *
 * These map directly to Keycloak realm roles. The Keycloak role name must exactly match
 * the enum name (case-insensitive comparison applied in {@link #fromString}).
 *
 * <h3>Keycloak setup required:</h3>
 * <ul>
 *   <li>Define each value below as a Realm Role in the ciyex Keycloak realm.</li>
 *   <li>Assign roles to users directly or via Groups.</li>
 *   <li>Ensure the realm "Include in token scope" is enabled so roles appear
 *       in {@code realm_access.roles} in the JWT.</li>
 * </ul>
 */
public enum ClinicalRole {

    /** Full practice administrator: manages staff, settings, billing, and all clinical data. */
    ADMIN,

    /** Licensed clinician (physician, NP, PA): full clinical access. */
    PROVIDER,

    /** Registered nurse: clinical documentation, observations, limited prescriptions. */
    NURSE,

    /** Medical assistant: rooming, vitals, basic clinical support. */
    MA,

    /** Front desk: scheduling, patient registration, check-in/out, insurance collect. */
    FRONT_DESK,

    /** Billing specialist: claims, coverage, financial reports. */
    BILLING,

    /** Patient portal user: reads own records only (SMART {@code patient/} context). */
    PATIENT,

    /**
     * System-level super-admin (Keycloak realm role {@code super_admin}).
     * Bypasses all tenant and permission scoping.
     */
    SUPER_ADMIN;

    /**
     * Priority order for determining a user's "primary" role when they hold several.
     * Higher-privilege roles take precedence (lower index = higher priority).
     */
    public static final java.util.List<ClinicalRole> PRIORITY = java.util.List.of(
            SUPER_ADMIN, ADMIN, PROVIDER, NURSE, MA, FRONT_DESK, BILLING, PATIENT
    );

    /**
     * Parses a role name string (case-insensitive) to a {@link ClinicalRole}.
     * Returns empty if the string does not match any known role.
     */
    public static Optional<ClinicalRole> fromString(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        try {
            return Optional.of(valueOf(name.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
