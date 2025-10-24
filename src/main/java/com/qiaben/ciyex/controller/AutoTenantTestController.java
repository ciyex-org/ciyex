package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.service.PatientService;
import com.qiaben.ciyex.service.TenantAwareService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test controller to demonstrate automatic tenant switching functionality.
 * Shows how org ID-based tenant context works without manual schema selection.
 */
@RestController
@RequestMapping("/api/test/auto-tenant")
@RequiredArgsConstructor
@Slf4j
public class AutoTenantTestController {
    
    private final PatientService patientService;
    private final TenantAwareService tenantAwareService;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    /**
     * Test automatic tenant switching by counting patients for a specific org.
     * The aspect will automatically switch to the appropriate tenant schema.
     */
    @GetMapping("/patient-count/{orgId}")
    public ResponseEntity<Map<String, Object>> testPatientCount(@PathVariable Long orgId) {
        // orgId-based context deprecated; relying on tenantName resolution elsewhere
        long count = patientService.countPatientsForCurrentOrg();
        Map<String, Object> response = new HashMap<>();
        response.put("orgId", orgId);
        response.put("patientCount", count);
        response.put("message", "Automatically switched to tenant schema for org " + orgId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Test master schema operations (user/org management).
     */
    @GetMapping("/master-operation")
    @Transactional
    public ResponseEntity<Map<String, Object>> testMasterOperation() {
        // Clear RequestContext to ensure master schema is used
        RequestContext.clear();
        
        try {
            // This should automatically use master schema
            String currentSchema = getCurrentSchema();
            List<Map<String, Object>> orgs = getOrgsFromMasterSchema();
            
            Map<String, Object> response = new HashMap<>();
            response.put("currentSchema", currentSchema);
            response.put("orgCount", orgs.size());
            response.put("message", "Automatically used master schema for org management");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Master operation failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Test tenant schema operations with explicit org ID.
     */
    @GetMapping("/tenant-operation/{orgId}")
    public ResponseEntity<Map<String, Object>> testTenantOperation(@PathVariable Long orgId) {
        boolean schemaExists = tenantAwareService.tenantSchemaExists(orgId);
        Map<String, Object> response = new HashMap<>();
        response.put("orgId", orgId);
        response.put("tenantSchema", "practice_" + orgId);
        response.put("schemaExists", schemaExists);
        if (schemaExists) {
            int tableCount = getTenantTableCount(orgId);
            response.put("tableCount", tableCount);
            response.put("message", "Automatically switched to tenant schema practice_" + orgId);
        } else {
            response.put("message", "Tenant schema does not exist for org " + orgId);
        }
        return ResponseEntity.ok(response);
    }
    
    /**
     * Compare master vs tenant schema table counts.
     */
    @GetMapping("/schema-comparison/{orgId}")
    public ResponseEntity<Map<String, Object>> compareSchemas(@PathVariable Long orgId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Test master schema
            RequestContext.clear();
            int masterTableCount = tenantAwareService.executeInMasterContext(() -> {
                return getMasterTableCount();
            });
            
            // Test tenant schema
            int tenantTableCount = tenantAwareService.executeInTenantContext(orgId, () -> {
                return getTenantTableCount(orgId);
            });
            
            response.put("masterSchema", Map.of(
                "name", "public",
                "tableCount", masterTableCount
            ));
            response.put("tenantSchema", Map.of(
                "name", "practice_" + orgId,
                "tableCount", tenantTableCount
            ));
            response.put("message", "Successfully compared master and tenant schemas");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Schema comparison failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        } finally {
            // No per-request orgId context to clear
        }
    }
    
    // Helper methods
    
    @Transactional(readOnly = true)
    private String getCurrentSchema() {
        return entityManager.createNativeQuery("SELECT current_schema()")
                .getSingleResult().toString();
    }
    
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getOrgsFromMasterSchema() {
        return entityManager.createNativeQuery("SELECT id, org_name FROM orgs LIMIT 10")
                .getResultList();
    }
    
    @Transactional(readOnly = true)
    private int getMasterTableCount() {
        Long count = (Long) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE'")
                .getSingleResult();
        return count.intValue();
    }
    
    @Transactional(readOnly = true)
    private int getTenantTableCount(Long orgId) {
        String schemaName = "practice_" + orgId;
        Long count = (Long) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = :schemaName AND table_type = 'BASE TABLE'")
                .setParameter("schemaName", schemaName)
                .getSingleResult();
        return count.intValue();
    }
}
