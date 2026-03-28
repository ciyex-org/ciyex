package org.ciyex.ehr.security.unit;

import org.ciyex.ehr.security.Permission;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates the structure and naming conventions of {@link Permission} constants.
 * Uses reflection to iterate all constants so new additions are automatically checked.
 */
class PermissionConstantsTest {

    private List<String> allConstants() throws Exception {
        List<String> values = new ArrayList<>();
        for (Field f : Permission.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) && f.getType() == String.class) {
                values.add((String) f.get(null));
            }
        }
        return values;
    }

    @Test
    void allConstants_startWithSCOPE_prefix() throws Exception {
        for (String scope : allConstants()) {
            assertTrue(scope.startsWith("SCOPE_"),
                    "Permission constant missing SCOPE_ prefix: " + scope);
        }
    }

    @Test
    void allConstants_followSmartOnFhirFormat() throws Exception {
        // After SCOPE_ prefix, must be context/ResourceType.action
        // context = user | patient | system
        for (String scope : allConstants()) {
            // all scopes must follow SMART format
            String inner = scope.substring("SCOPE_".length()); // e.g. user/Patient.read
            assertTrue(inner.contains("/"),
                    "Missing '/' context separator: " + scope);
            assertTrue(inner.contains("."),
                    "Missing '.' action separator: " + scope);
            String context = inner.substring(0, inner.indexOf('/'));
            assertTrue(context.equals("user") || context.equals("patient") || context.equals("system"),
                    "Unknown SMART context '" + context + "' in: " + scope);
        }
    }

    @Test
    void allUserScopeActions_areReadOrWrite() throws Exception {
        for (String scope : allConstants()) {
            if (!scope.contains("user/")) continue;
            String action = scope.substring(scope.lastIndexOf('.') + 1);
            assertTrue(action.equals("read") || action.equals("write"),
                    "Unexpected action '" + action + "' in scope: " + scope);
        }
    }

    @Test
    void patientScopeActions_areAllRead() throws Exception {
        for (String scope : allConstants()) {
            if (!scope.contains("patient/")) continue;
            assertTrue(scope.endsWith(".read"),
                    "Patient context scopes must be read-only, found: " + scope);
        }
    }

    @Test
    void noduplicateValues() throws Exception {
        List<String> all = allConstants();
        Set<String> unique = new HashSet<>(all);
        assertEquals(unique.size(), all.size(),
                "Duplicate Permission constant values detected");
    }

    @Test
    void knownClinicalScopes_haveCorrectValues() {
        assertEquals("SCOPE_user/Patient.read",       Permission.PATIENT_READ);
        assertEquals("SCOPE_user/Patient.write",      Permission.PATIENT_WRITE);
        assertEquals("SCOPE_user/Appointment.read",   Permission.APPOINTMENT_READ);
        assertEquals("SCOPE_user/Appointment.write",  Permission.APPOINTMENT_WRITE);
        assertEquals("SCOPE_user/Organization.read",  Permission.ORGANIZATION_READ);
        assertEquals("SCOPE_user/Organization.write", Permission.ORGANIZATION_WRITE);
        assertEquals("SCOPE_user/Claim.read",         Permission.CLAIM_READ);
        assertEquals("SCOPE_user/Claim.write",        Permission.CLAIM_WRITE);
        assertEquals("SCOPE_patient/Patient.read",    Permission.PATIENT_SELF_READ);
    }

    @Test
    void totalConstantCount_isAtLeast35() throws Exception {
        // Sanity check: we have >= 35 constants (28 user + 7 patient + 1 system)
        assertTrue(allConstants().size() >= 35,
                "Expected at least 35 Permission constants, found: " + allConstants().size());
    }
}
