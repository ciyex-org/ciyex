package org.ciyex.ehr.usermgmt.controller;

import ca.uhn.fhir.rest.gclient.TokenClientParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.fhir.FhirClientService;
import org.ciyex.ehr.service.KeycloakUserService;
import org.ciyex.ehr.usermgmt.dto.*;
import org.ciyex.ehr.usermgmt.service.EmailService;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Slf4j
public class UserManagementController {

    private final KeycloakUserService keycloakUserService;
    private final EmailService emailService;
    private final FhirClientService fhirClient;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String search) {
        try {

            String orgAlias = RequestContext.get().getOrgName();
            var users = keycloakUserService.listUsersByOrg(orgAlias, page * size, size, search);
            return ResponseEntity.ok(ApiResponse.ok("Users retrieved", users));
        } catch (Exception e) {
            log.error("Failed to list users", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to list users: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable String id) {
        try {

            var user = keycloakUserService.getUserById(id);
            return ResponseEntity.ok(ApiResponse.ok("User retrieved", user));
        } catch (Exception e) {
            log.error("Failed to get user {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed to get user: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@RequestBody CreateUserRequest req) {
        try {

            String orgAlias = RequestContext.get().getOrgName();

            // Generate temp password if not provided
            String password = req.getTemporaryPassword();
            if (password == null || password.isBlank()) {
                password = generateTempPassword();
            }

            // Build attributes — auto-link to FHIR record by email
            Map<String, String> attributes = new HashMap<>();
            if (req.getPhone() != null) attributes.put("phone", req.getPhone());

            // Resolve FHIR ID: use explicit linkedFhirId if provided, otherwise auto-search by email
            String fhirId = req.getLinkedFhirId();
            boolean isPatient = "PATIENT".equalsIgnoreCase(req.getRoleName());
            if (fhirId == null || fhirId.isBlank()) {
                fhirId = findFhirIdByEmail(orgAlias, isPatient, req.getEmail());
            }
            if (fhirId != null && !fhirId.isBlank()) {
                attributes.put(isPatient ? "patient_fhir_id" : "practitioner_fhir_id", fhirId);
                log.info("Auto-linked user {} to FHIR {} ID {}", req.getEmail(),
                        isPatient ? "Patient" : "Practitioner", fhirId);
            }

            // Create user in Keycloak
            String userId = keycloakUserService.createUser(
                    req.getEmail(), req.getFirstName(), req.getLastName(),
                    password, attributes);

            if (userId == null) {
                return ResponseEntity.ok(ApiResponse.error("Failed to create user — no ID returned"));
            }

            // Add to Keycloak organization
            keycloakUserService.addUserToOrganization(userId, orgAlias);

            // Assign role (block reserved platform roles)
            if (req.getRoleName() != null && !req.getRoleName().isBlank()) {
                String upperRole = req.getRoleName().toUpperCase();
                if ("CIYEX_SUPER_ADMIN".equals(upperRole) || "SUPER_ADMIN".equals(upperRole)) {
                    return ResponseEntity.badRequest().body(
                            ApiResponse.error("Role '" + req.getRoleName() + "' is reserved and cannot be assigned"));
                }
                keycloakUserService.assignRolesToUser(userId, List.of(req.getRoleName()));
            }

            // Send welcome email if requested
            boolean emailSent = false;
            String emailError = null;
            if (req.isSendWelcomeEmail()) {
                try {
                    emailService.sendEmail(orgAlias, req.getEmail(),
                            "Welcome to Ciyex EHR — Your Account",
                            buildWelcomeEmailHtml(req.getFirstName(), req.getEmail(), password));
                    emailSent = true;
                } catch (Exception e) {
                    emailError = e.getMessage();
                    log.warn("Failed to send welcome email to {}: {}", req.getEmail(), emailError);
                }
            }

            // Return user with temp password for print
            var user = keycloakUserService.getUserById(userId);
            if (req.isGeneratePrintCredentials() || !emailSent) {
                // Always include temp password if email failed so user can share it manually
                user.setTemporaryPassword(password);
            }

            String message = emailSent ? "User created" :
                    (req.isSendWelcomeEmail() ? "User created but welcome email failed to send. Temporary password: " + password : "User created");
            return ResponseEntity.ok(ApiResponse.ok(message, user));
        } catch (Exception e) {
            log.error("Failed to create user", e);
            String msg = e.getMessage();
            if (msg != null && msg.contains("409")) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("A user with this email already exists"));
            }
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + msg));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable String id, @RequestBody UpdateUserRequest req) {
        try {

            keycloakUserService.updateUser(id, req.getFirstName(), req.getLastName(),
                    req.getEmail(), req.getPhone(), req.getEnabled());

            // Update role if changed (block reserved platform roles)
            if (req.getRoleName() != null && !req.getRoleName().isBlank()) {
                String upperRole = req.getRoleName().toUpperCase();
                if ("CIYEX_SUPER_ADMIN".equals(upperRole) || "SUPER_ADMIN".equals(upperRole)) {
                    return ResponseEntity.badRequest().body(
                            ApiResponse.error("Role '" + req.getRoleName() + "' is reserved and cannot be assigned"));
                }
                keycloakUserService.assignRolesToUser(id, List.of(req.getRoleName()));
            }

            var user = keycloakUserService.getUserById(id);
            return ResponseEntity.ok(ApiResponse.ok("User updated", user));
        } catch (Exception e) {
            log.error("Failed to update user {}", id, e);
            String msg = e.getMessage();
            if (msg != null && msg.contains("409")) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("A user with this email already exists"));
            }
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to update user: " + msg));
        }
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(@PathVariable String id) {
        try {

            keycloakUserService.disableUser(id);
            return ResponseEntity.ok(ApiResponse.ok("User deactivated", null));
        } catch (Exception e) {
            log.error("Failed to deactivate user {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<ResetPasswordResponse>> resetPassword(@PathVariable String id) {
        try {

            String tempPassword = keycloakUserService.resetPassword(id);
            var user = keycloakUserService.getUserById(id);

            // Send temp password via email
            boolean emailSent = false;
            String orgAlias = RequestContext.get().getOrgName();
            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                try {
                    emailService.sendEmail(orgAlias, user.getEmail(),
                            "Ciyex EHR — Password Reset",
                            buildResetPasswordEmailHtml(user.getFirstName(), user.getEmail(), tempPassword));
                    emailSent = true;
                } catch (Exception e) {
                    log.warn("Failed to send reset password email to {}: {}", user.getEmail(), e.getMessage());
                }
            }

            var resp = ResetPasswordResponse.builder()
                    .userId(id)
                    .username(user.getUsername())
                    .temporaryPassword(tempPassword)
                    .resetDate(LocalDate.now().toString())
                    .build();

            String message = emailSent ? "Password reset — email sent to " + user.getEmail()
                    : "Password reset — email not sent, share password manually";
            return ResponseEntity.ok(ApiResponse.ok(message, resp));
        } catch (Exception e) {
            log.error("Failed to reset password for user {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/send-reset-email")
    public ResponseEntity<ApiResponse<Void>> sendResetEmail(@PathVariable String id) {
        try {
            // First try Keycloak's built-in execute-actions-email
            try {
                keycloakUserService.sendPasswordResetEmail(id);
                return ResponseEntity.ok(ApiResponse.ok("Password reset email sent", null));
            } catch (Exception keycloakEx) {
                log.warn("Keycloak email failed for user {}, falling back to app EmailService: {}", id, keycloakEx.getMessage());
            }

            // Fallback: reset password and send credentials via app's own EmailService
            var user = keycloakUserService.getUserById(id);
            if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                return ResponseEntity.ok(ApiResponse.error("User has no email address configured"));
            }

            String tempPassword = keycloakUserService.resetPassword(id);
            String orgAlias = RequestContext.get().getOrgName();
            String htmlBody = "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;'>"
                    + "<h2 style='color:#1e40af;'>Password Reset</h2>"
                    + "<p>Hello " + (user.getFirstName() != null ? user.getFirstName() : "") + ",</p>"
                    + "<p>Your password has been reset. Please use the following temporary credentials to log in:</p>"
                    + "<div style='background:#f1f5f9;padding:16px;border-radius:8px;margin:16px 0;'>"
                    + "<p style='margin:4px 0;'><strong>Username:</strong> " + user.getUsername() + "</p>"
                    + "<p style='margin:4px 0;'><strong>Temporary Password:</strong> " + tempPassword + "</p>"
                    + "</div>"
                    + "<p>You will be required to change your password upon first login.</p>"
                    + "<p style='color:#64748b;font-size:12px;'>This is an automated message. Please do not reply.</p>"
                    + "</div>";

            emailService.sendEmail(orgAlias, user.getEmail(), "Password Reset - Your Temporary Credentials", htmlBody);
            return ResponseEntity.ok(ApiResponse.ok("Password reset email sent", null));
        } catch (Exception e) {
            log.error("Failed to send reset email for user {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/link-practitioner")
    public ResponseEntity<ApiResponse<UserResponse>> linkPractitioner(
            @PathVariable String id, @RequestBody Map<String, String> body) {
        try {

            String fhirId = body.get("practitionerFhirId");

            // Detect user role to decide attribute name
            var existingUser = keycloakUserService.getUserById(id);
            boolean isPatient = existingUser != null && existingUser.getRoles() != null
                    && existingUser.getRoles().contains("PATIENT");

            Map<String, String> attrs = new HashMap<>();
            if (fhirId != null && !fhirId.isBlank()) {
                attrs.put(isPatient ? "patient_fhir_id" : "practitioner_fhir_id", fhirId);
            }
            String npi = body.get("npi");
            if (npi != null && !npi.isBlank()) {
                attrs.put("npi", npi);
            }

            keycloakUserService.setUserAttributes(id, attrs);
            var user = keycloakUserService.getUserById(id);
            String label = isPatient ? "Patient" : "Practitioner";
            return ResponseEntity.ok(ApiResponse.ok(label + " linked", user));
        } catch (Exception e) {
            log.error("Failed to link practitioner for user {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    // ── Helpers ──

    private String generateTempPassword() {
        java.security.SecureRandom random = new java.security.SecureRandom();
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$";
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
        return sb.toString();
    }

    /**
     * Search FHIR for a Patient or Practitioner with matching email in telecom.
     * Searches by value only (no system qualifier) so it works for both old resources
     * (missing system code) and new ones (with system=email).
     */
    private String findFhirIdByEmail(String orgAlias, boolean isPatient, String email) {
        if (email == null || email.isBlank() || orgAlias == null) return null;
        try {
            Bundle bundle;
            if (isPatient) {
                bundle = fhirClient.getClient(orgAlias).search()
                        .forResource(Patient.class)
                        .where(new TokenClientParam("telecom").exactly().code(email))
                        .count(1)
                        .returnBundle(Bundle.class)
                        .execute();
            } else {
                bundle = fhirClient.getClient(orgAlias).search()
                        .forResource(Practitioner.class)
                        .where(new TokenClientParam("telecom").exactly().code(email))
                        .count(1)
                        .returnBundle(Bundle.class)
                        .execute();
            }
            if (bundle.hasEntry()) {
                return bundle.getEntry().get(0).getResource().getIdElement().getIdPart();
            }
        } catch (Exception e) {
            log.warn("Failed to search FHIR {} by email {}: {}",
                    isPatient ? "Patient" : "Practitioner", email, e.getMessage());
        }
        return null;
    }

    private String buildResetPasswordEmailHtml(String firstName, String email, String password) {
        String name = firstName != null && !firstName.isBlank() ? firstName : "User";
        return """
                <div style="font-family: sans-serif; max-width: 500px; margin: 0 auto;">
                    <h2 style="color: #1e40af;">Password Reset</h2>
                    <p>Hello %s,</p>
                    <p>Your password has been reset. Here are your new login credentials:</p>
                    <div style="background: #f1f5f9; padding: 16px; border-radius: 8px; margin: 16px 0;">
                        <p style="margin: 4px 0;"><strong>Username:</strong> %s</p>
                        <p style="margin: 4px 0;"><strong>Temporary Password:</strong> %s</p>
                    </div>
                    <p style="color: #dc2626; font-weight: bold;">Please change your password on first login.</p>
                    <p style="color: #6b7280; font-size: 12px;">If you did not request this reset, contact your administrator immediately.</p>
                    <p style="color: #6b7280; font-size: 12px;">— Ciyex EHR Team</p>
                </div>
                """.formatted(name, email, password);
    }

    private String buildWelcomeEmailHtml(String firstName, String email, String password) {
        return """
                <div style="font-family: sans-serif; max-width: 500px; margin: 0 auto;">
                    <h2 style="color: #1e40af;">Welcome to Ciyex EHR</h2>
                    <p>Hello %s,</p>
                    <p>Your account has been created. Here are your login credentials:</p>
                    <div style="background: #f1f5f9; padding: 16px; border-radius: 8px; margin: 16px 0;">
                        <p style="margin: 4px 0;"><strong>Username:</strong> %s</p>
                        <p style="margin: 4px 0;"><strong>Temporary Password:</strong> %s</p>
                    </div>
                    <p style="color: #dc2626; font-weight: bold;">Please change your password on first login.</p>
                    <p style="color: #6b7280; font-size: 12px;">— Ciyex EHR Team</p>
                </div>
                """.formatted(firstName, email, password);
    }
}
