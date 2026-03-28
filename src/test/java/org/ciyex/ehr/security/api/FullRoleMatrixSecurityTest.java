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
 * Phase 4 — Full role matrix tests.
 *
 * Tests each complete ClinicalRole (as granted by RolePermissionRegistry)
 * against representative endpoints. Verifies both what IS accessible and
 * what is NOT accessible for each role.
 *
 * Role matrix summary:
 *
 * Endpoint                              ADMIN  PROVIDER  NURSE  MA   FRONT_DESK  BILLING  PATIENT
 * GET  /api/fhir-resource/{tab}           ✓      ✓        ✓     ✓      ✓          ✓        ✗
 * POST /api/fhir-resource/{tab}           ✓      ✓        ✓     ✗      ✓          ✗        ✗
 * POST /api/fhir-resource/{tab}/pat/{id}  ✓      ✓        ✓     ✗      ✓(appt)    ✗        ✗
 * GET  /api/admin/roles                   ✓      ✗        ✗     ✗      ✗          ✗        ✗
 * GET  /api/admin/users                   ✓      ✗        ✗     ✗      ✗          ✗        ✗
 * POST /api/admin/users                   ✓      ✗        ✗     ✗      ✗          ✗        ✗
 */
@DisplayName("Phase 4 — Full role permission matrix")
class FullRoleMatrixSecurityTest extends BaseApiSecurityTest {

    @BeforeEach
    void stubServices() {
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
        Mockito.when(rolePermissionService.listRoles()).thenReturn(List.of());
        Mockito.when(keycloakUserService.listUsersByOrg(Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any()))
                .thenReturn(List.of());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ADMIN — full access to everything
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("ADMIN: GET /fhir-resource/patients → 200")
    void admin_canReadPatientResources() throws Exception {
        mockMvc.perform(get("/api/fhir-resource/patients").with(adminToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ADMIN: POST /fhir-resource/patients → 201")
    void admin_canCreatePatientResources() throws Exception {
        mockMvc.perform(post("/api/fhir-resource/patients")
                        .contentType("application/json").content("{}")
                        .with(adminToken()))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("ADMIN: GET /admin/roles → 200")
    void admin_canAccessAdminRoles() throws Exception {
        mockMvc.perform(get("/api/admin/roles").with(adminToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ADMIN: GET /admin/users → 200")
    void admin_canAccessAdminUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users").with(adminToken()))
                .andExpect(status().isOk());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PROVIDER — full clinical access, no admin
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("PROVIDER: GET /fhir-resource/patients → 200")
    void provider_canReadPatientResources() throws Exception {
        mockMvc.perform(get("/api/fhir-resource/patients").with(providerToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PROVIDER: POST /fhir-resource/encounters/patient/1 → 201")
    void provider_canCreateClinicalResources() throws Exception {
        mockMvc.perform(post("/api/fhir-resource/encounters/patient/1")
                        .contentType("application/json").content("{}")
                        .with(providerToken()))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("PROVIDER: GET /admin/roles → 403 (no Org.write scope)")
    void provider_cannotAccessAdminRoles() throws Exception {
        mockMvc.perform(get("/api/admin/roles").with(providerToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PROVIDER: GET /admin/users → 403")
    void provider_cannotAccessAdminUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users").with(providerToken()))
                .andExpect(status().isForbidden());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BILLING — financial access only
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("BILLING: GET /fhir-resource/claims → 200 (has Patient.read)")
    void billing_canReadPatientResources() throws Exception {
        mockMvc.perform(get("/api/fhir-resource/claims").with(billingToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("BILLING: POST /fhir-resource/encounters/patient/1 → 403 (no Patient.write)")
    void billing_cannotWriteClinicalResources() throws Exception {
        mockMvc.perform(post("/api/fhir-resource/encounters/patient/1")
                        .contentType("application/json").content("{}")
                        .with(billingToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("BILLING: GET /admin/roles → 403")
    void billing_cannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/roles").with(billingToken()))
                .andExpect(status().isForbidden());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PATIENT — self-access only (patient/ scopes)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("PATIENT: GET /fhir-resource/patients → 403 (no user/Patient.read)")
    void patient_cannotAccessStaffPatientEndpoints() throws Exception {
        mockMvc.perform(get("/api/fhir-resource/patients").with(patientToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATIENT: POST /fhir-resource/anything → 403")
    void patient_cannotWriteAnything() throws Exception {
        mockMvc.perform(post("/api/fhir-resource/patients")
                        .contentType("application/json").content("{}")
                        .with(patientToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATIENT: GET /admin/roles → 403")
    void patient_cannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/roles").with(patientToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATIENT: GET /admin/users → 403")
    void patient_cannotAccessAdminUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users").with(patientToken()))
                .andExpect(status().isForbidden());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Privilege escalation: BILLING attempting clinical writes
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("BILLING cannot escalate to write by providing only Claim.write scope")
    void billing_claimWriteScope_cannotWritePatientResources() throws Exception {
        mockMvc.perform(post("/api/fhir-resource/patients/patient/1")
                        .contentType("application/json").content("{}")
                        .with(scopes("SCOPE_user/Claim.write", "SCOPE_user/Patient.read")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cannot reach admin endpoints with only clinical scopes")
    void clinical_scopes_cannotAccessAdminEndpoints() throws Exception {
        // Combining read + write clinical scopes should NEVER unlock admin routes
        mockMvc.perform(get("/api/admin/users")
                        .with(scopes(
                                "SCOPE_user/Patient.read",    "SCOPE_user/Patient.write",
                                "SCOPE_user/Encounter.read",  "SCOPE_user/Encounter.write",
                                "SCOPE_user/Observation.read","SCOPE_user/Observation.write"
                        )))
                .andExpect(status().isForbidden());
    }
}
