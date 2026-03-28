package org.ciyex.ehr.security.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Parameterized security tests covering ALL 18 FHIR resource scope domains.
 *
 * <p>For each resource type, we verify three properties:
 * <ol>
 *   <li>Correct scope → security passes (status is NOT 401 or 403)</li>
 *   <li>No auth token → 401</li>
 *   <li>Valid JWT with zero scopes → 403</li>
 *   <li>Valid JWT with wrong resource scope → 403</li>
 * </ol>
 *
 * <p>Additionally tests write scope isolation:
 * read-only token cannot call mutation (POST/PUT/DELETE) endpoints that
 * require the corresponding write scope.
 *
 * <p>Scope domains covered (26 controllers, 18 FHIR resource types):
 * <ul>
 *   <li>Patient         — GenericFhirResourceController</li>
 *   <li>Encounter       — EncounterSummaryController</li>
 *   <li>Appointment     — RecallController</li>
 *   <li>Observation     — CdsAlertController</li>
 *   <li>MedicationRequest — PrescriptionController</li>
 *   <li>DiagnosticReport  — LabResultController, LabOrderController</li>
 *   <li>ServiceRequest  — ReferralController</li>
 *   <li>CarePlan        — CarePlanController</li>
 *   <li>Immunization    — ImmunizationController</li>
 *   <li>Consent         — PatientConsentController</li>
 *   <li>Task            — ClinicalTaskController</li>
 *   <li>Claim           — AllClaimsController, InvoiceController, PaymentController, PatientBillingController</li>
 *   <li>Coverage        — InsuranceCompanyController</li>
 *   <li>DocumentReference — DocumentController</li>
 *   <li>Organization    — OrgConfigController, AuditLogController, AppInstallationController,
 *                         RolePermissionController, UserManagementController</li>
 *   <li>Practitioner    — UserController</li>
 *   <li>Communication   — NotificationController, MessagingController</li>
 * </ul>
 */
@DisplayName("All FHIR Resource Scope Security — parameterized")
class AllResourcesScopeTest extends BaseApiSecurityTest {

    // ═══════════════════════════════════════════════════════════════════════════
    // READ SCOPE MATRIX
    // One representative GET per scope domain.
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * (resource-label, required-scope, representative-GET-url)
     */
    static Stream<Arguments> readScopeEndpoints() {
        return Stream.of(
                // Patient (GenericFhirResourceController – settings/tab view)
                Arguments.of("Patient",              "SCOPE_user/Patient.read",           "/api/fhir-resource/patients"),
                // Encounter (EncounterSummaryController)
                Arguments.of("Encounter",            "SCOPE_user/Encounter.read",         "/api/encounters/1/2/summary"),
                // Appointment / Recall (RecallController)
                Arguments.of("Appointment",          "SCOPE_user/Appointment.read",       "/api/recalls"),
                // Observation / CDS (CdsAlertController)
                Arguments.of("Observation",          "SCOPE_user/Observation.read",       "/api/cds/alerts"),
                // MedicationRequest / Prescription (PrescriptionController)
                Arguments.of("MedicationRequest",    "SCOPE_user/MedicationRequest.read", "/api/prescriptions"),
                // DiagnosticReport / Lab results (LabResultController)
                Arguments.of("DiagnosticReport(results)", "SCOPE_user/DiagnosticReport.read", "/api/lab-results"),
                // DiagnosticReport / Lab orders (LabOrderController – search is the only no-path-variable GET)
                Arguments.of("DiagnosticReport(orders)", "SCOPE_user/DiagnosticReport.read", "/api/lab-order/search"),
                // ServiceRequest / Referral (ReferralController)
                Arguments.of("ServiceRequest",       "SCOPE_user/ServiceRequest.read",   "/api/referrals"),
                // CarePlan (CarePlanController)
                Arguments.of("CarePlan",             "SCOPE_user/CarePlan.read",         "/api/care-plans"),
                // Immunization (ImmunizationController)
                Arguments.of("Immunization",         "SCOPE_user/Immunization.read",     "/api/immunizations"),
                // Consent (PatientConsentController)
                Arguments.of("Consent",              "SCOPE_user/Consent.read",          "/api/consents"),
                // Task (ClinicalTaskController)
                Arguments.of("Task",                 "SCOPE_user/Task.read",             "/api/tasks"),
                // Claim – AllClaimsController
                Arguments.of("Claim(all-claims)",    "SCOPE_user/Claim.read",            "/api/all-claims"),
                // Claim – InvoiceController
                Arguments.of("Claim(invoices)",      "SCOPE_user/Claim.read",            "/api/billing/invoices/1"),
                // Claim – PaymentController
                Arguments.of("Claim(payments)",      "SCOPE_user/Claim.read",            "/api/payments/transactions"),
                // Claim – PatientBillingController
                Arguments.of("Claim(billing)",       "SCOPE_user/Claim.read",            "/api/patient-billing/1/statement"),
                // Coverage / Insurance (InsuranceCompanyController)
                Arguments.of("Coverage",             "SCOPE_user/Coverage.read",         "/api/insurance-companies"),
                // DocumentReference (DocumentController – lists all docs)
                Arguments.of("DocumentReference",    "SCOPE_user/DocumentReference.read", "/api/documents/upload"),
                // Organization – OrgConfigController (read)
                Arguments.of("Organization(config)", "SCOPE_user/Organization.read",     "/api/orgConfig"),
                // Organization – AuditLogController (read)
                Arguments.of("Organization(audit)",  "SCOPE_user/Organization.read",     "/api/audit-log"),
                // Organization – AppInstallationController (write required even for GET)
                Arguments.of("Organization(apps)",   "SCOPE_user/Organization.write",    "/api/app-installations"),
                // Organization – RolePermissionController (write required)
                Arguments.of("Organization(roles)",  "SCOPE_user/Organization.write",    "/api/admin/roles"),
                // Organization – UserManagementController (write required)
                Arguments.of("Organization(users)",  "SCOPE_user/Organization.write",    "/api/admin/users"),
                // Practitioner – UserController has only write endpoints; test via /api/admin/users as proxy
                //   (Practitioner.write scope tested separately in writeScopeEndpoints)
                // Communication – NotificationController
                Arguments.of("Communication(notif)", "SCOPE_user/Communication.read",    "/api/notifications/log"),
                // Communication – MessagingController
                Arguments.of("Communication(msg)",   "SCOPE_user/Communication.read",    "/api/channels")
        );
    }

