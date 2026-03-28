package org.ciyex.ehr.security.unit;

import org.ciyex.ehr.security.ClinicalRole;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ClinicalRole} enum.
 * No Spring context required — pure JUnit 5.
 */
class ClinicalRoleTest {

    @Test
    void fromString_lowerCase_returnsPresent() {
        Optional<ClinicalRole> result = ClinicalRole.fromString("admin");
        assertTrue(result.isPresent());
        assertEquals(ClinicalRole.ADMIN, result.get());
    }

    @Test
    void fromString_upperCase_returnsPresent() {
        Optional<ClinicalRole> result = ClinicalRole.fromString("ADMIN");
        assertTrue(result.isPresent());
        assertEquals(ClinicalRole.ADMIN, result.get());
    }

    @Test
    void fromString_mixedCase_returnsPresent() {
        assertTrue(ClinicalRole.fromString("Provider").isPresent());
        assertTrue(ClinicalRole.fromString("NURSE").isPresent());
        assertTrue(ClinicalRole.fromString("front_desk").isPresent());
        assertTrue(ClinicalRole.fromString("Billing").isPresent());
    }

    @Test
    void fromString_superAdmin_returnsPresent() {
        assertTrue(ClinicalRole.fromString("super_admin").isPresent());
        assertEquals(ClinicalRole.SUPER_ADMIN, ClinicalRole.fromString("SUPER_ADMIN").get());
    }

    @Test
    void fromString_unknown_returnsEmpty() {
        assertTrue(ClinicalRole.fromString("RECEPTIONIST").isEmpty());
        assertTrue(ClinicalRole.fromString("DOCTOR").isEmpty());
        assertTrue(ClinicalRole.fromString("ROOT").isEmpty());
    }

    @Test
    void fromString_null_returnsEmpty() {
        assertTrue(ClinicalRole.fromString(null).isEmpty());
    }

    @Test
    void fromString_blank_returnsEmpty() {
        assertTrue(ClinicalRole.fromString("").isEmpty());
        assertTrue(ClinicalRole.fromString("   ").isEmpty());
    }

    @Test
    void priorityOrder_superAdminIsFirst() {
        assertEquals(ClinicalRole.SUPER_ADMIN, ClinicalRole.PRIORITY.get(0));
    }

    @Test
    void priorityOrder_adminIsSecond() {
        assertEquals(ClinicalRole.ADMIN, ClinicalRole.PRIORITY.get(1));
    }

    @Test
    void priorityOrder_patientIsLast() {
        int last = ClinicalRole.PRIORITY.size() - 1;
        assertEquals(ClinicalRole.PATIENT, ClinicalRole.PRIORITY.get(last));
    }

    @Test
    void priorityList_containsAllRoles() {
        for (ClinicalRole role : ClinicalRole.values()) {
            assertTrue(ClinicalRole.PRIORITY.contains(role),
                    "PRIORITY list is missing role: " + role);
        }
    }

    @Test
    void priorityList_noDuplicates() {
        long distinct = ClinicalRole.PRIORITY.stream().distinct().count();
        assertEquals(ClinicalRole.PRIORITY.size(), distinct,
                "PRIORITY list has duplicate roles");
    }

    @Test
    void allEnumValues_parseableViaFromString() {
        for (ClinicalRole role : ClinicalRole.values()) {
            Optional<ClinicalRole> parsed = ClinicalRole.fromString(role.name());
            assertTrue(parsed.isPresent(), "Could not parse role: " + role.name());
            assertEquals(role, parsed.get());
        }
    }
}
