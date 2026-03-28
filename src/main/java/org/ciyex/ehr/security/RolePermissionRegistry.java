package org.ciyex.ehr.security;

import java.util.*;
import java.util.stream.Collectors;

import static org.ciyex.ehr.security.Permission.*;

/**
 * Maps each {@link ClinicalRole} to its allowed set of SMART on FHIR permission scopes.
 *
 * This is the single authoritative source for "what can each role do?".
 * {@link KeycloakJwtAuthenticationConverter} calls this at authentication time
 * to expand a user's Keycloak roles into Spring Security {@code SCOPE_*}
 * GrantedAuthority objects. Controllers then enforce access via
 * {@code @PreAuthorize("hasAuthority(Permission.PATIENT_READ)")}.
 *
 * <h3>Design principles</h3>
 * <ul>
 *   <li>Roles are additive: a user holding multiple roles receives the union of
 *       all their permitted scopes.</li>
 *   <li>Write scopes imply nothing about read — both must be explicitly granted.</li>
 *   <li>SUPER_ADMIN maps to the same scopes as ADMIN (org-scoped, no cross-org bypass).</li>
 * </ul>
 *
 * <h3>Extending roles</h3>
 * Add a new {@link ClinicalRole} entry and register its scopes here.
 * No Keycloak client-scope configuration is required — the backend expands roles
 * automatically.
 */
public final class RolePermissionRegistry {

    private RolePermissionRegistry() {}

    private static final Map<ClinicalRole, Set<String>> REGISTRY;

