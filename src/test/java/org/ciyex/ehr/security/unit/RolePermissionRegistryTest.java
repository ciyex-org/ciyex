package org.ciyex.ehr.security.unit;

import org.ciyex.ehr.security.ClinicalRole;
import org.ciyex.ehr.security.Permission;
import org.ciyex.ehr.security.RolePermissionRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.ciyex.ehr.security.ClinicalRole.*;
import static org.ciyex.ehr.security.Permission.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RolePermissionRegistry}.
 * Validates the role → SMART scope mapping without a Spring context.
 *
 * These tests act as a contract: if a role's permissions change, the test
 * fails immediately, forcing a deliberate code review.
 */
class RolePermissionRegistryTest {

    // ─── SUPER_ADMIN ──────────────────────────────────────────────────────────

    @Test
    void superAdmin_hasSameScopesAsAdmin() {
        // SUPER_ADMIN maps to ADMIN scopes (org-scoped, no cross-org bypass)
        assertEquals(perms(ADMIN), perms(SUPER_ADMIN),
                "SUPER_ADMIN should have the same scopes as ADMIN");
    }

    // ─── ADMIN ────────────────────────────────────────────────────────────────

    @Test
    void admin_hasAllClinicalRead() {
        Set<String> p = perms(ADMIN);
        assertTrue(p.contains(PATIENT_READ));
        assertTrue(p.contains(APPOINTMENT_READ));
        assertTrue(p.contains(ENCOUNTER_READ));
        assertTrue(p.contains(OBSERVATION_READ));
        assertTrue(p.contains(PROCEDURE_READ));
        assertTrue(p.contains(MEDICATION_REQUEST_READ));
        assertTrue(p.contains(DIAGNOSTIC_REPORT_READ));
        assertTrue(p.contains(CARE_PLAN_READ));
        assertTrue(p.contains(IMMUNIZATION_READ));
        assertTrue(p.contains(DOCUMENT_REFERENCE_READ));
        assertTrue(p.contains(CONSENT_READ));
        assertTrue(p.contains(TASK_READ));
    }

    @Test
    void admin_hasAllClinicalWrite() {
        Set<String> p = perms(ADMIN);
        assertTrue(p.contains(PATIENT_WRITE));
        assertTrue(p.contains(APPOINTMENT_WRITE));
        assertTrue(p.contains(ENCOUNTER_WRITE));
        assertTrue(p.contains(OBSERVATION_WRITE));
        assertTrue(p.contains(PROCEDURE_WRITE));
        assertTrue(p.contains(MEDICATION_REQUEST_WRITE));
    }

    @Test
    void admin_hasAdminScopes() {
        Set<String> p = perms(ADMIN);
        assertTrue(p.contains(ORGANIZATION_READ));
        assertTrue(p.contains(ORGANIZATION_WRITE));
        assertTrue(p.contains(PRACTITIONER_READ));
        assertTrue(p.contains(PRACTITIONER_WRITE));
        assertTrue(p.contains(CLAIM_READ));
        assertTrue(p.contains(CLAIM_WRITE));
        assertTrue(p.contains(COVERAGE_READ));
        assertTrue(p.contains(COVERAGE_WRITE));
    }

    @Test
    void admin_doesNotHaveSelfOnlyScopes() {
        // user/ scopes, not patient/ scopes
        assertFalse(perms(ADMIN).contains(PATIENT_SELF_READ));
    }

    // ─── PROVIDER ─────────────────────────────────────────────────────────────

    @Test
    void provider_hasFullClinicalAccess() {
        Set<String> p = perms(PROVIDER);
        assertTrue(p.contains(PATIENT_READ));
        assertTrue(p.contains(PATIENT_WRITE));
        assertTrue(p.contains(ENCOUNTER_WRITE));
        assertTrue(p.contains(MEDICATION_REQUEST_WRITE));
        assertTrue(p.contains(PROCEDURE_WRITE));
    }

    @Test
    void provider_hasOnlyClaimRead_notWrite() {
        Set<String> p = perms(PROVIDER);
        assertTrue(p.contains(CLAIM_READ));
        assertFalse(p.contains(CLAIM_WRITE),
                "PROVIDER should not be able to write/submit claims");
    }

    @Test
    void provider_hasNoOrganizationWrite() {
        assertFalse(perms(PROVIDER).contains(ORGANIZATION_WRITE),
                "PROVIDER should not change practice settings");
    }

    @Test
    void provider_hasNoPractitionerWrite() {
        assertFalse(perms(PROVIDER).contains(PRACTITIONER_WRITE),
                "PROVIDER should not manage staff");
    }

    // ─── NURSE ────────────────────────────────────────────────────────────────

