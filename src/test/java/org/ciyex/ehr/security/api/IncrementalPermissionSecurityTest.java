package org.ciyex.ehr.security.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 3 — Incremental permission tests.
 *
 * Simulates adding permissions to a role one scope at a time and verifying that:
 * 1. The newly-granted endpoint becomes accessible (200/201).
 * 2. All other endpoints still return 403 — no accidental scope leakage.
 *
 * Progression:
 *   Step 0: No scopes                     → all 403
 *   Step 1: +user/Patient.read            → GET patient resources pass, writes still 403
 *   Step 2: +user/Patient.write           → POST/PUT/DELETE patient resources pass
 *   Step 3: +user/Organization.write      → admin/roles + admin/users now pass
 *   Step 4: ONLY user/Claim.read          → claim endpoints pass, patient endpoints still 403
 */
class IncrementalPermissionSecurityTest extends BaseApiSecurityTest {

    @BeforeEach
    void stubServices() {
        // Stub service calls so the controller body succeeds when security passes
        Map<String, Object> emptyPage = new LinkedHashMap<>();
        emptyPage.put("content", List.of());
        emptyPage.put("page", 0);
        emptyPage.put("size", 20);
        emptyPage.put("totalElements", 0);
        emptyPage.put("totalPages", 0);
        emptyPage.put("hasNext", false);

        Mockito.when(genericFhirResourceService.listAll(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(emptyPage);
        Mockito.when(genericFhirResourceService.list(Mockito.anyString(), Mockito.anyLong(),
                Mockito.anyInt(), Mockito.anyInt(), Mockito.any()))
                .thenReturn(emptyPage);
        Mockito.when(genericFhirResourceService.create(Mockito.anyString(), Mockito.any(), Mockito.anyMap()))
                .thenReturn(Map.of("id", "new-001"));
        Mockito.when(genericFhirResourceService.create(Mockito.anyString(), Mockito.any(), Mockito.anyMap(), Mockito.any()))
                .thenReturn(Map.of("id", "new-001"));
        Mockito.when(genericFhirResourceService.update(Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.anyMap()))
                .thenReturn(Map.of("id", "upd-001"));
    }

    // ─── Step 0: No scopes ────────────────────────────────────────────────────

    @Test
    @DisplayName("Step 0: No scopes → GET /fhir-resource 403")
    void step0_noScopes_getPatientResource_isForbidden() throws Exception {
        mockMvc.perform(get("/api/fhir-resource/patients")
                        .with(noPermissions()))
                .andExpect(status().isForbidden());
    }

    // ─── Step 1: +user/Patient.read ───────────────────────────────────────────

    @Test
    @DisplayName("Step 1: +Patient.read → GET /fhir-resource/patients passes")
    void step1_patientRead_getSettingsResource_passes() throws Exception {
        mockMvc.perform(get("/api/fhir-resource/patients")
                        .with(scopes("SCOPE_user/Patient.read")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Step 1: +Patient.read → GET /fhir-resource/patients/patient/{id} passes")
    void step1_patientRead_getPatientResource_passes() throws Exception {
        mockMvc.perform(get("/api/fhir-resource/patients/patient/1")
                        .with(scopes("SCOPE_user/Patient.read")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Step 1: +Patient.read → POST /fhir-resource/patients still 403 (no write)")
    void step1_patientRead_postPatientResource_stillForbidden() throws Exception {
        mockMvc.perform(post("/api/fhir-resource/patients")
                        .contentType("application/json")
                        .content("{\"name\":\"test\"}")
                        .with(scopes("SCOPE_user/Patient.read")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Step 1: +Patient.read → DELETE /fhir-resource still 403")
    void step1_patientRead_deleteResource_stillForbidden() throws Exception {
        mockMvc.perform(delete("/api/fhir-resource/patients/patient/1/res-001")
                        .with(scopes("SCOPE_user/Patient.read")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Step 1: +Patient.read → /api/admin/roles still 403 (needs Org.write)")
    void step1_patientRead_adminRoles_stillForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/roles")
                        .with(scopes("SCOPE_user/Patient.read")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Step 1: +Patient.read → /api/admin/users still 403")
    void step1_patientRead_adminUsers_stillForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .with(scopes("SCOPE_user/Patient.read")))
                .andExpect(status().isForbidden());
    }

    // ─── Step 2: +user/Patient.write (added to read) ──────────────────────────

    @Test
    @DisplayName("Step 2: +Patient.write → POST /fhir-resource/patients now passes")
    void step2_patientWrite_postPatientResource_passes() throws Exception {
        mockMvc.perform(post("/api/fhir-resource/patients")
                        .contentType("application/json")
                        .content("{\"name\":\"test\"}")
                        .with(scopes("SCOPE_user/Patient.read", "SCOPE_user/Patient.write")))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Step 2: +Patient.write → PUT /fhir-resource/patients/patient/1/res now passes")
    void step2_patientWrite_putResource_passes() throws Exception {
        mockMvc.perform(put("/api/fhir-resource/patients/patient/1/res-001")
                        .contentType("application/json")
                        .content("{\"name\":\"updated\"}")
                        .with(scopes("SCOPE_user/Patient.read", "SCOPE_user/Patient.write")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Step 2: +Patient.write → admin endpoints still 403")
    void step2_patientWrite_adminEndpoints_stillForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .with(scopes("SCOPE_user/Patient.read", "SCOPE_user/Patient.write")))
                .andExpect(status().isForbidden());
    }

    // ─── Step 3: +user/Organization.write → admin endpoints unlock ────────────

    @Test
    @DisplayName("Step 3: +Org.write → GET /api/admin/roles now passes")
    void step3_orgWrite_adminRoles_passes() throws Exception {
        Mockito.when(rolePermissionService.listRoles()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/roles")
                        .with(scopes("SCOPE_user/Patient.read", "SCOPE_user/Organization.write")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Step 3: +Org.write → GET /api/admin/users now passes")
    void step3_orgWrite_adminUsers_passes() throws Exception {
        Mockito.when(keycloakUserService.listUsersByOrg(Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/admin/users")
                        .with(scopes("SCOPE_user/Patient.read", "SCOPE_user/Organization.write")))
                .andExpect(status().isOk());
    }

    // ─── Step 4: ONLY user/Claim.read — scope isolation ──────────────────────

    @Test
    @DisplayName("Step 4: Only Claim.read → patient endpoints still 403 (scope isolation)")
    void step4_claimReadOnly_patientEndpoints_areForbidden() throws Exception {
        mockMvc.perform(get("/api/fhir-resource/patients")
                        .with(scopes("SCOPE_user/Claim.read")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Step 4: Only Claim.read → admin endpoints still 403")
    void step4_claimReadOnly_adminEndpoints_areForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/roles")
                        .with(scopes("SCOPE_user/Claim.read")))
                .andExpect(status().isForbidden());
    }

    // ─── Scope boundary: patient/ context scopes ─────────────────────────────

    @Test
    @DisplayName("Patient portal scopes (patient/*) cannot access user/* endpoints")
    void patientPortalScopes_cannotAccessStaffEndpoints() throws Exception {
        // All patient/ scopes together still cannot access user/ endpoints
        mockMvc.perform(get("/api/fhir-resource/patients")
                        .with(patientToken()))
                .andExpect(status().isForbidden());
    }
}