    static {
        Map<ClinicalRole, Set<String>> m = new EnumMap<>(ClinicalRole.class);

        // ── Full org access scopes (shared by ADMIN and SUPER_ADMIN) ────────
        Set<String> adminScopes = Set.of(
                PATIENT_READ, PATIENT_WRITE,
                APPOINTMENT_READ, APPOINTMENT_WRITE,
                ENCOUNTER_READ, ENCOUNTER_WRITE,
                OBSERVATION_READ, OBSERVATION_WRITE,
                PROCEDURE_READ, PROCEDURE_WRITE,
                MEDICATION_REQUEST_READ, MEDICATION_REQUEST_WRITE,
                DIAGNOSTIC_REPORT_READ, DIAGNOSTIC_REPORT_WRITE,
                SERVICE_REQUEST_READ, SERVICE_REQUEST_WRITE,
                CARE_PLAN_READ, CARE_PLAN_WRITE,
                IMMUNIZATION_READ, IMMUNIZATION_WRITE,
                DOCUMENT_REFERENCE_READ, DOCUMENT_REFERENCE_WRITE,
                CONSENT_READ, CONSENT_WRITE,
                TASK_READ, TASK_WRITE,
                CLAIM_READ, CLAIM_WRITE,
                COVERAGE_READ, COVERAGE_WRITE,
                PRACTITIONER_READ, PRACTITIONER_WRITE,
                ORGANIZATION_READ, ORGANIZATION_WRITE,
                COMMUNICATION_READ, COMMUNICATION_WRITE,
                ALLERGY_INTOLERANCE_READ, ALLERGY_INTOLERANCE_WRITE,
                CONDITION_READ, CONDITION_WRITE,
                RELATED_PERSON_READ, RELATED_PERSON_WRITE,
                FLAG_READ, FLAG_WRITE,
                QUESTIONNAIRE_RESPONSE_READ, QUESTIONNAIRE_RESPONSE_WRITE,
                INVOICE_READ, INVOICE_WRITE,
                MEASURE_REPORT_READ, MEASURE_REPORT_WRITE,
                LOCATION_READ, LOCATION_WRITE,
                CLAIM_RESPONSE_READ, CLAIM_RESPONSE_WRITE,
                EXPLANATION_OF_BENEFIT_READ, EXPLANATION_OF_BENEFIT_WRITE,
                COMPOSITION_READ, COMPOSITION_WRITE
        );

        // ── SUPER_ADMIN: same as ADMIN — org-scoped, no cross-org bypass ───
        m.put(ClinicalRole.SUPER_ADMIN, adminScopes);

        // ── ADMIN: full org access — clinical + administrative ────────────────
        m.put(ClinicalRole.ADMIN, adminScopes);

        // ── PROVIDER: full clinical; read billing/coverage; no practice config write ──
        m.put(ClinicalRole.PROVIDER, Set.of(
                PATIENT_READ, PATIENT_WRITE,
                APPOINTMENT_READ, APPOINTMENT_WRITE,
                ENCOUNTER_READ, ENCOUNTER_WRITE,
                OBSERVATION_READ, OBSERVATION_WRITE,
                PROCEDURE_READ, PROCEDURE_WRITE,
                MEDICATION_REQUEST_READ, MEDICATION_REQUEST_WRITE,
                DIAGNOSTIC_REPORT_READ, DIAGNOSTIC_REPORT_WRITE,
                SERVICE_REQUEST_READ, SERVICE_REQUEST_WRITE,
                CARE_PLAN_READ, CARE_PLAN_WRITE,
                IMMUNIZATION_READ, IMMUNIZATION_WRITE,
                DOCUMENT_REFERENCE_READ, DOCUMENT_REFERENCE_WRITE,
                CONSENT_READ, CONSENT_WRITE,
                TASK_READ, TASK_WRITE,
                CLAIM_READ,
                COVERAGE_READ,
                PRACTITIONER_READ,
                ORGANIZATION_READ,
                COMMUNICATION_READ,
                ALLERGY_INTOLERANCE_READ, ALLERGY_INTOLERANCE_WRITE,
                CONDITION_READ, CONDITION_WRITE,
                RELATED_PERSON_READ,
                FLAG_READ, FLAG_WRITE,
                QUESTIONNAIRE_RESPONSE_READ, QUESTIONNAIRE_RESPONSE_WRITE,
                INVOICE_READ,
                MEASURE_REPORT_READ,
                LOCATION_READ,
                CLAIM_RESPONSE_READ,
                EXPLANATION_OF_BENEFIT_READ,
                COMPOSITION_READ, COMPOSITION_WRITE
        ));

        // ── NURSE: clinical documentation + most reads; no practice admin ─────
        m.put(ClinicalRole.NURSE, Set.of(
                PATIENT_READ, PATIENT_WRITE,
                APPOINTMENT_READ, APPOINTMENT_WRITE,
                ENCOUNTER_READ, ENCOUNTER_WRITE,
                OBSERVATION_READ, OBSERVATION_WRITE,
                PROCEDURE_READ,
                MEDICATION_REQUEST_READ,
                DIAGNOSTIC_REPORT_READ,
                SERVICE_REQUEST_READ,
                CARE_PLAN_READ,
                IMMUNIZATION_READ, IMMUNIZATION_WRITE,
                DOCUMENT_REFERENCE_READ, DOCUMENT_REFERENCE_WRITE,
                CONSENT_READ,
                TASK_READ, TASK_WRITE,
                COVERAGE_READ,
                PRACTITIONER_READ,
                ORGANIZATION_READ,
                COMMUNICATION_READ, COMMUNICATION_WRITE,
                ALLERGY_INTOLERANCE_READ, ALLERGY_INTOLERANCE_WRITE,
                CONDITION_READ, CONDITION_WRITE,
                RELATED_PERSON_READ,
                FLAG_READ, FLAG_WRITE,
                QUESTIONNAIRE_RESPONSE_READ, QUESTIONNAIRE_RESPONSE_WRITE,
                INVOICE_READ,
                MEASURE_REPORT_READ,
                LOCATION_READ,
                CLAIM_RESPONSE_READ,
                EXPLANATION_OF_BENEFIT_READ,
                COMPOSITION_READ, COMPOSITION_WRITE
        ));

        // ── MA (Medical Assistant): rooming, vitals, basic clinical support ───
        m.put(ClinicalRole.MA, Set.of(
                PATIENT_READ,
                APPOINTMENT_READ, APPOINTMENT_WRITE,
                ENCOUNTER_READ,
                OBSERVATION_READ, OBSERVATION_WRITE,
                PROCEDURE_READ,
                MEDICATION_REQUEST_READ,
                DIAGNOSTIC_REPORT_READ,
                SERVICE_REQUEST_READ,
                IMMUNIZATION_READ,
                DOCUMENT_REFERENCE_READ,
                CONSENT_READ,
                TASK_READ, TASK_WRITE,
                COVERAGE_READ,
                PRACTITIONER_READ,
                ORGANIZATION_READ,
                ALLERGY_INTOLERANCE_READ, ALLERGY_INTOLERANCE_WRITE,
                CONDITION_READ,
                RELATED_PERSON_READ,
                FLAG_READ,
                QUESTIONNAIRE_RESPONSE_READ,
                LOCATION_READ,
                COMPOSITION_READ, COMPOSITION_WRITE
        ));

        // ── FRONT_DESK: patient intake, scheduling, insurance collect ──────────
        m.put(ClinicalRole.FRONT_DESK, Set.of(
                PATIENT_READ, PATIENT_WRITE,
                APPOINTMENT_READ, APPOINTMENT_WRITE,
                ENCOUNTER_READ,
                CONSENT_READ, CONSENT_WRITE,
                COVERAGE_READ,
                DOCUMENT_REFERENCE_READ,
                TASK_READ,
                PRACTITIONER_READ,
                ORGANIZATION_READ,
                COMMUNICATION_READ,
                ALLERGY_INTOLERANCE_READ,
                CONDITION_READ,
                RELATED_PERSON_READ,
                FLAG_READ,
                INVOICE_READ,
                LOCATION_READ
        ));

        // ── BILLING: financial access only ────────────────────────────────────
        m.put(ClinicalRole.BILLING, Set.of(
                PATIENT_READ,
                APPOINTMENT_READ,
                ENCOUNTER_READ,
                PROCEDURE_READ,
                DIAGNOSTIC_REPORT_READ,
                SERVICE_REQUEST_READ,
                CLAIM_READ, CLAIM_WRITE,
                COVERAGE_READ, COVERAGE_WRITE,
                DOCUMENT_REFERENCE_READ,
                ORGANIZATION_READ,
                ALLERGY_INTOLERANCE_READ,
                CONDITION_READ,
                INVOICE_READ, INVOICE_WRITE,
                MEASURE_REPORT_READ,
                CLAIM_RESPONSE_READ, CLAIM_RESPONSE_WRITE,
                EXPLANATION_OF_BENEFIT_READ, EXPLANATION_OF_BENEFIT_WRITE
        ));

        // ── PATIENT: self-access only (SMART patient/ context) ────────────────
        m.put(ClinicalRole.PATIENT, Set.of(
                PATIENT_SELF_READ,
                PATIENT_APPOINTMENT_READ,
                PATIENT_OBSERVATION_READ,
                PATIENT_MEDICATION_READ,
                PATIENT_DIAGNOSTIC_READ,
                PATIENT_DOCUMENT_READ,
                PATIENT_CARE_PLAN_READ,
                PATIENT_ALLERGY_READ,
                PATIENT_CONDITION_READ,
                PATIENT_COMMUNICATION_READ,
                PATIENT_COMMUNICATION_WRITE,
                PATIENT_COVERAGE_READ,
                PATIENT_CLAIM_READ,
                PATIENT_QUESTIONNAIRE_READ
        ));

        REGISTRY = Collections.unmodifiableMap(m);
    }

    /**
     * Returns all SMART scope authority strings for a given role.
     * The returned strings are already prefixed with {@code SCOPE_} for direct
     * use as {@link org.springframework.security.core.GrantedAuthority} values.
     */
    public static Set<String> permissionsFor(ClinicalRole role) {
        return REGISTRY.getOrDefault(role, Set.of());
    }

    /**
     * Resolves the union of SMART scope authorities for a collection of role-name
     * strings (as they appear in the JWT). Unrecognised role names are silently ignored.
     */
    public static Set<String> permissionsForRoles(Collection<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) return Set.of();
        return roleNames.stream()
                .map(ClinicalRole::fromString)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .flatMap(role -> permissionsFor(role).stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Determines the primary (highest-privilege) role from a list of role-name strings.
     * Uses {@link ClinicalRole#PRIORITY} ordering.
     * Returns {@code null} if no recognised role is found.
     */
    public static ClinicalRole primaryRole(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) return null;
        return ClinicalRole.PRIORITY.stream()
                .filter(r -> roleNames.contains(r.name()))
                .findFirst()
                .orElse(null);
    }
}
