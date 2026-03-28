package org.ciyex.ehr.security.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 1 + Phase 2 permission tests.
 *
 * Phase 1 — No authentication:
 *   Any request without a JWT must return 401 Unauthorized.
 *
 * Phase 2 — Valid JWT, zero SMART scopes:
 *   A legitimately-authenticated user with NO scope authorities must receive 403 Forbidden
 *   on every protected endpoint (not 401, because authentication succeeded).
 */
class NoAuthAndEmptyPermissionTest extends BaseApiSecurityTest {

    // ──────────────────────────────────────────────────────────────────────────
    // PHASE 1: No authentication at all → 401
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Phase 1 — No JWT → 401 on all protected endpoints")
    class NoAuth {

        @Test
        void getPatientResources_noAuth_returns401() throws Exception {
            mockMvc.perform(get("/api/fhir-resource/patients"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void postPatientResource_noAuth_returns401() throws Exception {
            mockMvc.perform(post("/api/fhir-resource/patients")
                            .contentType("application/json")
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void getPatientById_noAuth_returns401() throws Exception {
            mockMvc.perform(get("/api/fhir-resource/patients/patient/123"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void listAdminRoles_noAuth_returns401() throws Exception {
            mockMvc.perform(get("/api/admin/roles"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void createAdminRole_noAuth_returns401() throws Exception {
            mockMvc.perform(post("/api/admin/roles")
                            .contentType("application/json")
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void listUsers_noAuth_returns401() throws Exception {
            mockMvc.perform(get("/api/admin/users"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void createUser_noAuth_returns401() throws Exception {
            mockMvc.perform(post("/api/admin/users")
                            .contentType("application/json")
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void deleteResource_noAuth_returns401() throws Exception {
            mockMvc.perform(delete("/api/fhir-resource/appointments/patient/1/res-001"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void updateResource_noAuth_returns401() throws Exception {
            mockMvc.perform(put("/api/fhir-resource/encounters/patient/1/enc-001")
                            .contentType("application/json")
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PHASE 2: Valid JWT with NO scopes → 403 on all protected endpoints
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Phase 2 — JWT with zero scopes → 403 on all protected endpoints")
    class EmptyPermissions {

        @Test
        void getPatientResources_noScopes_returns403() throws Exception {
            mockMvc.perform(get("/api/fhir-resource/patients")
                            .with(noPermissions()))
                    .andExpect(status().isForbidden());
        }

        @Test
        void postPatientResource_noScopes_returns403() throws Exception {
            mockMvc.perform(post("/api/fhir-resource/patients")
                            .contentType("application/json")
                            .content("{}")
                            .with(noPermissions()))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getPatientById_noScopes_returns403() throws Exception {
            mockMvc.perform(get("/api/fhir-resource/patients/patient/123")
                            .with(noPermissions()))
                    .andExpect(status().isForbidden());
        }

        @Test
        void listAdminRoles_noScopes_returns403() throws Exception {
            mockMvc.perform(get("/api/admin/roles")
                            .with(noPermissions()))
                    .andExpect(status().isForbidden());
        }

        @Test
        void listUsers_noScopes_returns403() throws Exception {
            mockMvc.perform(get("/api/admin/users")
                            .with(noPermissions()))
                    .andExpect(status().isForbidden());
        }

        @Test
        void deleteResource_noScopes_returns403() throws Exception {
            mockMvc.perform(delete("/api/fhir-resource/appointments/patient/1/res-001")
                            .with(noPermissions()))
                    .andExpect(status().isForbidden());
        }

        @Test
        void updateResource_noScopes_returns403() throws Exception {
            mockMvc.perform(put("/api/fhir-resource/encounters/patient/1/enc-001")
                            .contentType("application/json")
                            .content("{}")
                            .with(noPermissions()))
                    .andExpect(status().isForbidden());
        }

        @Test
        void claimRead_scopeDoesNotGrantPatientRead_returns403() throws Exception {
            // SCOPE_user/Claim.read should NOT unlock /api/fhir-resource/{tab}/patient/{id}
            // because that endpoint requires SCOPE_user/Patient.read
            mockMvc.perform(get("/api/fhir-resource/patients/patient/123")
                            .with(scopes("SCOPE_user/Claim.read")))
                    .andExpect(status().isForbidden());
        }

        @Test
        void patientSelfScope_doesNotGrantUserPatientRead_returns403() throws Exception {
            // patient/Patient.read should NOT satisfy @PreAuthorize requiring user/Patient.read
            mockMvc.perform(get("/api/fhir-resource/patients")
                            .with(scopes("SCOPE_patient/Patient.read")))
                    .andExpect(status().isForbidden());
        }

        @Test
        void patientReadScope_doesNotGrantAdminEndpoints_returns403() throws Exception {
            // SCOPE_user/Patient.read should NOT unlock /api/admin/roles
            mockMvc.perform(get("/api/admin/roles")
                            .with(scopes("SCOPE_user/Patient.read")))
                    .andExpect(status().isForbidden());
        }
    }
}