    @Test
    void nurse_hasReadAndWriteForClinicalDocs() {
        Set<String> p = perms(NURSE);
        assertTrue(p.contains(OBSERVATION_READ));
        assertTrue(p.contains(OBSERVATION_WRITE));
        assertTrue(p.contains(IMMUNIZATION_WRITE));
        assertTrue(p.contains(DOCUMENT_REFERENCE_WRITE));
        assertTrue(p.contains(TASK_WRITE));
        assertTrue(p.contains(COMMUNICATION_WRITE));
    }

    @Test
    void nurse_cannotPrescribe() {
        assertFalse(perms(NURSE).contains(MEDICATION_REQUEST_WRITE),
                "NURSE should not write prescriptions");
    }

    @Test
    void nurse_hasNoAdminWrite() {
        assertFalse(perms(NURSE).contains(ORGANIZATION_WRITE));
        assertFalse(perms(NURSE).contains(PRACTITIONER_WRITE));
        assertFalse(perms(NURSE).contains(CLAIM_WRITE));
    }

    // ─── MA ───────────────────────────────────────────────────────────────────

    @Test
    void ma_hasVitalsAndRoomingAccess() {
        Set<String> p = perms(MA);
        assertTrue(p.contains(OBSERVATION_READ));
        assertTrue(p.contains(OBSERVATION_WRITE));   // vitals entry
        assertTrue(p.contains(APPOINTMENT_WRITE));   // rooming
        assertTrue(p.contains(TASK_WRITE));
    }

    @Test
    void ma_hasNoPatientWrite() {
        assertFalse(perms(MA).contains(PATIENT_WRITE),
                "MA should not modify patient demographics");
    }

    @Test
    void ma_hasNoEncounterWrite() {
        assertFalse(perms(MA).contains(ENCOUNTER_WRITE));
    }

    @Test
    void ma_hasNoBillingAccess() {
        assertFalse(perms(MA).contains(CLAIM_READ));
        assertFalse(perms(MA).contains(CLAIM_WRITE));
        assertFalse(perms(MA).contains(COVERAGE_WRITE));
    }

    // ─── FRONT_DESK ───────────────────────────────────────────────────────────

    @Test
    void frontDesk_hasSchedulingAndRegistration() {
        Set<String> p = perms(FRONT_DESK);
        assertTrue(p.contains(PATIENT_READ));
        assertTrue(p.contains(PATIENT_WRITE));
        assertTrue(p.contains(APPOINTMENT_READ));
        assertTrue(p.contains(APPOINTMENT_WRITE));
        assertTrue(p.contains(CONSENT_WRITE));
        assertTrue(p.contains(COVERAGE_READ));
    }

    @Test
    void frontDesk_hasNoClinicalWrite() {
        Set<String> p = perms(FRONT_DESK);
        assertFalse(p.contains(OBSERVATION_WRITE));
        assertFalse(p.contains(ENCOUNTER_WRITE));
        assertFalse(p.contains(MEDICATION_REQUEST_WRITE));
        assertFalse(p.contains(PROCEDURE_WRITE));
    }

    @Test
    void frontDesk_hasNoBillingWrite() {
        assertFalse(perms(FRONT_DESK).contains(CLAIM_WRITE));
        assertFalse(perms(FRONT_DESK).contains(COVERAGE_WRITE));
    }

    // ─── BILLING ──────────────────────────────────────────────────────────────

    @Test
    void billing_hasClaimReadAndWrite() {
        Set<String> p = perms(BILLING);
        assertTrue(p.contains(CLAIM_READ));
        assertTrue(p.contains(CLAIM_WRITE));
        assertTrue(p.contains(COVERAGE_READ));
        assertTrue(p.contains(COVERAGE_WRITE));
    }

    @Test
    void billing_hasLimitedClinicalRead() {
        Set<String> p = perms(BILLING);
        assertTrue(p.contains(PATIENT_READ));        // needs patient context
        assertTrue(p.contains(ENCOUNTER_READ));      // needs encounter for billing
        assertTrue(p.contains(PROCEDURE_READ));      // needs procedure codes
    }

    @Test
    void billing_hasNoClinicalWrite() {
        Set<String> p = perms(BILLING);
        assertFalse(p.contains(PATIENT_WRITE));
        assertFalse(p.contains(ENCOUNTER_WRITE));
        assertFalse(p.contains(OBSERVATION_WRITE));
        assertFalse(p.contains(MEDICATION_REQUEST_WRITE));
    }

    @Test
    void billing_hasNoAdminWrite() {
        assertFalse(perms(BILLING).contains(ORGANIZATION_WRITE));
        assertFalse(perms(BILLING).contains(PRACTITIONER_WRITE));
    }

