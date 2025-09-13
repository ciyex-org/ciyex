package com.qiaben.ciyex.service;

import com.qiaben.ciyex.entity.Org;
import com.qiaben.ciyex.entity.User;
import com.qiaben.ciyex.entity.UserOrgRole;
import com.qiaben.ciyex.repository.OrgRepository;
import com.qiaben.ciyex.service.TenantSchemaInitializer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service to initialize master schema tables during startup.
 * Uses JPA entities to avoid hardcoded SQL - relies on Spring Boot's
 * automatic schema creation via hibernate.hbm2ddl.auto=update property.
 */
@Service
@Slf4j
public class MasterSchemaInitializer {

    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private OrgRepository orgRepository;
    
    @Autowired
    private TenantSchemaInitializer tenantSchemaInitializer;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeMasterSchema() {
        log.info("Initializing master schema with authentication tables (via JPA entities)...");

        try {
            // Ensure we're in the public schema
            entityManager.createNativeQuery("SET search_path TO public").executeUpdate();

            // Touch the entities to ensure they are loaded and tables are created
            // This works with hibernate.hbm2ddl.auto=update to create missing tables
            entityManager.getMetamodel().entity(User.class);
            entityManager.getMetamodel().entity(Org.class);
            entityManager.getMetamodel().entity(UserOrgRole.class);

            log.info("Master schema initialization completed successfully");
            
            // After master schema is initialized, check and initialize tenant schemas
            initializeTenantSchemasForExistingOrgs();
            
        } catch (Exception e) {
            log.error("Failed to initialize master schema", e);
            throw new RuntimeException("Master schema initialization failed", e);
        }
    }
    
    private void initializeTenantSchemasForExistingOrgs() {
        log.info("Scanning orgs table to initialize missing tenant schemas...");
        
        try {
            // Ensure we're in the public schema to read orgs
            entityManager.createNativeQuery("SET search_path TO public").executeUpdate();
            
            // Get all organizations from the master schema
            List<Org> allOrgs = orgRepository.findAll();
            log.info("Found {} organizations in master schema", allOrgs.size());
            
            for (Org org : allOrgs) {
                Long orgId = org.getId();
                String tenantSchemaName = "practice_" + orgId;
                
                // Check if tenant schema exists
                if (!tenantSchemaExists(tenantSchemaName)) {
                    log.info("Tenant schema {} not found for org ID {}. Initializing...", tenantSchemaName, orgId);
                    tenantSchemaInitializer.initializeTenantSchema(orgId);
                    log.info("Successfully initialized tenant schema for org ID {}", orgId);
                } else {
                    log.debug("Tenant schema {} already exists for org ID {}", tenantSchemaName, orgId);
                    // Run idempotent migrations to keep schema up to date (e.g., JSONB columns)
                    tenantSchemaInitializer.runTenantMigrations(orgId);
                }
            }
            
            log.info("Completed tenant schema initialization for all existing organizations");
            
        } catch (Exception e) {
            log.error("Failed to initialize tenant schemas for existing orgs", e);
            // Don't throw exception here - let the application continue even if tenant schema init fails
        }
    }
    
    private boolean tenantSchemaExists(String schemaName) {
        try {
            String checkSchemaQuery = "SELECT EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = ?)";
            Object result = entityManager.createNativeQuery(checkSchemaQuery)
                    .setParameter(1, schemaName)
                    .getSingleResult();
            return (Boolean) result;
        } catch (Exception e) {
            log.warn("Failed to check if schema {} exists: {}", schemaName, e.getMessage());
            return false;
        }
    }
}