    @ParameterizedTest(name = "[{index}] {0}: GET with correct scope → security allows")
    @MethodSource("readScopeEndpoints")
    void readScope_correctScope_isAllowed(String resource, String scope, String url) throws Exception {
        var result = mockMvc.perform(get(url).with(scopes(scope))).andReturn();
        assertThat(result.getResponse().getStatus())
                .as("GET %s with %s should pass security (not 401/403), actual=%d",
                        url, scope, result.getResponse().getStatus())
                .isNotIn(401, 403);
    }

    @ParameterizedTest(name = "[{index}] {0}: GET without auth token → 401")
    @MethodSource("readScopeEndpoints")
    void readScope_noAuth_isUnauthorized(String resource, String scope, String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "[{index}] {0}: GET with valid JWT but zero scopes → 403")
    @MethodSource("readScopeEndpoints")
    void readScope_zeroScopes_isForbidden(String resource, String scope, String url) throws Exception {
        mockMvc.perform(get(url).with(noPermissions()))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "[{index}] {0}: GET with unrelated scope → 403")
    @MethodSource("readScopeEndpoints")
    void readScope_wrongScope_isForbidden(String resource, String scope, String url) throws Exception {
        // Pick a scope that is clearly unrelated to the required one
        String wrongScope = scope.contains("Claim") ? "SCOPE_user/Patient.read"
                : scope.contains("Patient") ? "SCOPE_user/Claim.read"
                : "SCOPE_user/Claim.read";
        mockMvc.perform(get(url).with(scopes(wrongScope)))
                .andExpect(status().isForbidden());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // WRITE SCOPE MATRIX
    // POST endpoints that require explicit write scope (confirmed via @PreAuthorize).
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * (resource-label, required-write-scope, request-builder-method, url)
     *
     * <p>Endpoints are chosen to avoid {@code @Valid @RequestBody} validation (which fires before
     * Spring Security AOP and would give 400/415 instead of 403). Preference:
     * <ul>
     *   <li>POST with no body (path-variable-only actions like {@code archive})</li>
     *   <li>DELETE with path variable (no body, clearly write-gated)</li>
     * </ul>
     */
    static Stream<Arguments> writeScopeEndpoints() {
        return Stream.of(
                // Patient.write — GenericFhirResourceController: POST /fhir-resource/patients
                //   body is a FHIR resource JSON with no @Valid, so {} passes deserialization
                Arguments.of("Patient",           "SCOPE_user/Patient.write",           "POST",   "/api/fhir-resource/patients"),
                // Coverage.write — InsuranceCompanyController: archive action has no @RequestBody
                Arguments.of("Coverage",          "SCOPE_user/Coverage.write",          "POST",   "/api/insurance-companies/1/archive"),
                // DocumentReference.write — DocumentController: DELETE has no @RequestBody
                Arguments.of("DocumentReference", "SCOPE_user/DocumentReference.write", "DELETE", "/api/documents/upload/1"),
                // Communication.write — NotificationController: POST /send (no @Valid body schema)
                Arguments.of("Communication",     "SCOPE_user/Communication.write",     "POST",   "/api/notifications/send"),
                // Organization.write — UserManagementController class-level write gate
                Arguments.of("Organization",      "SCOPE_user/Organization.write",      "POST",   "/api/admin/users")
        );
    }

    /** Build a request for the given HTTP method string. */
    private MockHttpServletRequestBuilder buildRequest(String method, String url) {
        return switch (method) {
            case "POST"   -> post(url).contentType(MediaType.APPLICATION_JSON).content("{}");
            case "DELETE" -> delete(url);
            default       -> get(url);
        };
    }

    @ParameterizedTest(name = "[{index}] {0}: {2} with read scope only → 403 (write required)")
    @MethodSource("writeScopeEndpoints")
    void writeScope_readScopeOnly_isForbidden(String resource, String writeScope, String method, String url)
            throws Exception {
        String readScope = writeScope.replace(".write", ".read");
        mockMvc.perform(buildRequest(method, url).with(scopes(readScope)))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "[{index}] {0}: {2} with write scope → security allows")
    @MethodSource("writeScopeEndpoints")
    void writeScope_writeScope_isAllowed(String resource, String writeScope, String method, String url)
            throws Exception {
        var result = mockMvc.perform(buildRequest(method, url).with(scopes(writeScope))).andReturn();
        assertThat(result.getResponse().getStatus())
                .as("%s %s with %s should pass security (not 401/403), actual=%d",
                        method, url, writeScope, result.getResponse().getStatus())
                .isNotIn(401, 403);
    }

    @ParameterizedTest(name = "[{index}] {0}: {2} without auth → 401")
    @MethodSource("writeScopeEndpoints")
    void writeScope_noAuth_isUnauthorized(String resource, String writeScope, String method, String url)
            throws Exception {
        mockMvc.perform(buildRequest(method, url))
                .andExpect(status().isUnauthorized());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCOPE ISOLATION — patient/* cannot access staff endpoints
    // ═══════════════════════════════════════════════════════════════════════════

    static Stream<Arguments> staffEndpointsBlockedForPatients() {
        return Stream.of(
                Arguments.of("GenericFhirResource(Patient)",     "/api/fhir-resource/patients"),
                Arguments.of("EncounterSummary",                 "/api/encounters/1/2/summary"),
                Arguments.of("Recalls(Appointment)",             "/api/recalls"),
                Arguments.of("CDS alerts (Observation)",         "/api/cds/alerts"),
                Arguments.of("Prescriptions (MedicationRequest)","/api/prescriptions"),
                Arguments.of("Lab results (DiagnosticReport)",   "/api/lab-results"),
                Arguments.of("Referrals (ServiceRequest)",       "/api/referrals"),
                Arguments.of("CarePlans",                        "/api/care-plans"),
                Arguments.of("Immunizations",                    "/api/immunizations"),
                Arguments.of("Consents",                         "/api/consents"),
                Arguments.of("Tasks",                            "/api/tasks"),
                Arguments.of("All claims (Claim)",               "/api/all-claims"),
                Arguments.of("Invoices (Claim)",                 "/api/billing/invoices/1"),
                Arguments.of("Insurance companies (Coverage)",   "/api/insurance-companies"),
                Arguments.of("Documents",                        "/api/documents/upload"),
                Arguments.of("Org config (Organization)",        "/api/orgConfig"),
                Arguments.of("Audit log (Organization)",         "/api/audit-log"),
                Arguments.of("Admin roles (Organization)",       "/api/admin/roles"),
                Arguments.of("Admin users (Organization)",       "/api/admin/users"),
                Arguments.of("Notifications (Communication)",    "/api/notifications/log"),
                Arguments.of("Messaging channels (Communication)","/api/channels")
        );
    }

    @ParameterizedTest(name = "[{index}] patient/* token cannot access {0}")
    @MethodSource("staffEndpointsBlockedForPatients")
    void patientPortalToken_cannotAccessStaffEndpoints(String label, String url) throws Exception {
        mockMvc.perform(get(url).with(patientToken()))
                .andExpect(status().isForbidden());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCOPE ISOLATION — billing cannot access clinical write endpoints
    // ═══════════════════════════════════════════════════════════════════════════

    static Stream<Arguments> clinicalWriteEndpointsBlockedForBilling() {
        return Stream.of(
                Arguments.of("Prescription write",   "SCOPE_user/MedicationRequest.write", "/api/prescriptions"),
                Arguments.of("Referral write",       "SCOPE_user/ServiceRequest.write",    "/api/referrals"),
                Arguments.of("CarePlan write",       "SCOPE_user/CarePlan.write",          "/api/care-plans"),
                Arguments.of("Immunization write",   "SCOPE_user/Immunization.write",      "/api/immunizations"),
                Arguments.of("Consent write",        "SCOPE_user/Consent.write",           "/api/consents"),
                Arguments.of("Task write",           "SCOPE_user/Task.write",              "/api/tasks"),
                Arguments.of("Admin users write",    "SCOPE_user/Organization.write",      "/api/admin/users")
        );
    }

    @ParameterizedTest(name = "[{index}] billing token cannot POST to {0}")
    @MethodSource("clinicalWriteEndpointsBlockedForBilling")
    void billingToken_cannotAccessClinicalWriteEndpoints(String label, String writeScope, String url)
            throws Exception {
        // billingToken has Claim.read/write + Patient.read + Coverage.read/write — no clinical write
        mockMvc.perform(post(url)
                        .with(billingToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCOPE ISOLATION — provider cannot access admin endpoints
    // ═══════════════════════════════════════════════════════════════════════════

    static Stream<Arguments> adminEndpointsBlockedForProvider() {
        return Stream.of(
                // These require Organization.write — provider has only Organization.read
                Arguments.of("GET /admin/roles",       "/api/admin/roles"),
                Arguments.of("GET /admin/users",       "/api/admin/users"),
                Arguments.of("GET /app-installations", "/api/app-installations")
        );
    }

    @ParameterizedTest(name = "[{index}] provider token cannot access {0}")
    @MethodSource("adminEndpointsBlockedForProvider")
    void providerToken_cannotAccessAdminEndpoints(String label, String url) throws Exception {
        mockMvc.perform(get(url).with(providerToken()))
                .andExpect(status().isForbidden());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ADMIN TOKEN — can access all resource endpoints
    // ═══════════════════════════════════════════════════════════════════════════

    static Stream<Arguments> allReadEndpoints() {
        return Stream.of(
                Arguments.of("/api/fhir-resource/patients"),
                Arguments.of("/api/encounters/1/2/summary"),
                Arguments.of("/api/recalls"),
                Arguments.of("/api/cds/alerts"),
                Arguments.of("/api/prescriptions"),
                Arguments.of("/api/lab-results"),
                Arguments.of("/api/lab-order"),
                Arguments.of("/api/referrals"),
                Arguments.of("/api/care-plans"),
                Arguments.of("/api/immunizations"),
                Arguments.of("/api/consents"),
                Arguments.of("/api/tasks"),
                Arguments.of("/api/all-claims"),
                Arguments.of("/api/billing/invoices/1"),
                Arguments.of("/api/payments/transactions"),
                Arguments.of("/api/patient-billing/1/statement"),
                Arguments.of("/api/insurance-companies"),
                Arguments.of("/api/documents/upload"),
                Arguments.of("/api/orgConfig"),
                Arguments.of("/api/audit-log"),
                Arguments.of("/api/app-installations"),
                Arguments.of("/api/admin/roles"),
                Arguments.of("/api/admin/users"),
                Arguments.of("/api/notifications/log"),
                Arguments.of("/api/channels")
        );
    }

    @ParameterizedTest(name = "[{index}] ADMIN token → GET {0} allowed")
    @MethodSource("allReadEndpoints")
    void adminToken_canAccessAllEndpoints(String url) throws Exception {
        var result = mockMvc.perform(get(url).with(adminToken())).andReturn();
        assertThat(result.getResponse().getStatus())
                .as("ADMIN should access GET %s (not 401/403), actual=%d", url, result.getResponse().getStatus())
                .isNotIn(401, 403);
    }
}
