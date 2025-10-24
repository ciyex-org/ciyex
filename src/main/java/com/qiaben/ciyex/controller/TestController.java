package com.qiaben.ciyex.controller;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final EntityManagerFactory entityManagerFactory;

    @GetMapping("/schemas")
    public ResponseEntity<Map<String, Object>> listSchemas() {
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Use database client to check schemas: SELECT schema_name FROM information_schema.schemata;"
        ));
    }

    @GetMapping("/entities")
    public ResponseEntity<Map<String, Object>> listDetectedEntities() {
        try {
            SessionFactoryImplementor sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
            
            List<String> allEntities = new ArrayList<>();
            List<String> tenantEntities = new ArrayList<>();
            List<String> masterEntities = new ArrayList<>();
            
            // Define master schema entities for comparison
            Set<String> masterEntityNames = Set.of(
                "com.qiaben.ciyex.entity.User",
                "com.qiaben.ciyex.entity.Org", 
                "com.qiaben.ciyex.entity.UserOrgRole"
            );
            
            sessionFactory.getMetamodel().getEntities().forEach(entityType -> {
                Class<?> entityClass = entityType.getJavaType();
                String className = entityClass.getName();
                allEntities.add(className);
                
                if (masterEntityNames.contains(className)) {
                    masterEntities.add(className);
                } else {
                    tenantEntities.add(className);
                }
            });
            
            // Sort for better readability
            Collections.sort(allEntities);
            Collections.sort(tenantEntities);
            Collections.sort(masterEntities);
            
            boolean auditLogFound = tenantEntities.contains("com.qiaben.ciyex.audit.AuditLog");
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "totalEntities", allEntities.size(),
                "auditLogDetected", auditLogFound,
                "tenantEntitiesCount", tenantEntities.size(),
                "masterEntitiesCount", masterEntities.size(),
                "allEntities", allEntities,
                "tenantEntities", tenantEntities,
                "masterEntities", masterEntities
            ));
        } catch (Exception e) {
            log.error("Failed to list entities", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "Failed to list entities: " + e.getMessage()
            ));
        }
    }
}
