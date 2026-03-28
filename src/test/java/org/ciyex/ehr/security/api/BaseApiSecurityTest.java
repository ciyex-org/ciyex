package org.ciyex.ehr.security.api;

import org.ciyex.ehr.fhir.GenericFhirResourceService;
import org.ciyex.ehr.service.KeycloakUserService;
import org.ciyex.ehr.usermgmt.service.RolePermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Base class for API security tests.
 *
 * Uses Spring's core test infrastructure (no Spring Boot test slices needed).
 * {@link TestSecurityConfig} loads only the security layer + controllers + mocked services.
 * No database, no Vault, no FHIR server is required.
 *
 * <h3>How it works</h3>
 * {@code SecurityMockMvcRequestPostProcessors.jwt()} injects a pre-built
 * {@link org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken}
 * directly into the security context — bypassing the JWT decoder entirely.
 * Spring Security's {@code @PreAuthorize} then evaluates the injected authorities.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestSecurityConfig.class)
@WebAppConfiguration
public abstract class BaseApiSecurityTest {

    @Autowired
    protected WebApplicationContext webApplicationContext;

    @Autowired
    protected GenericFhirResourceService genericFhirResourceService;

    @Autowired
    protected RolePermissionService rolePermissionService;

    @Autowired
    protected KeycloakUserService keycloakUserService;

    protected MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    // ─── JWT builder helpers ───────────────────────────────────────────────────

    /** JWT with NO SMART scope authorities — should produce 403 on any protected endpoint. */
    protected RequestPostProcessor noPermissions() {
        return jwt();
    }

    /**
     * JWT with the given explicit scope strings as GrantedAuthority values.
     * Scope strings must start with {@code SCOPE_} (e.g. {@code SCOPE_user/Patient.read}).
     */
    protected RequestPostProcessor scopes(String... scopeAuthorities) {
        SimpleGrantedAuthority[] auths = Arrays.stream(scopeAuthorities)
                .map(SimpleGrantedAuthority::new)
                .toArray(SimpleGrantedAuthority[]::new);
        return jwt().authorities(auths);
    }

    /** JWT simulating an ADMIN user — all user/* read + write scopes across all 18 FHIR resource types. */
    protected RequestPostProcessor adminToken() {
        return scopes(
                "SCOPE_user/Patient.read",           "SCOPE_user/Patient.write",
                "SCOPE_user/Appointment.read",       "SCOPE_user/Appointment.write",
                "SCOPE_user/Encounter.read",         "SCOPE_user/Encounter.write",
                "SCOPE_user/Observation.read",       "SCOPE_user/Observation.write",
                "SCOPE_user/MedicationRequest.read", "SCOPE_user/MedicationRequest.write",
                "SCOPE_user/DiagnosticReport.read",  "SCOPE_user/DiagnosticReport.write",
                "SCOPE_user/ServiceRequest.read",    "SCOPE_user/ServiceRequest.write",
                "SCOPE_user/CarePlan.read",          "SCOPE_user/CarePlan.write",
                "SCOPE_user/Immunization.read",      "SCOPE_user/Immunization.write",
                "SCOPE_user/Consent.read",           "SCOPE_user/Consent.write",
                "SCOPE_user/Task.read",              "SCOPE_user/Task.write",
                "SCOPE_user/Claim.read",             "SCOPE_user/Claim.write",
                "SCOPE_user/Coverage.read",          "SCOPE_user/Coverage.write",
                "SCOPE_user/DocumentReference.read", "SCOPE_user/DocumentReference.write",
                "SCOPE_user/Organization.read",      "SCOPE_user/Organization.write",
                "SCOPE_user/Practitioner.read",      "SCOPE_user/Practitioner.write",
                "SCOPE_user/Communication.read",     "SCOPE_user/Communication.write"
        );
    }

    /** JWT simulating a PROVIDER user — full clinical, no admin. */
    protected RequestPostProcessor providerToken() {
        return scopes(
                "SCOPE_user/Patient.read",    "SCOPE_user/Patient.write",
                "SCOPE_user/Appointment.read","SCOPE_user/Appointment.write",
                "SCOPE_user/Encounter.read",  "SCOPE_user/Encounter.write",
                "SCOPE_user/Observation.read","SCOPE_user/Observation.write",
                "SCOPE_user/Procedure.read",  "SCOPE_user/Procedure.write",
                "SCOPE_user/MedicationRequest.read","SCOPE_user/MedicationRequest.write",
                "SCOPE_user/Claim.read",
                "SCOPE_user/Organization.read"
        );
    }

    /** JWT simulating a BILLING user — financial access only. */
    protected RequestPostProcessor billingToken() {
        return scopes(
                "SCOPE_user/Patient.read",
                "SCOPE_user/Encounter.read",
                "SCOPE_user/Claim.read",    "SCOPE_user/Claim.write",
                "SCOPE_user/Coverage.read", "SCOPE_user/Coverage.write",
                "SCOPE_user/Organization.read"
        );
    }

    /** JWT simulating a PATIENT portal user — patient/ context scopes only. */
    protected RequestPostProcessor patientToken() {
        return scopes(
                "SCOPE_patient/Patient.read",
                "SCOPE_patient/Appointment.read",
                "SCOPE_patient/Observation.read",
                "SCOPE_patient/MedicationRequest.read",
                "SCOPE_patient/DiagnosticReport.read",
                "SCOPE_patient/DocumentReference.read",
                "SCOPE_patient/CarePlan.read"
        );
    }
}