    // ─── PATIENT ──────────────────────────────────────────────────────────────

    @Test
    void patient_hasOnlySelfAccessScopes() {
        Set<String> p = perms(PATIENT);
        assertTrue(p.contains(PATIENT_SELF_READ));
        assertTrue(p.contains(PATIENT_APPOINTMENT_READ));
        assertTrue(p.contains(PATIENT_OBSERVATION_READ));
        assertTrue(p.contains(PATIENT_MEDICATION_READ));
        assertTrue(p.contains(PATIENT_DIAGNOSTIC_READ));
        assertTrue(p.contains(PATIENT_DOCUMENT_READ));
        assertTrue(p.contains(PATIENT_CARE_PLAN_READ));
    }

    @Test
    void patient_hasNoUserContextScopes() {
        // PATIENT should only have patient/* scopes, not user/* scopes
        Set<String> p = perms(PATIENT);
        assertFalse(p.contains(PATIENT_READ),
                "PATIENT should use patient/Patient.read, not user/Patient.read");
        assertFalse(p.contains(APPOINTMENT_READ));
        assertFalse(p.contains(ENCOUNTER_READ));
        assertFalse(p.contains(CLAIM_READ));
        assertFalse(p.contains(ORGANIZATION_READ));
    }

    @Test
    void patient_hasNoWriteScopes() {
        Set<String> p = perms(PATIENT);
        for (String scope : p) {
            assertFalse(scope.endsWith(".write"),
                    "PATIENT should have no write scopes, found: " + scope);
        }
    }

    // ─── permissionsForRoles ──────────────────────────────────────────────────

    @Test
    void permissionsForRoles_mergesMultipleRoles() {
        // FRONT_DESK + BILLING = union of both sets
        Set<String> merged = RolePermissionRegistry.permissionsForRoles(
                List.of("FRONT_DESK", "BILLING"));

        // From FRONT_DESK
        assertTrue(merged.contains(APPOINTMENT_WRITE));
        // From BILLING
        assertTrue(merged.contains(CLAIM_WRITE));
    }

    @Test
    void permissionsForRoles_caseInsensitive() {
        Set<String> a = RolePermissionRegistry.permissionsForRoles(List.of("admin"));
        Set<String> b = RolePermissionRegistry.permissionsForRoles(List.of("ADMIN"));
        assertEquals(a, b);
    }

    @Test
    void permissionsForRoles_unknownRoleIgnored() {
        Set<String> result = RolePermissionRegistry.permissionsForRoles(
                List.of("UNKNOWN_ROLE", "ADMIN"));
        assertEquals(perms(ADMIN), result);
    }

    @Test
    void permissionsForRoles_emptyListReturnsEmpty() {
        assertTrue(RolePermissionRegistry.permissionsForRoles(List.of()).isEmpty());
    }

    @Test
    void permissionsForRoles_nullReturnsEmpty() {
        assertTrue(RolePermissionRegistry.permissionsForRoles(null).isEmpty());
    }

    // ─── primaryRole ──────────────────────────────────────────────────────────

    @Test
    void primaryRole_superAdminBeatsAdmin() {
        ClinicalRole primary = RolePermissionRegistry.primaryRole(
                List.of("ADMIN", "SUPER_ADMIN"));
        assertEquals(SUPER_ADMIN, primary);
    }

    @Test
    void primaryRole_adminBeatsProvider() {
        ClinicalRole primary = RolePermissionRegistry.primaryRole(
                List.of("PROVIDER", "ADMIN"));
        assertEquals(ADMIN, primary);
    }

    @Test
    void primaryRole_singleRole() {
        assertEquals(NURSE, RolePermissionRegistry.primaryRole(List.of("NURSE")));
    }

    @Test
    void primaryRole_nullReturnsNull() {
        assertNull(RolePermissionRegistry.primaryRole(null));
    }

    @Test
    void primaryRole_emptyReturnsNull() {
        assertNull(RolePermissionRegistry.primaryRole(List.of()));
    }

    @Test
    void primaryRole_unknownRolesOnlyReturnsNull() {
        assertNull(RolePermissionRegistry.primaryRole(List.of("RANDOM", "FAKE")));
    }

    // ─── All scopes start with SCOPE_ ─────────────────────────────────────────

    @Test
    void allRegisteredScopes_startWithSCOPE_prefix() {
        for (ClinicalRole role : ClinicalRole.values()) {
            for (String scope : perms(role)) {
                assertTrue(scope.startsWith("SCOPE_"),
                        "Scope for " + role + " missing SCOPE_ prefix: " + scope);
            }
        }
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private static Set<String> perms(ClinicalRole role) {
        return RolePermissionRegistry.permissionsFor(role);
    }
}
