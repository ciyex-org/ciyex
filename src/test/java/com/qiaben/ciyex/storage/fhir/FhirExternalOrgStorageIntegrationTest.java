/*
package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.OrgDto;
import com.qiaben.ciyex.storage.ExternalOrgStorage;
import com.qiaben.ciyex.dto.core.integration.RequestContext;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

@SpringBootTest
@ActiveProfiles("test") // Use a test profile if configured
@TestMethodOrder(OrderAnnotation.class) // Enforce test method order
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Share instance state across tests
public class FhirExternalOrgStorageIntegrationTest {

    @Autowired
    private ExternalOrgStorage orgStorage;

    private String externalId;
    private Long testOrgId = 10000L; // Use the preconfigured orgId from the SQL insert

    @BeforeAll
    public void setUpAll() {
        // Create and configure RequestContext with the preconfigured test orgId
        RequestContext context = new RequestContext();
        context.setOrgId(testOrgId);
        RequestContext.set(context);

        // Create the initial org
        OrgDto orgDto = new OrgDto();
        orgDto.setOrgName("Test Org - " + testOrgId);
        orgDto.setAddress("123 Test St - " + testOrgId);
        orgDto.setCity("Test City - " + testOrgId);
        orgDto.setState("TS");
        orgDto.setPostalCode("12345");
        orgDto.setCountry("Test Country");
        externalId = orgStorage.create(orgDto);
        assertNotNull(externalId, "External ID should be generated for orgId: " + testOrgId);
        System.out.println("Initialized org with externalId: " + externalId + " for orgId: " + testOrgId);
    }

    @BeforeEach
    public void setUp() {
        // Re-set the context for each test to ensure consistency
        RequestContext context = new RequestContext();
        context.setOrgId(testOrgId);
        RequestContext.set(context);
    }

    @AfterAll
    public void tearDownAll() {
        // Clean up RequestContext and delete the created resource if it exists
        if (externalId != null) {
            try {
                // Re-set the context for cleanup
                RequestContext context = new RequestContext();
                context.setOrgId(testOrgId);
                RequestContext.set(context);

                orgStorage.delete(externalId);
                System.out.println("Cleaned up test resource with externalId: " + externalId + " for orgId: " + testOrgId);
            } catch (Exception e) {
                System.err.println("Failed to clean up test resource with externalId: " + externalId + " for orgId: " + testOrgId + ". Error: " + e.getMessage());
            } finally {
                RequestContext.clear(); // Clear context after cleanup
            }
        }
    }

    @Test
    @Order(1)
    public void testCreateOrg() {
        // Verify the initial creation from setUpAll
        OrgDto retrievedOrg = orgStorage.get(externalId);
        assertNotNull(retrievedOrg, "Retrieved org should not be null for orgId: " + testOrgId);
        assertEquals("Test Org - " + testOrgId, retrievedOrg.getOrgName(), "Org name should match for orgId: " + testOrgId);
        assertEquals("123 Test St - " + testOrgId, retrievedOrg.getAddress(), "Address should match for orgId: " + testOrgId);
        assertEquals("Test City - " + testOrgId, retrievedOrg.getCity(), "City should match for orgId: " + testOrgId);
        assertEquals("TS", retrievedOrg.getState(), "State should match for orgId: " + testOrgId);
        assertEquals("12345", retrievedOrg.getPostalCode(), "Postal code should match for orgId: " + testOrgId);
        assertEquals("Test Country", retrievedOrg.getCountry(), "Country should match for orgId: " + testOrgId);
    }

    @Test
    @Order(2)
    public void testUpdateOrg() {
        if (externalId == null) {
            fail("externalId is null, testCreateOrg must run first for orgId: " + testOrgId);
        }

        // Update org
        OrgDto updatedDto = new OrgDto();
        updatedDto.setOrgName("Updated Org - " + testOrgId);
        updatedDto.setAddress("456 Updated St - " + testOrgId);
        orgStorage.update(updatedDto, externalId);

        // Verify update
        OrgDto retrievedOrg = orgStorage.get(externalId);
        assertNotNull(retrievedOrg, "Retrieved org should not be null for orgId: " + testOrgId);
        assertEquals(updatedDto.getOrgName(), retrievedOrg.getOrgName(), "Updated org name should match for orgId: " + testOrgId);
        assertEquals(updatedDto.getAddress(), retrievedOrg.getAddress(), "Updated address should match for orgId: " + testOrgId);
    }

    @Test
    @Order(3)
    public void testGetOrg() {
        if (externalId == null) {
            fail("externalId is null, testCreateOrg must run first for orgId: " + testOrgId);
        }

        // Retrieve org
        OrgDto retrievedOrg = orgStorage.get(externalId);
        assertNotNull(retrievedOrg, "Retrieved org should not be null for orgId: " + testOrgId);
        assertEquals("Updated Org - " + testOrgId, retrievedOrg.getOrgName(), "Org name should match updated value for orgId: " + testOrgId);
        assertEquals(externalId, retrievedOrg.getFhirId(), "External ID should match for orgId: " + testOrgId);
    }

    @Test
    @Order(4)
    public void testSearchAllOrgs() {
        if (externalId == null) {
            fail("externalId is null, testCreateOrg must run first for orgId: " + testOrgId);
        }

        // Search all orgs
        List<OrgDto> orgs = orgStorage.searchAll();
        assertNotNull(orgs, "Org list should not be null for orgId: " + testOrgId);
        assertTrue(orgs.stream().anyMatch(o -> o.getFhirId().equals(externalId)), "Created org should be in the list for orgId: " + testOrgId);
        System.out.println("Found " + orgs.size() + " orgs, including externalId: " + externalId + " for orgId: " + testOrgId);
    }

    //TODO: Uncomment this test when delete functionality tests are stable
    //@Test
    //@Order(5)
    public void testDeleteOrg() {
        if (externalId == null) {
            fail("externalId is null, testCreateOrg must run first for orgId: " + testOrgId);
        }

        // Delete org
        orgStorage.delete(externalId);

        // Verify deletion with context still set
        System.out.println("Verifying deletion with orgId: " + RequestContext.get().getOrgId());
        OrgDto retrievedOrg = orgStorage.get(externalId);
        assertNull(retrievedOrg, "Deleted org should not be retrievable for orgId: " + testOrgId);
    }
}*/
